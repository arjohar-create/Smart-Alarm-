package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ads.AdMobManager
import com.example.receiver.AlarmReceiver
import com.example.ui.screens.AlarmRingingScreen
import com.example.ui.theme.MyApplicationTheme

class AlarmRingingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_PROBLEM_COUNT = "extra_problem_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload Interstitial ad so it's ready when dismissed
        AdMobManager.loadInterstitialAd(this)

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up!"
        val mathDifficulty = intent.getStringExtra(EXTRA_MATH_DIFFICULTY) ?: "EASY"
        val problemCount = intent.getIntExtra(EXTRA_PROBLEM_COUNT, 1)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AlarmRingingScreen(
                    alarmLabel = alarmLabel,
                    mathDifficulty = mathDifficulty,
                    targetProblemCount = problemCount,
                    onAlarmDismissed = {
                        stopAlarm()
                        // Show Interstitial Ad upon dismissal as requested
                        AdMobManager.showInterstitialAd(this@AlarmRingingActivity) {
                            finish()
                        }
                    },
                    onSnooze = {
                        snoozeAlarm()
                        finish()
                    }
                )
            }
        }
    }

    private fun stopAlarm() {
        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
        }
        sendBroadcast(stopIntent)
    }

    private fun snoozeAlarm() {
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
        }
        sendBroadcast(snoozeIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent bypassing the math challenge with back button
    }
}

