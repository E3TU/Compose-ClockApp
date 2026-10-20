package com.example.clockapp.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.clockapp.R

enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    CLOCK(
        route = "clock",
        labelRes = R.string.nav_clock,
        iconRes = R.drawable.ic_clock
    ),
    ALARM(
        route = "alarm",
        labelRes = R.string.nav_alarm,
        iconRes = R.drawable.ic_alarm
    ),
    TIMER(
        route = "timer",
        labelRes = R.string.nav_timer,
        iconRes = R.drawable.ic_timer
    ),
    STOPWATCH(
        route = "stopwatch",
        labelRes = R.string.nav_stopwatch,
        iconRes = R.drawable.ic_stopwatch
    );

    companion object {
        val START = CLOCK

        fun fromRoute(route: String?): TopLevelDestination {
            return entries.firstOrNull { it.route == route } ?: START
        }
    }
}
