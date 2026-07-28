package com.aseelan.adhan.util

object TimeFormat {
    fun to12Hour(hhmm: String): String {
        val parts = hhmm.split(":")
        if (parts.size != 2) return hhmm
        val hour24 = parts[0].toIntOrNull() ?: return hhmm
        val minute = parts[1].toIntOrNull() ?: return hhmm

        val isPm = hour24 >= 12
        var hour12 = hour24 % 12
        if (hour12 == 0) hour12 = 12
        val suffix = if (isPm) "م" else "ص"

        return String.format("%d:%02d %s", hour12, minute, suffix)
    }
}
