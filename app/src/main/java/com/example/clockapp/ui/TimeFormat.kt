package com.example.clockapp.ui

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import com.example.clockapp.ui.theme.TimeFormatMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun resolveIs24Hour(context: Context, mode: TimeFormatMode): Boolean = when (mode) {
    TimeFormatMode.H12 -> false
    TimeFormatMode.H24 -> true
    TimeFormatMode.SYSTEM -> DateFormat.is24HourFormat(context)
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatTime(value: LocalDateTime, is24Hour: Boolean, withSeconds: Boolean): String {
    val locale = Locale.getDefault()
    val hourToken = if (is24Hour) "HH" else "hh"
    val pattern = buildString {
        append(hourToken)
        append(":mm")
        if (withSeconds) append(":ss")
        if (!is24Hour) append(" a")
    }
    return value.format(DateTimeFormatter.ofPattern(pattern, locale))
}

fun formatClockTime(hour: Int, minute: Int, is24Hour: Boolean): String {
    return if (is24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour % 12 == 0 -> 12
            else -> hour % 12
        }
        "$displayHour:%02d %s".format(minute, period)
    }
}

fun formatDuration(millis: Long): String {
    // Ceil so a running timer never reads 00:00 before it expires.
    val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** MM:SS.cc under an hour, H:MM:SS at or over. */
fun formatStopwatch(millis: Long): String {
    val clamped = millis.coerceAtLeast(0)
    val hours = clamped / 3_600_000
    val minutes = (clamped % 3_600_000) / 60_000
    val seconds = (clamped % 60_000) / 1000
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        val centis = (clamped % 1000) / 10
        "%02d:%02d.%02d".format(minutes, seconds, centis)
    }
}

/** Per-lap splits from cumulative lap totals. */
fun lapSplits(totals: List<Long>): List<Long> =
    totals.mapIndexed { index, total -> total - (totals.getOrNull(index - 1) ?: 0) }
