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
import com.aseelan.adhan.databinding.ItemWeekDayBinding
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

    // اليوم الذي يُعرض حالياً في قائمة المواقيت وسطر التاريخ (قابل للتغيير من شريط الأسبوع)
    private var selectedCalendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsRepository(requireContext())
        selectedCalendar = Calendar.getInstance()

        binding.btnSettings.setOnClickListener { openTab(SettingsFragment()) }
        binding.btnBell.setOnClickListener { openTab(SettingsFragment()) }
        binding.btnQiblaShortcut.setOnClickListener { openTab(QiblaFragment()) }

        renderWeekStrip()
        renderDates()
        renderPrayerList()
        startCountdown()
    }

    private fun openTab(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * يبني شريط أيام الأسبوع الحالي (من الأحد إلى السبت) مع تمييز اليوم المختار حالياً،
     * ويسمح بالنقر على أي يوم لعرض مواقيته في القائمة أسفله.
     */
    private fun renderWeekStrip() {
        binding.weekStripContainer.removeAllViews()

        val today = Calendar.getInstance()
        val weekStart = today.clone() as Calendar
        // نرجع لبداية الأسبوع (الأحد)
        val diffToSunday = weekStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        weekStart.add(Calendar.DAY_OF_MONTH, -diffToSunday)

        val dayNameFormat = SimpleDateFormat("EEE", Locale("ar"))

        for (i in 0..6) {
            val dayCal = weekStart.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)

            val cellBinding = ItemWeekDayBinding.inflate(layoutInflater, binding.weekStripContainer, false)
            cellBinding.textWeekDayName.text = dayNameFormat.format(dayCal.time)
            cellBinding.textWeekDayNumber.text = dayCal.get(Calendar.DAY_OF_MONTH).toString()

            val isSelected = isSameDay(dayCal, selectedCalendar)
            applyWeekDayStyle(cellBinding, isSelected)

            cellBinding.weekDayRoot.setOnClickListener {
                selectedCalendar = dayCal.clone() as Calendar
                renderWeekStrip()
                renderDates()
                renderPrayerList()
            }

            binding.weekStripContainer.addView(cellBinding.root)
        }
    }

    private fun applyWeekDayStyle(cellBinding: ItemWeekDayBinding, isSelected: Boolean) {
        if (isSelected) {
            cellBinding.weekDayRoot.setBackgroundResource(R.drawable.bg_week_day_selected)
            cellBinding.textWeekDayName.setTextColor(resources.getColor(R.color.off_white, null))
            cellBinding.textWeekDayNumber.setTextColor(resources.getColor(R.color.gold, null))
        } else {
            cellBinding.weekDayRoot.setBackgroundResource(R.drawable.bg_week_day)
            cellBinding.textWeekDayName.setTextColor(resources.getColor(R.color.olive_dark, null))
            cellBinding.textWeekDayNumber.setTextColor(resources.getColor(R.color.olive_dark, null))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun renderDates() {
        val dayNameFormat = SimpleDateFormat("EEEE", Locale("ar"))
        val gregorianFormat = SimpleDateFormat("d MMMM yyyy", Locale("ar"))

        binding.textGregorianDate.text =
            "${dayNameFormat.format(selectedCalendar.time)} - ${gregorianFormat.format(selectedCalendar.time)}"

        val hijri = HijriDateConverter.fromGregorian(selectedCalendar, settings.getHijriDayOffset())
        binding.textHijriDate.text = hijri.formatted()
    }

    private fun renderPrayerList() {
        binding.prayerListContainer.removeAllViews()
        val month = selectedCalendar.get(Calendar.MONTH) + 1
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
        val times = PrayerTimesTable.getPrayerTimes(month, day)

        val isToday = isSameDay(selectedCalendar, Calendar.getInstance())
        val nextPrayer = if (isToday) findNextPrayer() else null

        val rows = listOf(
            Triple(PrayerType.FAJR, times.fajr, R.drawable.ic_fajr),
            Triple(PrayerType.SHURUQ, times.shuruq, R.drawable.ic_shuruq),
            Triple(PrayerType.DHUHR, times.dhuhr, R.drawable.ic_mosque),
            Triple(PrayerType.ASR, times.asr, R.drawable.ic_asr),
            Triple(PrayerType.MAGHRIB, times.maghrib, R.drawable.ic_maghrib),
            Triple(PrayerType.ISHA, times.isha, R.drawable.ic_isha)
        )

        for ((prayer, time, iconRes) in rows) {
            val itemBinding = ItemPrayerBinding.inflate(layoutInflater, binding.prayerListContainer, false)
