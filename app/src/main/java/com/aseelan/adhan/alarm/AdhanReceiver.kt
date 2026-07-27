package com.aseelan.adhan.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.aseelan.adhan.data.PrayerType

/**
 * يستقبل بث AlarmManager عند حلول وقت الصلاة (أو التذكير المسبق)
 * ويشغّل خدمة أمامية (Foreground Service) لتشغيل الأذان أو إظهار الإشعار
 * حتى لو كان التطبيق مغلقاً تماماً.
 */
class AdhanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
        val prayer = PrayerType.values().firstOrNull { it.key == prayerKey } ?: return

        val serviceIntent = Intent(context, AdhanForegroundService::class.java).apply {
            putExtra(EXTRA_PRAYER_KEY, prayer.key)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // إعادة جدولة الأيام القادمة لضمان استمرار التنبيهات
        AlarmScheduler.scheduleAll(context)
    }
}
