package com.example.clockapp.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.clockapp.ui.theme.AppearanceMode
import com.example.clockapp.ui.theme.ColorSource
import com.example.clockapp.ui.theme.ThemeSettings
import com.example.clockapp.ui.theme.TimeFormatMode

const val SETTINGS_ROUTE = "settings"

@Composable
fun SettingsScreen(
    settings: ThemeSettings,
    systemIs24Hour: Boolean,
    onColorSourceChange: (ColorSource) -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onTimeFormatChange: (TimeFormatMode) -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Color palette", style = MaterialTheme.typography.titleMedium)
        RadioRow(
            label = "Brand sunrise",
            selected = settings.colorSource == ColorSource.BRAND,
            onSelect = { onColorSourceChange(ColorSource.BRAND) }
        )
        RadioRow(
            label = "Material You",
            enabled = supportsDynamicColor,
            selected = settings.colorSource == ColorSource.DYNAMIC && supportsDynamicColor,
            onSelect = { onColorSourceChange(ColorSource.DYNAMIC) }
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        RadioRow(
            label = "System default",
            selected = settings.appearanceMode == AppearanceMode.SYSTEM,
            onSelect = { onAppearanceChange(AppearanceMode.SYSTEM) }
        )
        if (supportsDynamicColor) {
            RadioRow(
                label = "Light",
                selected = settings.appearanceMode == AppearanceMode.LIGHT,
                onSelect = { onAppearanceChange(AppearanceMode.LIGHT) }
            )
            RadioRow(
                label = "Dark",
                selected = settings.appearanceMode == AppearanceMode.DARK,
                onSelect = { onAppearanceChange(AppearanceMode.DARK) }
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text("Time format", style = MaterialTheme.typography.titleMedium)
        RadioRow(
            label = "System default (${if (systemIs24Hour) "24-hour" else "12-hour"})",
            selected = settings.timeFormat == TimeFormatMode.SYSTEM,
            onSelect = { onTimeFormatChange(TimeFormatMode.SYSTEM) }
        )
        RadioRow(
            label = "12-hour",
            selected = settings.timeFormat == TimeFormatMode.H12,
            onSelect = { onTimeFormatChange(TimeFormatMode.H12) }
        )
        RadioRow(
            label = "24-hour",
            selected = settings.timeFormat == TimeFormatMode.H24,
            onSelect = { onTimeFormatChange(TimeFormatMode.H24) }
        )
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
