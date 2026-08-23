package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartAlarmApp
import com.example.data.AlarmRepository
import com.example.model.AlarmItem
import com.example.receiver.AlarmReceiver
import com.example.util.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
    private val repository = AlarmRepository(SmartAlarmApp.database.alarmDao())

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
    }

    val alarms: StateFlow<List<AlarmItem>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _testAlarmCountdown = MutableStateFlow<Int?>(null)
    val testAlarmCountdown: StateFlow<Int?> = _testAlarmCountdown.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmItem?>(null)
    val editingAlarm: StateFlow<AlarmItem?> = _editingAlarm.asStateFlow()

    private val _isTimePickerOpen = MutableStateFlow(false)
    val isTimePickerOpen: StateFlow<Boolean> = _isTimePickerOpen.asStateFlow()

    init {
        // Pre-populate with a starter default alarm if empty
        viewModelScope.launch(Dispatchers.IO) {
            val list = SmartAlarmApp.database.alarmDao().getEnabledAlarms()
            if (list.isEmpty()) {
                val defaultAlarm = AlarmItem(
                    hour = 7,
                    minute = 30,
                    label = "Morning Wake Up",
                    isEnabled = true,
                    repeatDaysMask = 62, // Mon-Fri
                    mathDifficulty = "EASY",
                    mathProblemCount = 1,
                    isVibrationEnabled = true,
                    soundTone = "LOUD_ALARM",
                    ringtoneTitle = "Loud Alarm (Default)"
                )
                val id = repository.insertAlarm(defaultAlarm)
                AlarmScheduler.scheduleAlarm(application, defaultAlarm.copy(id = id.toInt()))
            }
        }
    }

    fun openNewAlarmDialog() {
        _editingAlarm.value = null
        _isTimePickerOpen.value = true
    }

    fun openEditAlarmDialog(alarm: AlarmItem) {
        _editingAlarm.value = alarm
        _isTimePickerOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerOpen.value = false
        _editingAlarm.value = null
    }

    fun saveAlarm(
        hour: Int,
        minute: Int,
        label: String,
        repeatDaysMask: Int,
        mathDifficulty: String,
        mathProblemCount: Int,
        isVibrationEnabled: Boolean,
        soundTone: String,
        ringtoneUri: String? = null,
        ringtoneTitle: String = "Loud Alarm (Default)"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _editingAlarm.value
            if (current != null) {
                val updated = current.copy(
                    hour = hour,
                    minute = minute,
                    label = label,
                    isEnabled = true,
                    repeatDaysMask = repeatDaysMask,
                    mathDifficulty = mathDifficulty,
                    mathProblemCount = mathProblemCount,
                    isVibrationEnabled = isVibrationEnabled,
                    soundTone = soundTone,
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle
                )
                repository.updateAlarm(updated)
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                val newAlarm = AlarmItem(
                    hour = hour,
                    minute = minute,
                    label = label.ifBlank { "Alarm" },
                    isEnabled = true,
                    repeatDaysMask = repeatDaysMask,
                    mathDifficulty = mathDifficulty,
                    mathProblemCount = mathProblemCount,
                    isVibrationEnabled = isVibrationEnabled,
                    soundTone = soundTone,
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle
                )
                val id = repository.insertAlarm(newAlarm)
                AlarmScheduler.scheduleAlarm(getApplication(), newAlarm.copy(id = id.toInt()))
            }
        }
        closeTimePickerDialog()
    }

    fun toggleAlarm(alarm: AlarmItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmItem) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelAlarm(getApplication(), alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun triggerQuickTest(context: Context) {
        viewModelScope.launch {
            _testAlarmCountdown.value = 5
            AlarmScheduler.scheduleQuickTestAlarm(context, delaySeconds = 5)
            for (i in 5 downTo 1) {
                _testAlarmCountdown.value = i
                kotlinx.coroutines.delay(1000)
            }
            _testAlarmCountdown.value = null
        }
    }

    fun stopAlarm(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
        }
        context.sendBroadcast(intent)
    }

    fun snoozeAlarm(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
        }
        context.sendBroadcast(intent)
    }
}
