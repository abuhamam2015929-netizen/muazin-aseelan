package com.aseelan.adhan.data

/** أنواع الصلوات الخمس + الشروق */
enum class PrayerType(val key: String, val arabicName: String, val hasAdhan: Boolean) {
    FAJR("fajr", "الفجر", true),
    SHURUQ("shuruq", "الشروق", false),
    DHUHR("dhuhr", "الظهر", true),
    ASR("asr", "العصر", true),
    MAGHRIB("maghrib", "المغرب", true),
    ISHA("isha", "العشاء", true);

    companion object {
        val adhanPrayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

/** أوضاع التنبيه لكل صلاة */
enum class AlertMode(val key: String, val arabicLabel: String) {
    FULL_ADHAN("full", "أذان كامل"),
    SHORT_BEEP("short", "تنبيه قصير"),
    SILENT("silent", "صامت"),
    OFF("off", "إيقاف");

    companion object {
        fun fromKey(key: String?): AlertMode = values().firstOrNull { it.key == key } ?: FULL_ADHAN
    }
}

/** خيارات التذكير المسبق بالدقائق */
enum class PreReminderOption(val minutes: Int, val arabicLabel: String) {
    NONE(0, "بدون تذكير"),
    FIVE(5, "قبل 5 دقائق"),
    TEN(10, "قبل 10 دقائق"),
    FIFTEEN(15, "قبل 15 دقيقة");

    companion object {
        fun fromMinutes(m: Int): PreReminderOption = values().firstOrNull { it.minutes == m } ?: NONE
    }
}

/** عرض موقّت لصلاة واحدة في الشاشة الرئيسية */
data class PrayerDisplayItem(
    val type: PrayerType,
    val time: String,        // HH:mm بعد تطبيق الإزاحة اليدوية
    val alertMode: AlertMode,
    val isNext: Boolean = false
)
