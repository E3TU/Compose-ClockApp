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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.clockapp.ui.screens.SETTINGS_ROUTE
import com.example.clockapp.ui.screens.SettingsScreen
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
    @RequiresApi(Build.VERSION_CODES.O)
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
                                    activity.getSystemService(UI_MODE_SERVICE) as UiModeManager
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
    val isSettingsRoute = currentRoute == SETTINGS_ROUTE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Crossfade(targetState = isSettingsRoute, label = "topBar") { showSettingsBar ->
                if (showSettingsBar) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.nav_settings)) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(currentDestination.labelRes)) },
                        actions = {
                            IconButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = "Theme settings"
                                )
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSettingsRoute,
                enter = slideInVertically(tween(250, easing = LinearOutSlowInEasing)) { it } +
                    fadeIn(tween(200)),
                exit = slideOutVertically(tween(250, easing = FastOutLinearInEasing)) { it } +
                    fadeOut(tween(200)),
                label = "bottomBar"
            ) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (targetState.destination.route == SETTINGS_ROUTE) {
                    slideInHorizontally(tween(350, easing = LinearOutSlowInEasing)) { it / 4 } +
                        fadeIn(tween(250, delayMillis = 90))
                } else {
                    fadeIn(tween(200))
                }
            },
            exitTransition = {
                if (targetState.destination.route == SETTINGS_ROUTE) {
                    slideOutHorizontally(tween(350, easing = FastOutLinearInEasing)) { -it / 5 } +
                        fadeOut(tween(200))
                } else {
                    fadeOut(tween(200))
                }
            },
            popEnterTransition = {
                slideInHorizontally(tween(350, easing = LinearOutSlowInEasing)) { -it / 5 } +
                    fadeIn(tween(250, delayMillis = 90))
            },
            popExitTransition = {
                if (targetState.destination.route == SETTINGS_ROUTE) {
                    slideOutHorizontally(tween(350, easing = FastOutLinearInEasing)) { it / 4 } +
                        fadeOut(tween(200))
                } else {
                    fadeOut(tween(200))
                }
            }
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
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    settings = settings,
                    systemIs24Hour = systemIs24Hour,
                    onColorSourceChange = onColorSourceChange,
                    onAppearanceChange = onAppearanceChange,
                    onTimeFormatChange = onTimeFormatChange
                )
            }
        }
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
