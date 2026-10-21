package com.example.clockapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Do not import java.util.Timer in this module; this class shadows it.
 */
@Entity(tableName = "timers")
data class Timer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val durationMillis: Long,
    // Snapshot of the remaining time while paused. Ignored while running.
    val remainingMillis: Long = durationMillis,
    // Wall-clock end time. 0 = not running.
    val endsAtMillis: Long = 0,
    val isRunning: Boolean = false
)
