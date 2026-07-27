package com.aseelan.adhan.data

import android.content.Context
import android.content.SharedPreferences

/**
 * إدارة كل إعدادات المستخدم: وضع التنبيه لكل صلاة، المؤذن المختار،
 * التذكير المسبق، الإزاحات اليدوية للوقت والتاريخ الهجري.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("muazin_aseelan_prefs", Context.MODE_PRIVATE)

    // ---------- وضع التنبيه لكل صلاة ----------
    fun getAlertMode(prayer: PrayerType): AlertMode =
        AlertMode.fromKey(prefs.getString("alert_${prayer.key}", AlertMode.FULL_ADHAN.key))

    fun setAlertMode(prayer: PrayerType, mode: AlertMode) {
        prefs.edit().putString("alert_${prayer.key}", mode.key).apply()
    }

    // ---------- المؤذن المختار لكل صلاة (افتراضياً نفس المؤذن العام) ----------
    fun getMuadhinId(prayer: PrayerType): Int =
        prefs.getInt("muadhin_${prayer.key}", getGlobalMuadhinId())

    fun setMuadhinId(prayer: PrayerType, muadhinId: Int) {
        prefs.edit().putInt("muadhin_${prayer.key}", muadhinId).apply()
    }

    fun getGlobalMuadhinId(): Int = prefs.getInt("muadhin_global", MuadhinList.default.id)

    fun setGlobalMuadhinId(id: Int) {
        prefs.edit().putInt("muadhin_global", id).apply()
    }

    // ---------- التذكير المسبق ----------
    fun getPreReminderMinutes(prayer: PrayerType): Int =
        prefs.getInt("pre_reminder_${prayer.key}", 0)

    fun setPreReminderMinutes(prayer: PrayerType, minutes: Int) {
        prefs.edit().putInt("pre_reminder_${prayer.key}", minutes).apply()
    }

    // ---------- تعديل يدوي للوقت بالدقائق (+/-) ----------
    fun getManualOffsetMinutes(prayer: PrayerType): Int =
        prefs.getInt("offset_${prayer.key}", 0)

    fun setManualOffsetMinutes(prayer: PrayerType, minutes: Int) {
        prefs.edit().putInt("offset_${prayer.key}", minutes).apply()
    }

    // ---------- تعديل يدوي للتاريخ الهجري بالأيام (+/-) ----------
    fun getHijriDayOffset(): Int = prefs.getInt("hijri_offset_days", 0)

    fun setHijriDayOffset(days: Int) {
        prefs.edit().putInt("hijri_offset_days", days).apply()
    }

    // ---------- هل تم جدولة المنبهات آخر مرة (لإعادة الجدولة بعد إعادة التشغيل) ----------
    fun setLastScheduledDay(dayOfYear: Int) {
        prefs.edit().putInt("last_scheduled_day", dayOfYear).apply()
    }

    fun getLastScheduledDay(): Int = prefs.getInt("last_scheduled_day", -1)
}
