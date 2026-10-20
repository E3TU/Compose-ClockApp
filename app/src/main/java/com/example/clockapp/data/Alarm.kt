package com.example.clockapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    // Repeat days as bitmask: bit 0 = Monday ... bit 6 = Sunday. 0 = one-shot alarm.
    val repeatDays: Int = 0
) {
    val isRepeating: Boolean get() = repeatDays != 0

    fun repeatsOn(dayIndex: Int): Boolean = repeatDays and (1 shl dayIndex) != 0
}

object Days {
    val LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

    fun maskOf(dayIndices: Set<Int>): Int =
        dayIndices.fold(0) { mask, day -> mask or (1 shl day) }
}
