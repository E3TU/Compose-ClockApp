package com.example.clockapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockapp.data.Timer
import com.example.clockapp.data.TimerRepository
import com.example.clockapp.ui.ExactAlarmBanner
import com.example.clockapp.ui.canScheduleExactAlarms
import com.example.clockapp.ui.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { TimerRepository(context) }
    val scope = rememberCoroutineScope()
    val timers by repository.timers.collectAsStateWithLifecycle(initialValue = emptyList())

    var showDurationDialog by remember { mutableStateOf(false) }

    // 100 ms tick drives the remaining-time display for running timers.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timers.any { it.isRunning }) {
        if (timers.none { it.isRunning }) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }

    LaunchedEffect(Unit) {
        repository.rescheduleAll()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDurationDialog = true },
                icon = { Text("+") },
                text = { Text("New timer") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!context.canScheduleExactAlarms()) {
                ExactAlarmBanner(context = context)
            }

            if (timers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No timers yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            nowMillis = now,
                            onStart = { scope.launch { repository.start(timer) } },
                            onPause = { scope.launch { repository.pause(timer) } },
                            onAddMinute = { scope.launch { repository.addMinute(timer) } },
                            onDelete = { scope.launch { repository.delete(timer) } }
                        )
                    }
                }
            }
        }
    }

    if (showDurationDialog) {
        TimerDurationDialog(
            onConfirm = { label, durationMillis ->
                showDurationDialog = false
                scope.launch { repository.add(label, durationMillis, startNow = true) }
            },
            onDismiss = { showDurationDialog = false }
        )
    }
}

@Composable
private fun TimerRow(
    timer: Timer,
    nowMillis: Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onAddMinute: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingMillis = when {
        timer.isRunning -> (timer.endsAtMillis - nowMillis).coerceAtLeast(0)
        else -> timer.remainingMillis
    }
    val expired = remainingMillis <= 0
    val fraction = if (timer.durationMillis > 0) {
        (remainingMillis.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val progressColor = if (expired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Canvas(modifier = Modifier.size(96.dp)) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = stroke
                    )
                }
                Text(
                    text = if (expired) "0:00" else formatDuration(remainingMillis),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label.ifBlank { "Timer" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expired) {
                    Text(
                        text = "Time's up",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = if (timer.isRunning) "Running" else "Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!expired) {
                    TextButton(onClick = if (timer.isRunning) onPause else onStart) {
                        Text(if (timer.isRunning) "Pause" else "Start")
                    }
                    if (timer.isRunning) {
                        TextButton(onClick = onAddMinute) {
                            Text("+1:00")
                        }
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
