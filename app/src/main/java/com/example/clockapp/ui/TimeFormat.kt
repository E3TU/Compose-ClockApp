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
