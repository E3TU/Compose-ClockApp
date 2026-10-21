package com.example.clockapp.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clockapp.AlarmActivity
import com.example.clockapp.MainActivity
import com.example.clockapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return

        if (intent.action == ACTION_DISMISS) {
            NotificationManagerCompat.from(context).cancel(alarmId.toInt())
            // Tell the ringing activity to stop its looping sound/vibration.
            context.sendBroadcast(Intent(AlarmActivity.ACTION_DISMISS))
            return
        }

        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL).orEmpty()
        showNotification(context, alarmId, label)

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dao = AppDatabase.get(appContext).alarmDao()
                val alarm = dao.getById(alarmId) ?: return@launch
                if (alarm.isRepeating) {
                    AlarmScheduler.schedule(appContext, alarm)
                } else {
                    dao.update(alarm.copy(isEnabled = false))
                }
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("WrongConstant", "FullScreenIntentPolicy")
    private fun showNotification(context: Context, alarmId: Long, label: String) {
        ensureChannel(context)

        val fullLabel = label.ifBlank { "Alarm" }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        val openIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            flags
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 100_000,
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            flags
        )

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt() + 200_000,
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, fullLabel)
            },
            flags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(fullLabel)
            .setContentText(context.getString(R.string.alarm_fire_time))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.alarm_dismiss), dismissIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        NotificationManagerCompat.from(context).notify(alarmId.toInt(), notification)
    }

    companion object {
        const val ACTION_DISMISS = "com.example.clockapp.action.DISMISS_ALARM"
        private const val CHANNEL_ID = "alarms"

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                if (soundUri != null) {
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                vibrationPattern = longArrayOf(0, 600, 800)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
