package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.AlarmItem
import com.example.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMillis = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDaysMask)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_MATH_DIFFICULTY, alarm.mathDifficulty)
            putExtra(AlarmReceiver.EXTRA_PROBLEM_COUNT, alarm.mathProblemCount)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_SOUND_TONE, alarm.soundTone)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm ID ${alarm.id} at $triggerTimeMillis (in ${(triggerTimeMillis - System.currentTimeMillis()) / 1000}s)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback alarm scheduling failed", ex)
            }
        }
    }

    fun scheduleQuickTestAlarm(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, 999999)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "Test Alarm (5s)")
            putExtra(AlarmReceiver.EXTRA_MATH_DIFFICULTY, "EASY")
            putExtra(AlarmReceiver.EXTRA_PROBLEM_COUNT, 1)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(AlarmReceiver.EXTRA_SOUND_TONE, "LOUD_ALARM")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule quick test alarm", e)
        }
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        delaySeconds: Int = 300,
        label: String = "Snoozed Alarm",
        mathDifficulty: String = "EASY",
        tone: String = "LOUD_ALARM",
        ringtoneUri: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, 888888)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "$label (Snooze)")
            putExtra(AlarmReceiver.EXTRA_MATH_DIFFICULTY, mathDifficulty)
            putExtra(AlarmReceiver.EXTRA_PROBLEM_COUNT, 1)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
            putExtra(AlarmReceiver.EXTRA_SOUND_TONE, tone)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled 5-minute snooze alarm at $triggerTimeMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze alarm", e)
        }
    }

    fun cancelAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun calculateNextTriggerTime(hour: Int, minute: Int, repeatDaysMask: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        if (repeatDaysMask == 0) {
            // Once: if time has passed today, schedule for tomorrow
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        // Repeating on specific days
        // Calendar.DAY_OF_WEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
        // Bitmask: bit 0 = Sunday, 1 = Monday, ..., 6 = Saturday
        for (dayOffset in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            if (checkCal.timeInMillis > now) {
                val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon
                val bitIndex = dayOfWeek - 1 // 0=Sun, 1=Mon
                if ((repeatDaysMask and (1 shl bitIndex)) != 0) {
                    return checkCal.timeInMillis
                }
            }
        }

        // Fallback: 24h from today
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
