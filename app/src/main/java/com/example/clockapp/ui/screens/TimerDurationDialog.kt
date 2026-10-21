package com.example.clockapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clockapp.ui.TimerInputLogic

@Composable
fun TimerDurationDialog(
    onConfirm: (label: String, durationMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    val presets = remember {
        listOf(
            "1m" to 60,
            "3m" to 180,
            "5m" to 300,
            "10m" to 600,
            "15m" to 900,
            "30m" to 1800,
            "1h" to 3600
        )
    }
    val selectedSeconds = TimerInputLogic.totalSeconds(input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New timer") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = TimerInputLogic.format(input),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "HH : MM : SS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (name, presetSeconds) ->
                        FilterChip(
                            selected = selectedSeconds == presetSeconds,
                            onClick = { input = TimerInputLogic.digitsFor(presetSeconds) },
                            label = { Text(name) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                val keys = listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                    listOf(7, 8, 9),
                    listOf(-1, 0, -2) // -1 = clear, -2 = backspace
                )
                keys.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                -1 -> OutlinedButton(
                                    onClick = { input = TimerInputLogic.clear() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("C") }
                                -2 -> OutlinedButton(
                                    onClick = { input = TimerInputLogic.backspace(input) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("<") }
                                else -> OutlinedButton(
                                    onClick = {
                                        input = TimerInputLogic.append(input, key)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = key.toString(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(label.trim(), selectedSeconds * 1000L)
                },
                enabled = selectedSeconds > 0
            ) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
