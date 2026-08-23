package com.example.data

import com.example.model.AlarmItem
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<AlarmItem>> = alarmDao.getAllAlarms()

    suspend fun getEnabledAlarms(): List<AlarmItem> = alarmDao.getEnabledAlarms()

    suspend fun getAlarmById(id: Int): AlarmItem? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: AlarmItem): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmItem) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: AlarmItem) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Int) = alarmDao.deleteAlarmById(id)
}
