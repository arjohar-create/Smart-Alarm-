package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Wake Up!",
    val isEnabled: Boolean = true,
    // Bitmask for repeating days: bit 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat. 0 = Once
    val repeatDaysMask: Int = 0,
    val mathDifficulty: String = "EASY", // EASY, MEDIUM, HARD
    val mathProblemCount: Int = 1,       // 1, 2, 3
    val isVibrationEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val soundTone: String = "LOUD_ALARM", // LOUD_ALARM, SIREN, RADAR, CYBER, CUSTOM
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Loud Alarm (Default)",
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val m = String.format("%02d", minute)
            val amPm = if (hour >= 12) "PM" else "AM"
            return "$h:$m $amPm"
        }

    val daysSummary: String
        get() {
            if (repeatDaysMask == 0) return "Once"
            if (repeatDaysMask == 127) return "Every day"
            if (repeatDaysMask == 62) return "Weekdays"
            if (repeatDaysMask == 65) return "Weekends"

            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val activeDays = mutableListOf<String>()
            for (i in 0..6) {
                if ((repeatDaysMask and (1 shl i)) != 0) {
                    activeDays.add(days[i])
                }
            }
            return activeDays.joinToString(", ")
        }

    val isRepeating: Boolean
        get() = repeatDaysMask != 0
}
