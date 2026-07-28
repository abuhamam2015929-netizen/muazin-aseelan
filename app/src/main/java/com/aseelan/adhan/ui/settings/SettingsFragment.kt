package com.aseelan.adhan.ui.settings

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aseelan.adhan.alarm.AlarmScheduler
import com.aseelan.adhan.data.AlertMode
import com.aseelan.adhan.data.Muadhin
import com.aseelan.adhan.data.MuadhinList
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.data.SettingsRepository
import com.aseelan.adhan.databinding.DialogMuadhinBinding
import com.aseelan.adhan.databinding.DialogPrayerSettingsBinding
import com.aseelan.adhan.databinding.FragmentSettingsBinding
import com.aseelan.adhan.databinding.ItemMuadhinBinding
import com.aseelan.adhan.databinding.ItemPrayerSettingBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: SettingsRepository

    private var previewPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsRepository(requireContext())

        renderGlobalMuadhin()
        renderHijriOffset()
        renderPrayerRows()

        binding.rowGlobalMuadhin.setOnClickListener { showMuadhinDialog() }
        binding.btnHijriMinus.setOnClickListener {
            settings.setHijriDayOffset(settings.getHijriDayOffset() - 1)
            renderHijriOffset()
        }
        binding.btnHijriPlus.setOnClickListener {
            settings.setHijriDayOffset(settings.getHijriDayOffset() + 1)
            renderHijriOffset()
        }
    }

    private fun renderGlobalMuadhin() {
        val muadhin = MuadhinList.byId(settings.getGlobalMuadhinId())
        binding.textGlobalMuadhinName.text = muadhin.name
    }

    private fun renderHijriOffset() {
        binding.textHijriOffset.text = settings.getHijriDayOffset().toString()
    }

    private fun renderPrayerRows() {
        binding.prayerSettingsContainer.removeAllViews()
        for (prayer in PrayerType.adhanPrayers) {
            val rowBinding = ItemPrayerSettingBinding.inflate(layoutInflater, binding.prayerSettingsContainer, false)
            rowBinding.textPrayerName.text = prayer.arabicName
            rowBinding.textCurrentMode.text = settings.getAlertMode(prayer).arabicLabel
            rowBinding.root.setOnClickListener {
                showPrayerSettingsDialog(prayer)
            }
            binding.prayerSettingsContainer.addView(rowBinding.root)
        }
    }

    private fun showMuadhinDialog() {
        val dialogBinding = DialogMuadhinBinding.inflate(layoutInflater)
        val currentId = settings.getGlobalMuadhinId()

        for (muadhin in MuadhinList.all) {
            val itemBinding = ItemMuadhinBinding.inflate(layoutInflater, dialogBinding.radioGroupMuadhin, false)
            itemBinding.radioMuadhin.text = muadhin.name
            itemBinding.radioMuadhin.id = View.generateViewId()
            itemBinding.radioMuadhin.isChecked = muadhin.id == currentId
            itemBinding.radioMuadhin.tag = muadhin.id

            itemBinding.btnPreviewMuadhin.setOnClickListener {
                playPreview(muadhin)
            }

            dialogBinding.radioGroupMuadhin.addView(itemBinding.root)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.radioGroupMuadhin.setOnCheckedChangeListener { group, checkedId ->
            val checkedButton = group.findViewById<RadioButton>(checkedId)
            val muadhinId = checkedButton?.tag as? Int ?: return@setOnCheckedChangeListener
            settings.setGlobalMuadhinId(muadhinId)
            renderGlobalMuadhin()
            AlarmScheduler.scheduleAll(requireContext())
            stopPreview()
            dialog.dismiss()
        }

        dialogBinding.btnCancelDialog.setOnClickListener {
            stopPreview()
            dialog.dismiss()
        }

        dialog.setOnDismissListener { stopPreview() }
        dialog.show()
    }

    private fun playPreview(muadhin: Muadhin) {
        stopPreview()

        val resId = resources.getIdentifier(muadhin.rawResName, "raw", requireContext().packageName)
        if (resId == 0) {
            Toast.makeText(
                requireContext(),
                "لم يتم رفع صوت هذا المؤذن بعد",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            previewPlayer = MediaPlayer.create(requireContext(), resId)
            previewPlayer?.setOnCompletionListener { stopPreview() }
            previewPlayer?.start()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "تعذّر تشغيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {
        }
        previewPlayer = null
    }

    private fun showPrayerSettingsDialog(prayer: PrayerType) {
        val db = DialogPrayerSettingsBinding.inflate(layoutInflater)
        db.textDialogPrayerName.text = prayer.arabicName

        when (settings.getAlertMode(prayer)) {
            AlertMode.FULL_ADHAN -> db.radioFullAzan.isChecked = true
            AlertMode.SHORT_BEEP -> db.radioShortBeep.isChecked = true
            AlertMode.SILENT -> db.radioSilent.isChecked = true
            AlertMode.OFF -> db.radioOff.isChecked = true
        }

        when (settings.getPreReminderMinutes(prayer)) {
            5 -> db.radioReminder5.isChecked = true
            10 -> db.radioReminder10.isChecked = true
            15 -> db.radioReminder15.isChecked = true
            else -> db.radioReminderNone.isChecked = true
        }

        var offsetValue = settings.getManualOffsetMinutes(prayer)
        db.textOffsetValue.text = offsetValue.toString()
        db.btnOffsetMinus.setOnClickListener {
            offsetValue -= 1
            db.textOffsetValue.text = offsetValue.toString()
        }
        db.btnOffsetPlus.setOnClickListener {
            offsetValue += 1
            db.textOffsetValue.text = offsetValue.toString()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(db.root)
            .create()

        db.btnSavePrayerSettings.setOnClickListener {
            val mode = when (db.radioGroupAlertMode.checkedRadioButtonId) {
                db.radioFullAzan.id -> AlertMode.FULL_ADHAN
                db.radioShortBeep.id -> AlertMode.SHORT_BEEP
                db.radioSilent.id -> AlertMode.SILENT
                else -> AlertMode.OFF
            }
            val preReminder = when (db.radioGroupPreReminder.checkedRadioButtonId) {
                db.radioReminder5.id -> 5
                db.radioReminder10.id -> 10
                db.radioReminder15.id -> 15
                else -> 0
            }

            settings.setAlertMode(prayer, mode)
            settings.setPreReminderMinutes(prayer, preReminder)
            settings.setManualOffsetMinutes(prayer, offsetValue)

            AlarmScheduler.scheduleAll(requireContext())
            renderPrayerRows()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        stopPreview()
        _binding = null
        super.onDestroyView()
    }
}
