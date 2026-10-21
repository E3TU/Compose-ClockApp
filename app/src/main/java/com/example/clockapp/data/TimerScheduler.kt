package com.example.clockapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerScheduler {
    const val EXTRA_TIMER_ID = "timer_id"
    const val EXTRA_TIMER_LABEL = "timer_label"

    // Distinct request-code space from AlarmScheduler (which uses id, id+100_000, id+200_000).
    private const val REQUEST_BASE = 400_000

    fun triggerAt(nowMillis: Long, endsAtMillis: Long): Long? =
        endsAtMillis.takeIf { it > 0 && it > nowMillis }

    fun schedule(context: Context, timer: Timer) {
        if (!timer.isRunning) {
            cancel(context, timer.id)
            return
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = triggerAt(System.currentTimeMillis(), timer.endsAtMillis)

        if (triggerAt == null) {
            cancel(context, timer.id)
            return
        }

        val operation = fireIntent(context, timer)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                60_000L,
                operation
            )
            return
        }

        manager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            operation
        )
    }

    fun cancel(context: Context, timerId: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(
            intentFor(
                context,
                timerId,
                "",
                TimerReceiver.ACTION_FIRE,
                requestCodeFor(timerId, TimerReceiver.ACTION_FIRE)
            )
        )
    }

    fun rescheduleAll(context: Context, timers: List<Timer>) {
        timers.forEach { timer ->
            if (timer.isRunning) schedule(context, timer) else cancel(context, timer.id)
        }
    }

    fun fireIntent(context: Context, timer: Timer): PendingIntent =
        intentFor(
            context,
            timer.id,
            timer.label,
            TimerReceiver.ACTION_FIRE,
            requestCodeFor(timer.id, TimerReceiver.ACTION_FIRE)
        )

    fun actionIntent(context: Context, timerId: Long, label: String, action: String): PendingIntent =
        intentFor(context, timerId, label, action, requestCodeFor(timerId, action))

    private fun requestCodeFor(timerId: Long, action: String?): Int {
        val salt = when (action) {
            TimerReceiver.ACTION_DISMISS -> 100_000
            TimerReceiver.ACTION_ADD_MINUTE -> 200_000
            else -> 0
        }
        return (REQUEST_BASE + timerId + salt).toInt()
    }

    private fun intentFor(
        context: Context,
        timerId: Long,
        label: String,
        action: String?,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            if (action != null) this.action = action
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_TIMER_LABEL, label)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
