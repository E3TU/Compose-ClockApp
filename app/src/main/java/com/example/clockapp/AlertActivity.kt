package com.example.clockapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.clockapp.data.AlertNotifications
import com.example.clockapp.data.AlarmScheduler
import com.example.clockapp.data.TimerReceiver
import com.example.clockapp.data.TimerScheduler
import com.example.clockapp.ui.formatClockTime
import com.example.clockapp.ui.resolveIs24Hour
import com.example.clockapp.ui.theme.ClockAppTheme
import com.example.clockapp.ui.theme.ThemeSettings
import com.example.clockapp.ui.theme.themeSettings
import java.util.Calendar

class AlertActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_DISMISS) finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: TYPE_ALARM
        val label = intent.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }
            ?: if (alertType == TYPE_TIMER) "Timer" else "Alarm"
        val alertId = intent.getLongExtra(EXTRA_ALERT_ID, -1L)

        setContent {
            val settings by themeSettings
                .collectAsStateWithLifecycle(initialValue = ThemeSettings())
            ClockAppTheme(colorSource = settings.colorSource) {
                AlertRingingScreen(
                    label = label,
                    is24Hour = resolveIs24Hour(this, settings.timeFormat),
                    isTimer = alertType == TYPE_TIMER,
                    onDismiss = {
                        if (alertType == TYPE_TIMER && alertId >= 0) {
                            sendBroadcast(
                                Intent(this, TimerReceiver::class.java).apply {
                                    action = TimerReceiver.ACTION_DISMISS
                                    putExtra(TimerScheduler.EXTRA_TIMER_ID, alertId)
                                }
                            )
                        }
                        finish()
                    },
                    onAddMinute = {
                        if (alertId >= 0) {
                            sendBroadcast(
                                Intent(this, TimerReceiver::class.java).apply {
                                    action = TimerReceiver.ACTION_ADD_MINUTE
                                    putExtra(TimerScheduler.EXTRA_TIMER_ID, alertId)
                                }
                            )
                        }
                        finish()
                    }
                )
            }
        }

        startRinging()
        registerReceiver(
            dismissReceiver,
            IntentFilter(ACTION_DISMISS),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Context.RECEIVER_NOT_EXPORTED else 0
        )
    }

    private fun startRinging() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlertActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        }

        val pattern = longArrayOf(0, 600, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    override fun onDestroy() {
        runCatching { vibrator?.cancel() }
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        vibrator = null
        runCatching { unregisterReceiver(dismissReceiver) }
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISMISS = "com.example.clockapp.action.DISMISS_RINGING"
        const val EXTRA_ALERT_TYPE = "alert_type"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_LABEL = "alert_label"
        const val TYPE_ALARM = "alarm"
        const val TYPE_TIMER = "timer"

        fun alarmIntent(context: Context, alarmId: Long, label: String): Intent =
            Intent(context, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_ALERT_TYPE, TYPE_ALARM)
                putExtra(EXTRA_ALERT_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
                putExtra(EXTRA_LABEL, label)
            }

        fun timerIntent(context: Context, timerId: Long, label: String): Intent =
            Intent(context, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_ALERT_TYPE, TYPE_TIMER)
                putExtra(EXTRA_ALERT_ID, timerId)
                putExtra(TimerScheduler.EXTRA_TIMER_LABEL, label)
                putExtra(EXTRA_LABEL, label)
            }
    }
}

@Composable
fun AlertRingingScreen(
    label: String,
    is24Hour: Boolean,
    isTimer: Boolean,
    onDismiss: () -> Unit,
    onAddMinute: () -> Unit = {}
) {
    val now = Calendar.getInstance()
    val time = formatClockTime(
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        is24Hour
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = time, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        if (isTimer) {
            Button(onClick = onDismiss, modifier = Modifier.height(56.dp)) {
                Text("Dismiss", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = onAddMinute,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Add 1 minute", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(onClick = onDismiss, modifier = Modifier.height(56.dp)) {
                Text("Dismiss", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
