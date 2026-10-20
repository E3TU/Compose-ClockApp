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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.clockapp.data.AlarmScheduler
import com.example.clockapp.ui.formatClockTime
import com.example.clockapp.ui.resolveIs24Hour
import com.example.clockapp.ui.theme.ClockAppTheme
import com.example.clockapp.ui.theme.themeSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import java.util.Calendar

class AlarmActivity : ComponentActivity() {

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

        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL)
            ?.takeIf { it.isNotBlank() } ?: "Alarm"
        setContent {
            val settings by themeSettings
                .collectAsStateWithLifecycle(
                    initialValue = com.example.clockapp.ui.theme.ThemeSettings()
                )
            ClockAppTheme(colorSource = settings.colorSource) {
                AlarmRingingScreen(
                    label = label,
                    is24Hour = resolveIs24Hour(this, settings.timeFormat),
                    onDismiss = { finish() }
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
                setDataSource(this@AlarmActivity, uri)
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
    }
}

@Composable
fun AlarmRingingScreen(label: String, is24Hour: Boolean, onDismiss: () -> Unit) {
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
        Button(onClick = onDismiss, modifier = Modifier.height(56.dp)) {
            Text("Dismiss", style = MaterialTheme.typography.titleMedium)
        }
    }
}
