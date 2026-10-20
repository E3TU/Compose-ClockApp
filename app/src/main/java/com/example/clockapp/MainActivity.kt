package com.example.clockapp

import android.app.UiModeManager
import android.text.format.DateFormat
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.clockapp.navigation.TopLevelDestination
import com.example.clockapp.ui.resolveIs24Hour
import com.example.clockapp.ui.screens.AlarmScreen
import com.example.clockapp.ui.screens.ClockScreen
import com.example.clockapp.ui.screens.StopwatchScreen
import com.example.clockapp.ui.screens.TimerScreen
import com.example.clockapp.ui.theme.AppearanceMode
import com.example.clockapp.ui.theme.ClockAppTheme
import com.example.clockapp.ui.theme.ColorSource
import com.example.clockapp.ui.theme.ThemeSettings
import com.example.clockapp.ui.theme.TimeFormatMode
import com.example.clockapp.ui.theme.setAppearanceMode
import com.example.clockapp.ui.theme.setColorSource
import com.example.clockapp.ui.theme.setTimeFormat
import com.example.clockapp.ui.theme.themeSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val activity = this
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val settings by context.themeSettings
                .collectAsStateWithLifecycle(initialValue = ThemeSettings())

            ClockAppTheme(colorSource = settings.colorSource) {
                ClockAppRoot(
                    settings = settings,
                    onColorSourceChange = { source ->
                        scope.launch { context.setColorSource(source) }
                    },
                    onAppearanceChange = { mode ->
                        scope.launch {
                            context.setAppearanceMode(mode)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val nightMode = when (mode) {
                                    AppearanceMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                                    AppearanceMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                                    AppearanceMode.DARK -> UiModeManager.MODE_NIGHT_YES
                                }
                                val uiModeManager =
                                    activity.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                                uiModeManager.setApplicationNightMode(nightMode)
                                activity.recreate()
                            }
                        }
                    },
                    onTimeFormatChange = { format ->
                        scope.launch { context.setTimeFormat(format) }
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockAppRoot(
    settings: ThemeSettings,
    onColorSourceChange: (ColorSource) -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onTimeFormatChange: (TimeFormatMode) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = resolveIs24Hour(context, settings.timeFormat)
    val systemIs24Hour = DateFormat.is24HourFormat(context)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = TopLevelDestination.fromRoute(currentRoute)
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentDestination.labelRes)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Theme settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.CLOCK.route) {
                ClockScreen(is24Hour = is24Hour)
            }
            composable(TopLevelDestination.ALARM.route) {
                AlarmScreen(is24Hour = is24Hour)
            }
            composable(TopLevelDestination.TIMER.route) {
                TimerScreen()
            }
            composable(TopLevelDestination.STOPWATCH.route) {
                StopwatchScreen()
            }
        }

        if (showSettings) {
            ThemeSettingsSheet(
                settings = settings,
                systemIs24Hour = systemIs24Hour,
                onColorSourceChange = onColorSourceChange,
                onAppearanceChange = onAppearanceChange,
                onTimeFormatChange = onTimeFormatChange,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsSheet(
    settings: ThemeSettings,
    systemIs24Hour: Boolean,
    onColorSourceChange: (ColorSource) -> Unit,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onTimeFormatChange: (TimeFormatMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun ClockAppPreviewLight() {
    ClockAppTheme(colorSource = ColorSource.BRAND) {
        ClockScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ClockAppPreviewDark() {
    ClockAppTheme(colorSource = ColorSource.BRAND) {
        ClockScreen()
    }
}
