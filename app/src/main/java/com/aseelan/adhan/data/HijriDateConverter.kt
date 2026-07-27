package com.aseelan.adhan.data

import java.util.Calendar

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    companion object {
        val monthNames = arrayOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
    }

    fun formatted(): String = "$day ${monthNames[(month - 1).coerceIn(0, 11)]} $year هـ"
}

/**
 * تحويل تقريبي (حسابي - Kuwaiti algorithm) من الميلادي إلى الهجري.
 * دقة كافية للاستخدام اليومي، مع إمكانية تعديل يدوي بالأيام من الإعدادات.
 */
object HijriDateConverter {

    fun fromGregorian(cal: Calendar, manualOffsetDays: Int = 0): HijriDate {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, manualOffsetDays)

        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH) + 1
        val year = c.get(Calendar.YEAR)

        val jd = gregorianToJulianDay(year, month, day)
        return julianDayToHijri(jd)
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return (day + ((153 * m + 2) / 5) + 365L * y + y / 4 - y / 100 + y / 400 - 32045).toLong()
    }

    private fun julianDayToHijri(jd: Long): HijriDate {
        val islamicEpoch = 1948440L // بداية التقويم الهجري بالتقويم اليولياني
        val daysSinceEpoch = jd - islamicEpoch + 1
        val cycles = daysSinceEpoch / 10631.0
        var year = (30 * cycles + 1).toInt()
        var remaining = daysSinceEpoch - hijriYearStart(year)
        while (remaining < 0) {
            year -= 1
            remaining = daysSinceEpoch - hijriYearStart(year)
        }
        while (remaining >= hijriYearLength(year)) {
            remaining -= hijriYearLength(year)
            year += 1
        }
        var month = 1
        while (month <= 12) {
            val ml = hijriMonthLength(year, month)
            if (remaining < ml) break
            remaining -= ml
            month += 1
        }
        val day = remaining + 1
        return HijriDate(day.toInt(), month, year)
    }

    private fun isHijriLeap(year: Int): Boolean = (11 * year + 14) % 30 < 11

    private fun hijriYearLength(year: Int): Int = if (isHijriLeap(year)) 355 else 354

    private fun hijriMonthLength(year: Int, month: Int): Int {
        if (month % 2 == 1) return 30
        if (month == 12 && isHijriLeap(year)) return 30
        return 29
    }

    private fun hijriYearStart(year: Int): Long {
        var total = 0L
        for (y in 1 until year) total += hijriYearLength(y)
        return total
    }
}
