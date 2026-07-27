package com.aseelan.adhan

import android.app.Application
import com.aseelan.adhan.alarm.AlarmScheduler
import com.aseelan.adhan.util.NotificationHelper

class AzanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        AlarmScheduler.scheduleAll(this)
    }
}
