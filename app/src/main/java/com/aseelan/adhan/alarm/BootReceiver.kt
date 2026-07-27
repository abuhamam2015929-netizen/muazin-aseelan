package com.aseelan.adhan.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * يعيد جدولة كل تنبيهات الأذان بعد إعادة تشغيل الجهاز أو تحديث التطبيق،
 * لأن AlarmManager يفقد كل المنبهات المجدولة عند إعادة التشغيل.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            AlarmScheduler.scheduleAll(context)
        }
    }
}
