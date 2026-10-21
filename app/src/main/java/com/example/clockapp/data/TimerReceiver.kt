package com.example.clockapp.data

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clockapp.AlertActivity
import com.example.clockapp.MainActivity
import com.example.clockapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1L)
        if (timerId < 0) return

        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_DISMISS -> {
                NotificationManagerCompat.from(appContext).cancel(notificationId(timerId))
                appContext.sendBroadcast(Intent(AlertActivity.ACTION_DISMISS))
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        TimerRepository(appContext).onFired(timerId)
                    } finally {
                        pending.finish()
                    }
                }
                return
            }

            ACTION_ADD_MINUTE -> {
                NotificationManagerCompat.from(appContext).cancel(notificationId(timerId))
                appContext.sendBroadcast(Intent(AlertActivity.ACTION_DISMISS))
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        TimerRepository(appContext).onAddMinute(timerId)
                    } finally {
                        pending.finish()
                    }
                }
                return
            }
        }

        val label = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_LABEL).orEmpty()

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dao = AppDatabase.get(appContext).timerDao()
                val timer = dao.getById(timerId) ?: return@launch
                showNotification(appContext, timerId, label.ifBlank { timer.label })
                dao.update(timer.copy(remainingMillis = 0, endsAtMillis = 0, isRunning = false))
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("WrongConstant", "FullScreenIntentPolicy")
    private fun showNotification(context: Context, timerId: Long, label: String) {
        AlertNotifications.ensureChannel(context, AlertNotifications.CHANNEL_TIMERS, "Timers")

        val fullLabel = label.ifBlank { "Timer" }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        val openIntent = PendingIntent.getActivity(
            context,
            TimerRequestCodes.OPEN.toInt() + timerId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            flags
        )

        val dismissIntent = TimerScheduler.actionIntent(
            context, timerId, fullLabel, ACTION_DISMISS
        )
        val addMinuteIntent = TimerScheduler.actionIntent(
            context, timerId, fullLabel, ACTION_ADD_MINUTE
        )
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            TimerRequestCodes.FULL_SCREEN.toInt() + timerId.toInt(),
            AlertActivity.timerIntent(context, timerId, fullLabel),
            flags
        )

        val notification = NotificationCompat.Builder(
            context,
            AlertNotifications.CHANNEL_TIMERS
        )
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(fullLabel)
            .setContentText(context.getString(R.string.timer_fire_time))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.alarm_dismiss), dismissIntent)
            .addAction(0, context.getString(R.string.timer_add_minute), addMinuteIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        NotificationManagerCompat.from(context).notify(notificationId(timerId), notification)
    }

    companion object {
        const val ACTION_FIRE = "com.example.clockapp.action.FIRE_TIMER"
        const val ACTION_DISMISS = "com.example.clockapp.action.DISMISS_TIMER"
        const val ACTION_ADD_MINUTE = "com.example.clockapp.action.ADD_MINUTE_TIMER"

        // Separate id space from alarm notifications (alarmId).
        private const val NOTIFICATION_ID_BASE = 800_000L

        fun notificationId(timerId: Long): Int = (NOTIFICATION_ID_BASE + timerId).toInt()
    }
}

private object TimerRequestCodes {
    const val OPEN = 900_000L
    const val FULL_SCREEN = 950_000L
}
