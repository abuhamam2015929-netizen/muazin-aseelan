package com.aseelan.adhan.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aseelan.adhan.data.AlertMode
import com.aseelan.adhan.data.PrayerTimesTable
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.data.SettingsRepository
import java.util.Calendar

const val EXTRA_PRAYER_KEY = "extra_prayer_key"
const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"

/**
 * مسؤول عن جدولة كل تنبيهات اليوم (والأيام القادمة القليلة) باستخدام
 * AlarmManager.setExactAndAllowWhileIdle لضمان الدقة حتى في وضع Doze.
 */
object AlarmScheduler {

    private const val DAYS_TO_SCHEDULE_AHEAD = 2

    fun scheduleAll(context: Context) {
        val settings = SettingsRepository(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        for (dayOffset in 0..DAYS_TO_SCHEDULE_AHEAD) {
            val day = now.clone() as Calendar
            day.add(Calendar.DAY_OF_YEAR, dayOffset)
            scheduleForDay(context, alarmManager, settings, day)
        }
    }

    private fun scheduleForDay(
        context: Context,
        alarmManager: AlarmManager,
        settings: SettingsRepository,
        day: Calendar
    ) {
        val month = day.get(Calendar.MONTH) + 1
        val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
        val times = PrayerTimesTable.getPrayerTimes(month, dayOfMonth)
        val timeMap = mapOf(
            PrayerType.FAJR to times.fajr,
            PrayerType.DHUHR to times.dhuhr,
            PrayerType.ASR to times.asr,
            PrayerType.MAGHRIB to times.maghrib,
            PrayerType.ISHA to times.isha
        )

        for ((prayer, hhmm) in timeMap) {
            val mode = settings.getAlertMode(prayer)
            if (mode == AlertMode.OFF) continue

            val offsetMinutes = settings.getManualOffsetMinutes(prayer)
            val target = parseTimeToCalendar(day, hhmm)
            target.add(Calendar.MINUTE, offsetMinutes)

            if (target.timeInMillis > System.currentTimeMillis()) {
                scheduleExact(context, alarmManager, prayer, target, isPreReminder = false)
            }

            val preMinutes = settings.getPreReminderMinutes(prayer)
            if (preMinutes > 0) {
                val preTarget = target.clone() as Calendar
                preTarget.add(Calendar.MINUTE, -preMinutes)
                if (preTarget.timeInMillis > System.currentTimeMillis()) {
                    scheduleExact(context, alarmManager, prayer, preTarget, isPreReminder = true)
                }
            }
        }
    }

    private fun scheduleExact(
        context: Context,
        alarmManager: AlarmManager,
        prayer: PrayerType,
        target: Calendar,
        isPreReminder: Boolean
    ) {
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_KEY, prayer.key)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
        val requestCode = requestCodeFor(prayer, target, isPreReminder)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
            )
        }
    }

    private fun requestCodeFor(prayer: PrayerType, target: Calendar, isPreReminder: Boolean): Int {
        val dayCode = target.get(Calendar.DAY_OF_YEAR)
        val base = prayer.ordinal * 1000 + dayCode
        return if (isPreReminder) base + 500 else base
    }

    private fun parseTimeToCalendar(day: Calendar, hhmm: String): Calendar {
        val parts = hhmm.split(":")
        val cal = day.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        cal.set(Calendar.MINUTE, parts[1].toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
