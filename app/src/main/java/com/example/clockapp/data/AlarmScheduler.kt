package com.example.clockapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_ALARM_LABEL = "alarm_label"

    fun schedule(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerMillis(alarm)

        if (triggerAt == null) {
            cancel(context, alarm.id)
            return
        }

        val operation = pendingIntent(context, alarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!manager.canScheduleExactAlarms()) return
        }

        if (alarm.isRepeating) {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, operation),
                operation
            )
        } else {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                operation
            )
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, alarmId, ""))
    }

    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        alarms.forEach { alarm ->
            if (alarm.isEnabled) schedule(context, alarm) else cancel(context, alarm.id)
        }
    }

    private fun pendingIntent(context: Context, alarm: Alarm): PendingIntent =
        pendingIntent(context, alarm.id, alarm.label)

    private fun pendingIntent(context: Context, alarmId: Long, label: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, alarmId.toInt(), intent, flags)
    }

    private fun nextTriggerMillis(alarm: Alarm): Long? {
        if (!alarm.isEnabled) return null

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.isRepeating) {
            for (offset in 0..7) {
                val candidate = trigger.clone() as Calendar
                candidate.add(Calendar.DAY_OF_YEAR, offset)
                // Calendar.DAY_OF_WEEK: Sunday = 1 .. Saturday = 7 -> map to Monday = 0 .. Sunday = 6
                val dayIndex = (candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7
                if (alarm.repeatsOn(dayIndex) && candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
            return null
        }

        if (trigger.before(now)) trigger.add(Calendar.DAY_OF_YEAR, 1)
        return trigger.timeInMillis
    }
}
