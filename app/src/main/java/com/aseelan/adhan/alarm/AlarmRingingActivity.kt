package com.aseelan.adhan.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.databinding.ActivityAlarmRingingBinding

class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingingBinding

    companion object {
        private var currentInstance: AlarmRingingActivity? = null

        fun finishIfShowing() {
            currentInstance?.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = this
        setupShowOverLockScreen()

        binding = ActivityAlarmRingingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)
        val prayer = PrayerType.values().firstOrNull { it.key == prayerKey }
        binding.textRingingPrayerName.text = prayer?.arabicName ?: ""

        binding.btnStopRinging.setOnClickListener { stopAdhanAndClose() }
    }

    private fun setupShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            stopAdhanAndClose()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun stopAdhanAndClose() {
        val stopIntent = Intent(this, AdhanForegroundService::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        startService(stopIntent)
        finish()
    }

    override fun onDestroy() {
        if (currentInstance == this) {
            currentInstance = null
        }
        super.onDestroy()
    }
}
