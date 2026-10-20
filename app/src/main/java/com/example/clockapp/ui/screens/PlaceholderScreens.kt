package com.example.clockapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(name = "Clock", modifier = modifier)
}

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(name = "Alarm", modifier = modifier)
}

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(name = "Timer", modifier = modifier)
}

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(name = "Stopwatch", modifier = modifier)
}

@Composable
private fun PlaceholderScreen(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
