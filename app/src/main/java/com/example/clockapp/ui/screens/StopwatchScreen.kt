package com.example.clockapp.ui.screens

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockapp.data.StopwatchRepository
import com.example.clockapp.data.StopwatchState
import com.example.clockapp.ui.formatStopwatch
import com.example.clockapp.ui.lapSplits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { StopwatchRepository(context) }
    val scope = rememberCoroutineScope()
    val state by repository.state.collectAsStateWithLifecycle(initialValue = StopwatchState())

    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    // 16 ms tick drives the centisecond display while running.
    LaunchedEffect(state.isRunning) {
        if (!state.isRunning) return@LaunchedEffect
        while (true) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(16)
        }
    }

    // Re-anchor persistence every 5 s so a reboot costs at most that much accuracy.
    LaunchedEffect(state.isRunning) {
        if (!state.isRunning) return@LaunchedEffect
        while (true) {
            delay(5_000)
            repository.heartbeat()
        }
    }

    val view = LocalView.current
    DisposableEffect(state.isRunning) {
        val window = (view.context as? Activity)?.window
        if (state.isRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val elapsed = state.elapsedAt(nowElapsed)
    val splits = remember(state.laps) { lapSplits(state.laps) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = stopwatchDisplay(elapsed),
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.isRunning) {
                    FilledTonalButton(onClick = { scope.launch { repository.lap() } }) {
                        Text("Lap")
                    }
                    Button(onClick = { scope.launch { repository.pause() } }) {
                        Text("Pause")
                    }
                } else {
                    Button(onClick = { scope.launch { repository.startOrResume() } }) {
                        Text(if (elapsed > 0) "Resume" else "Start")
                    }
                    FilledTonalButton(
                        onClick = { scope.launch { repository.reset() } },
                        enabled = state.laps.isNotEmpty() || elapsed > 0
                    ) {
                        Text("Reset")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (state.laps.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    items(
                        count = state.laps.size,
                        key = { state.laps.size - it }
                    ) { index ->
                        val lapNumber = state.laps.size - index
                        LapRow(
                            number = lapNumber,
                            split = splits[lapNumber - 1],
                            total = state.laps[lapNumber - 1]
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "No laps yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LapRow(number: Int, split: Long, total: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "Lap $number",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatStopwatch(split),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "  ${formatStopwatch(total)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun stopwatchDisplay(millis: Long): androidx.compose.ui.text.AnnotatedString {
    val clamped = millis.coerceAtLeast(0)
    val hours = clamped / 3_600_000
    val minutes = (clamped % 3_600_000) / 60_000
    val seconds = (clamped % 60_000) / 1000
    return if (hours > 0) {
        androidx.compose.ui.text.AnnotatedString("%d:%02d:%02d".format(hours, minutes, seconds))
    } else {
        val centis = (clamped % 1000) / 10
        buildAnnotatedString {
            append("%02d:%02d".format(minutes, seconds))
            withStyle(
                SpanStyle(
                    fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.6f,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            ) {
                append(".%02d".format(centis))
            }
        }
    }
}
