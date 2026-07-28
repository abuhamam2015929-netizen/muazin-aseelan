package com.aseelan.adhan.ui.home

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aseelan.adhan.R
import com.aseelan.adhan.data.AlertMode
import com.aseelan.adhan.data.HijriDateConverter
import com.aseelan.adhan.data.PrayerTimesTable
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.data.SettingsRepository
import com.aseelan.adhan.databinding.FragmentHomeBinding
import com.aseelan.adhan.databinding.ItemPrayerBinding
import com.aseelan.adhan.ui.qibla.QiblaFragment
import com.aseelan.adhan.ui.settings.SettingsFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var countdownTimer: CountDownTimer? = null
    private lateinit var settings: SettingsRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsRepository(requireContext())

        binding.btnSettings.setOnClickListener { openTab(SettingsFragment()) }
        binding.btnBell.setOnClickListener { openTab(SettingsFragment()) }
        binding.btnQiblaShortcut.setOnClickListener { openTab(QiblaFragment()) }

        renderDates()
        renderPrayerList()
        startCountdown()
    }

    private fun openTab(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun renderDates() {
        val cal = Calendar.getInstance()

        val dayNameFormat = SimpleDateFormat("EEEE", Locale("ar"))
        binding.textDayName.text = dayNameFormat.format(cal.time)

        val gregorianFormat = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        binding.textGregorianDate.text = gregorianFormat.format(cal.time)

        val hijri = HijriDateConverter.fromGregorian(cal, settings.getHijriDayOffset())
        binding.textHijriDate.text = hijri.formatted()
    }

    private fun renderPrayerList() {
        binding.prayerListContainer.removeAllViews()
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val times = PrayerTimesTable.getPrayerTimes(month, day)
        val nextPrayer = findNextPrayer()

       val rows = listOf(
            Pair(PrayerType.FAJR, times.fajr),
            Pair(PrayerType.SHURUQ, times.shuruq),
            Pair(PrayerType.DHUHR, times.dhuhr),
            Pair(PrayerType.ASR, times.asr),
            Pair(PrayerType.MAGHRIB, times.maghrib),
            Pair(PrayerType.ISHA, times.isha)
        )

        for ((prayer, time) in rows) {
            val itemBinding = ItemPrayerBinding.inflate(layoutInflater, binding.prayerListContainer, false)
            itemBinding.textPrayerName.text = prayer.arabicName
            itemBinding.textPrayerTime.text = applyOffset(time, settings.getManualOffsetMinutes(prayer)) 
            val alertMode = settings.getAlertMode(prayer)
            itemBinding.iconAlertMode.setImageResource(alertIconFor(prayer, alertMode))

            if (prayer == nextPrayer) {
                itemBinding.root.setBackgroundResource(R.drawable.bg_prayer_item_upcoming)
            }
            binding.prayerListContainer.addView(itemBinding.root)
        }
    }

    private fun alertIconFor(prayer: PrayerType, mode: AlertMode): Int {
        if (!prayer.hasAdhan) return R.drawable.ic_off
        return when (mode) {
            AlertMode.FULL_ADHAN -> R.drawable.ic_sound_full
            AlertMode.SHORT_BEEP -> R.drawable.ic_bell
            AlertMode.SILENT -> R.drawable.ic_mute
            AlertMode.OFF -> R.drawable.ic_off
        }
    }

    private fun applyOffset(time: String, offsetMinutes: Int): String {
        if (offsetMinutes == 0) return time
        val parts = time.split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        cal.set(Calendar.MINUTE, parts[1].toInt())
        cal.add(Calendar.MINUTE, offsetMinutes)
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun findNextPrayer(): PrayerType {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val day = now.get(Calendar.DAY_OF_MONTH)
        val times = PrayerTimesTable.getPrayerTimes(month, day)
        val ordered = listOf(
            PrayerType.FAJR to times.fajr,
            PrayerType.DHUHR to times.dhuhr,
            PrayerType.ASR to times.asr,
            PrayerType.MAGHRIB to times.maghrib,
            PrayerType.ISHA to times.isha
        )
        for ((prayer, time) in ordered) {
            val target = timeToCalendar(time)
            if (target.after(now)) return prayer
        }
        return PrayerType.FAJR // بعد العشاء، القادمة هي فجر الغد
    }

    private fun timeToCalendar(hhmm: String): Calendar {
        val parts = hhmm.split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        cal.set(Calendar.MINUTE, parts[1].toInt())
        cal.set(Calendar.SECOND, 0)
        return cal
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        val nextPrayer = findNextPrayer()
        binding.textNextPrayerName.text = nextPrayer.arabicName

        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val day = now.get(Calendar.DAY_OF_MONTH)
        val times = PrayerTimesTable.getPrayerTimes(month, day)
        var target = timeToCalendar(times.timeFor(nextPrayer))

        if (!target.after(now)) {
            // الصلاة القادمة هي فجر الغد
            val tomorrow = now.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            val tMonth = tomorrow.get(Calendar.MONTH) + 1
            val tDay = tomorrow.get(Calendar.DAY_OF_MONTH)
            val tomorrowTimes = PrayerTimesTable.getPrayerTimes(tMonth, tDay)
            target = timeToCalendar(tomorrowTimes.fajr)
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diff = target.timeInMillis - System.currentTimeMillis()
        if (diff <= 0) return

        countdownTimer = object : CountDownTimer(diff, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = millisUntilFinished / 3600000
                val m = (millisUntilFinished % 3600000) / 60000
                val s = (millisUntilFinished % 60000) / 1000
                if (_binding != null) {
                    binding.textCountdown.text = String.format("%02d:%02d:%02d", h, m, s)
                }
            }

            override fun onFinish() {
                if (_binding != null) {
                    renderPrayerList()
                    startCountdown()
                }
            }
        }.start()
    }

    private fun PrayerType.timeFor(all: com.aseelan.adhan.data.PrayerTimes) = when (this) {
        PrayerType.FAJR -> all.fajr
        PrayerType.SHURUQ -> all.shuruq
        PrayerType.DHUHR -> all.dhuhr
        PrayerType.ASR -> all.asr
        PrayerType.MAGHRIB -> all.maghrib
        PrayerType.ISHA -> all.isha
    }

    private fun com.aseelan.adhan.data.PrayerTimes.timeFor(type: PrayerType) = type.timeFor(this)

    override fun onDestroyView() {
        countdownTimer?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
