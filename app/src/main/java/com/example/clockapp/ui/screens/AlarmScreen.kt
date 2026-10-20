package com.example.clockapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockapp.data.Alarm
import com.example.clockapp.data.AlarmRepository
import com.example.clockapp.data.Days
import com.example.clockapp.ui.formatClockTime
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AlarmScreen(modifier: Modifier = Modifier, is24Hour: Boolean = true) {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val alarms by repository.alarms.collectAsStateWithLifecycle(initialValue = emptyList())

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        repository.rescheduleAll()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingAlarm = null
                    showTimePicker = true
                },
                icon = { Text("+") },
                text = { Text("New alarm") }
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

            if (alarms.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No alarms yet",
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
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            is24Hour = is24Hour,
                            onToggle = { enabled ->
                                scope.launch { repository.setEnabled(alarm, enabled) }
                            },
                            onDelete = { scope.launch { repository.delete(alarm) } },
                            onEdit = {
                                editingAlarm = alarm
                                showTimePicker = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        AlarmTimePickerDialog(
            initial = editingAlarm,
            is24Hour = is24Hour,
            onConfirm = { hour, minute, repeatMask ->
                showTimePicker = false
                scope.launch {
                    val existing = editingAlarm
                    if (existing != null) {
                        repository.update(
                            existing.copy(
                                hour = hour,
                                minute = minute,
                                repeatDays = repeatMask
                            )
                        )
                    } else {
                        repository.add(
                            Alarm(
                                hour = hour,
                                minute = minute,
                                repeatDays = repeatMask
                            )
                        )
                    }
                    editingAlarm = null
                }
            },
            onDismiss = {
                showTimePicker = false
                editingAlarm = null
            }
        )
    }
}

@Composable
private fun ExactAlarmBanner(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alarms will not fire until exact alarm permission is granted.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { context.requestExactAlarmSettings() }) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(onClick = onEdit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatClockTime(alarm.hour, alarm.minute, is24Hour),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = if (alarm.isEnabled) FontWeight.Normal else FontWeight.Light,
                    color = if (alarm.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (alarm.isRepeating) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Days.LABELS.forEachIndexed { index, label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (alarm.repeatsOn(index)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "One-time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimePickerDialog(
    initial: Alarm?,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int, repeatMask: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: now.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial?.minute ?: now.get(Calendar.MINUTE),
        is24Hour = is24Hour
    )
    var selectedDays by remember {
        mutableStateOf(
            initial?.repeatDays?.let { mask ->
                (0..6).filter { mask and (1 shl it) != 0 }.toSet()
            } ?: emptySet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New alarm" else "Edit alarm") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = { selectedDays = (0..6).toSet() }) {
                        Text("Every day")
                    }
                    TextButton(onClick = { selectedDays = (0..4).toSet() }) {
                        Text("Weekdays")
                    }
                    TextButton(onClick = { selectedDays = emptySet() }) {
                        Text("Off")
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Days.LABELS.forEachIndexed { index, label ->
                        val selected = index in selectedDays
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable {
                                    selectedDays = if (selected) {
                                        selectedDays - index
                                    } else {
                                        selectedDays + index
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        state.hour,
                        state.minute,
                        Days.maskOf(selectedDays)
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun Context.canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val manager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    return manager.canScheduleExactAlarms()
}

private fun Context.requestExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    startActivity(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
