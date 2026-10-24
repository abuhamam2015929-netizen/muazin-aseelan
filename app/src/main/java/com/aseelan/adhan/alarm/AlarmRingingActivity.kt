package com.aseelan.adhan.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.aseelan.adhan.data.PrayerType
import com.aseelan.adhan.databinding.ActivityAlarmRingingBinding

class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingingBinding
    private var stopped = false

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

        updatePrayerName(intent)

        binding.btnStopRinging.setOnClickListener { stopAdhanAndClose() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        stopped = false
        updatePrayerName(intent)
    }

    private fun updatePrayerName(intent: Intent?) {
        val prayerKey = intent?.getStringExtra(EXTRA_PRAYER_KEY)
        val prayer = PrayerType.values().firstOrNull { it.key == prayerKey }
        binding.textRingingPrayerName.text = prayer?.arabicName ?: ""
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

    // أي زر (ومنها زر الصوت) يوقف الأذان
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            stopAdhanAndClose()
        }
        return true
    }

    // لمس الشاشة في أي مكان يوقف الأذان
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            stopAdhanAndClose()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun stopAdhanAndClose() {
        if (stopped) return
        stopped = true
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
