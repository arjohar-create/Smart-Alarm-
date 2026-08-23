package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.AlarmRingingActivity
import com.example.SmartAlarmApp
import com.example.service.AlarmService
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_TRIGGER_ALARM = "com.example.smartalarm.ACTION_TRIGGER_ALARM"
        const val ACTION_STOP_ALARM = "com.example.smartalarm.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.smartalarm.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_PROBLEM_COUNT = "extra_problem_count"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SOUND_TONE = "extra_sound_tone"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SmartAlarm:WakeLock"
        )
        wakeLock?.acquire(60_000L) // 1 minute lock

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                rescheduleAllAlarms(context)
            }

            ACTION_TRIGGER_ALARM -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0)
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
                val mathDiff = intent.getStringExtra(EXTRA_MATH_DIFFICULTY) ?: "EASY"
                val count = intent.getIntExtra(EXTRA_PROBLEM_COUNT, 1)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                val tone = intent.getStringExtra(EXTRA_SOUND_TONE) ?: "LOUD_ALARM"
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)

                // 1. Start Foreground Alarm Service for loud continuous sound and vibration
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_START
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmService.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmService.EXTRA_MATH_DIFFICULTY, mathDiff)
                    putExtra(AlarmService.EXTRA_PROBLEM_COUNT, count)
                    putExtra(AlarmService.EXTRA_VIBRATE, vibrate)
                    putExtra(AlarmService.EXTRA_SOUND_TONE, tone)
                    putExtra(AlarmService.EXTRA_RINGTONE_URI, ringtoneUri)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // 2. Launch Full-screen Ringing Activity
                val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmRingingActivity.EXTRA_MATH_DIFFICULTY, mathDiff)
                    putExtra(AlarmRingingActivity.EXTRA_PROBLEM_COUNT, count)
                    putExtra(AlarmService.EXTRA_RINGTONE_URI, ringtoneUri)
                }
                context.startActivity(activityIntent)

                // If non-repeating alarm, disable it in database
                if (alarmId != 999999) {
                    disableNonRepeatingAlarm(context, alarmId)
                }
            }

            ACTION_STOP_ALARM -> {
                val stopServiceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_STOP
                }
                context.startService(stopServiceIntent)
            }

            ACTION_SNOOZE_ALARM -> {
                val stopServiceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_SNOOZE
                }
                context.startService(stopServiceIntent)
                // Schedule snooze in 5 minutes (300 seconds)
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Snoozed Alarm"
                val mathDiff = intent.getStringExtra(EXTRA_MATH_DIFFICULTY) ?: "EASY"
                val tone = intent.getStringExtra(EXTRA_SOUND_TONE) ?: "LOUD_ALARM"
                val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
                AlarmScheduler.scheduleSnoozeAlarm(
                    context = context,
                    delaySeconds = 300,
                    label = label,
                    mathDifficulty = mathDiff,
                    tone = tone,
                    ringtoneUri = ringtoneUri
                )
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SmartAlarmApp.database.alarmDao()
                val alarms = dao.getEnabledAlarms()
                for (alarm in alarms) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on boot", e)
            }
        }
    }

    private fun disableNonRepeatingAlarm(context: Context, alarmId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = SmartAlarmApp.database.alarmDao()
                val alarm = dao.getAlarmById(alarmId)
                if (alarm != null && !alarm.isRepeating) {
                    dao.updateAlarm(alarm.copy(isEnabled = false))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating non-repeating alarm state", e)
            }
        }
    }
}
