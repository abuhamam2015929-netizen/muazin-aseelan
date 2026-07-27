package com.aseelan.adhan.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.aseelan.adhan.MainActivity
import com.aseelan.adhan.R
import com.aseelan.adhan.data.AlertMode
import com.aseelan.adhan.data.MuadhinList
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.data.SettingsRepository
import com.aseelan.adhan.util.NotificationHelper

const val ACTION_STOP_ADHAN = "com.aseelan.adhan.action.STOP_ADHAN"

/**
 * خدمة أمامية (Foreground Service) تُشغَّل عند حلول وقت الصلاة أو التذكير المسبق.
 * تعمل حتى لو كان التطبيق مغلقاً تماماً، وتُظهر إشعاراً وتشغّل صوت الأذان
 * حسب وضع التنبيه المختار لتلك الصلاة.
 */
class AdhanForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ADHAN) {
            stopAdhanAndService()
            return START_NOT_STICKY
        }

        val prayerKey = intent?.getStringExtra(EXTRA_PRAYER_KEY)
        val isPreReminder = intent?.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false) ?: false
        val prayer = PrayerType.values().firstOrNull { it.key == prayerKey }

        if (prayer == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val settings = SettingsRepository(this)
        val mode = settings.getAlertMode(prayer)

        val notification = buildNotification(prayer, isPreReminder)
        startForeground(NotificationHelper.NOTIF_ID_ADHAN, notification)

        if (!isPreReminder) {
            when (mode) {
                AlertMode.FULL_ADHAN -> playFullAdhan(prayer, settings)
                AlertMode.SHORT_BEEP -> playShortBeep()
                AlertMode.SILENT -> vibrateOnly()
                AlertMode.OFF -> stopAdhanAndService()
            }
        } else {
            // التذكير المسبق: اهتزاز خفيف + إشعار فقط، بدون أذان كامل
            vibrateOnly()
            stopSelfAfterDelay(4000)
        }

        return START_NOT_STICKY
    }

    private fun playFullAdhan(prayer: PrayerType, settings: SettingsRepository) {
        val muadhinId = settings.getMuadhinId(prayer)
        val muadhin = MuadhinList.byId(muadhinId)
        val resId = resources.getIdentifier(muadhin.rawResName, "raw", packageName)

        if (resId == 0) {
            // لم يتم رفع ملف الأذان الصوتي بعد - نكتفي بنغمة قصيرة + اهتزاز
            playShortBeep()
            vibrateOnly()
            stopSelfAfterDelay(6000)
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = resources.openRawResourceFd(resId)
                if (afd != null) {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                setOnCompletionListener { stopAdhanAndService() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopAdhanAndService()
        }
    }

    private fun playShortBeep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 1500)
        } catch (_: Exception) {
        }
    }

    private fun vibrateOnly() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
        }
    }

    private fun stopSelfAfterDelay(delayMs: Long) {
        android.os.Handler(mainLooper).postDelayed({ stopAdhanAndService() }, delayMs)
    }

    private fun stopAdhanAndService() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(prayer: PrayerType, isPreReminder: Boolean): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanForegroundService::class.java).apply { action = ACTION_STOP_ADHAN }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreReminder) {
            getString(R.string.notif_pre_reminder, SettingsRepository(this).getPreReminderMinutes(prayer), prayer.arabicName)
        } else {
            getString(R.string.notif_prayer_time, prayer.arabicName)
        }

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.stop_azan), stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
