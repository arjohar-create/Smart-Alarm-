package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmRingingActivity
import com.example.SmartAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        const val NOTIFICATION_ID = 404040

        const val ACTION_START = "com.example.smartalarm.ACTION_START_ALARM"
        const val ACTION_STOP = "com.example.smartalarm.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE = "com.example.smartalarm.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_MATH_DIFFICULTY = "extra_math_difficulty"
        const val EXTRA_PROBLEM_COUNT = "extra_problem_count"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SOUND_TONE = "extra_sound_tone"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"

        var isRinging = false
            private set
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioTrack: AudioTrack? = null
    private var toneJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping alarm service")
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Snoozing alarm service")
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val alarmId = intent?.getIntExtra(EXTRA_ALARM_ID, 0) ?: 0
                val label = intent?.getStringExtra(EXTRA_ALARM_LABEL) ?: "Smart Alarm"
                val mathDiff = intent?.getStringExtra(EXTRA_MATH_DIFFICULTY) ?: "EASY"
                val count = intent?.getIntExtra(EXTRA_PROBLEM_COUNT, 1) ?: 1
                val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
                val soundTone = intent?.getStringExtra(EXTRA_SOUND_TONE) ?: "LOUD_ALARM"
                val ringtoneUri = intent?.getStringExtra(EXTRA_RINGTONE_URI)

                isRinging = true
                val notification = buildForegroundNotification(alarmId, label, mathDiff, count)
                startForeground(NOTIFICATION_ID, notification)

                startRinging(soundTone, ringtoneUri)
                if (vibrate) {
                    startVibrating()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun buildForegroundNotification(
        alarmId: Int,
        label: String,
        difficulty: String,
        count: Int
    ): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_MATH_DIFFICULTY, difficulty)
            putExtra(EXTRA_PROBLEM_COUNT, count)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SmartAlarmApp.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Alarm Ringing: $label")
            .setContentText("Solve math problem to stop alarm!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }

    private fun startRinging(soundTone: String, customUriString: String?) {
        try {
            var alarmUri: Uri? = null
            if (!customUriString.isNullOrEmpty()) {
                try {
                    alarmUri = Uri.parse(customUriString)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing custom ringtone Uri: $customUriString", e)
                }
            }

            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (soundTone == "SIREN" || soundTone == "RADAR" || soundTone == "CYBER") {
                startSynthesizedTone(soundTone)
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                if (alarmUri != null) {
                    setDataSource(this@AlarmService, alarmUri)
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    prepare()
                    start()
                } else {
                    startSynthesizedTone("LOUD_ALARM")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed, fallback to high-intensity tone", e)
            startSynthesizedTone(soundTone)
        }
    }

    /**
     * Synthesizes customizable loud pulsed alarm tones (Siren / Cyber / Radar / High Alarm)
     */
    private fun startSynthesizedTone(toneType: String = "LOUD_ALARM") {
        toneJob?.cancel()
        toneJob = serviceScope.launch {
            val sampleRate = 44100
            val numSamples = sampleRate / 2
            val audioData = ShortArray(numSamples)

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                minBufferSize.coerceAtLeast(numSamples * 2),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            try {
                audioTrack?.play()
                var step = 0
                while (isActive) {
                    val freq = when (toneType) {
                        "SIREN" -> if (step % 2 == 0) 1400.0 else 700.0
                        "RADAR" -> (600.0 + (step % 5) * 200.0)
                        "CYBER" -> if (step % 3 == 0) 1800.0 else if (step % 3 == 1) 1200.0 else 900.0
                        else -> if (step % 2 == 0) 1200.0 else 800.0
                    }
                    step++
                    for (i in 0 until numSamples) {
                        val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                        val sample = (sin(angle) * Short.MAX_VALUE * 0.9).toInt().toShort()
                        audioData[i] = sample
                    }
                    audioTrack?.write(audioData, 0, numSamples)
                    delay(40)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Synthesized tone error", e)
            }
        }
    }

    private fun startVibrating() {
        try {
            val pattern = longArrayOf(0, 700, 300, 700, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    private fun stopAlarm() {
        isRinging = false
        toneJob?.cancel()
        toneJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio track", e)
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling vibration", e)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
