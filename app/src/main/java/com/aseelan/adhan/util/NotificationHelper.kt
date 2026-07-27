package com.aseelan.adhan.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.aseelan.adhan.R

object NotificationHelper {

    const val CHANNEL_ID_ADHAN = "channel_adhan"
    const val CHANNEL_ID_SERVICE = "channel_service"

    const val NOTIF_ID_SERVICE = 1001
    const val NOTIF_ID_ADHAN = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val adhanChannel = NotificationChannel(
            CHANNEL_ID_ADHAN,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setSound(null, null) // الصوت يُشغَّل يدوياً عبر MediaPlayer
            enableVibration(true)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            context.getString(R.string.notif_ongoing_title),
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(adhanChannel)
        manager.createNotificationChannel(serviceChannel)
    }
}
