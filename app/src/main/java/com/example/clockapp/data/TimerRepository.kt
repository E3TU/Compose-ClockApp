package com.example.clockapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TimerRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).timerDao()

    val timers: Flow<List<Timer>> = dao.observeAll()

    suspend fun add(label: String, durationMillis: Long, startNow: Boolean) {
        val now = System.currentTimeMillis()
        val timer = Timer(
            label = label,
            durationMillis = durationMillis,
            remainingMillis = durationMillis,
            endsAtMillis = if (startNow) now + durationMillis else 0L,
            isRunning = startNow
        )
        val id = dao.insert(timer)
        val saved = timer.copy(id = id)
        if (startNow) TimerScheduler.schedule(context, saved)
    }

    suspend fun start(timer: Timer) {
        if (timer.isRunning || timer.remainingMillis <= 0) return
        val updated = timer.copy(
            endsAtMillis = System.currentTimeMillis() + timer.remainingMillis,
            isRunning = true
        )
        dao.update(updated)
        TimerScheduler.schedule(context, updated)
    }

    suspend fun pause(timer: Timer) {
        if (!timer.isRunning) return
        val remaining = (timer.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val updated = timer.copy(
            remainingMillis = remaining,
            endsAtMillis = 0,
            isRunning = false
        )
        dao.update(updated)
        TimerScheduler.cancel(context, timer.id)
    }

    suspend fun addMinute(timer: Timer) {
        addTime(timer, 60_000)
    }

    private suspend fun addTime(timer: Timer, millis: Long) {
        val now = System.currentTimeMillis()
        val updated = when {
            timer.isRunning -> timer.copy(
                endsAtMillis = maxOf(now, timer.endsAtMillis) + millis,
                isRunning = true
            )
            // Expired (remaining == 0): restart it for the added time.
            timer.remainingMillis <= 0 -> timer.copy(
                remainingMillis = millis,
                endsAtMillis = now + millis,
                isRunning = true
            )
            else -> timer.copy(
                durationMillis = timer.durationMillis + millis,
                remainingMillis = timer.remainingMillis + millis
            )
        }
        dao.update(updated)
        TimerScheduler.schedule(context, updated)
    }

    suspend fun delete(timer: Timer) {
        TimerScheduler.cancel(context, timer.id)
        dao.delete(timer)
    }

    /** Called when AlarmManager fires: keep the row but mark it expired. */
    suspend fun onFired(id: Long) {
        val timer = dao.getById(id) ?: return
        dao.update(timer.copy(remainingMillis = 0, endsAtMillis = 0, isRunning = false))
    }

    suspend fun deleteById(id: Long) {
        val timer = dao.getById(id) ?: return
        TimerScheduler.cancel(context, id)
        dao.delete(timer)
    }

    suspend fun onAddMinute(id: Long) {
        val timer = dao.getById(id) ?: return
        addTime(timer, 60_000)
    }

    /** Drop timers already expired on boot: they could not ring. */
    suspend fun expireStale() {
        val now = System.currentTimeMillis()
        dao.getAll().forEach { timer ->
            if (timer.isRunning && timer.endsAtMillis <= now) {
                dao.update(
                    timer.copy(remainingMillis = 0, endsAtMillis = 0, isRunning = false)
                )
            }
        }
    }

    suspend fun rescheduleAll() {
        expireStale()
        TimerScheduler.rescheduleAll(context, dao.getAll())
    }
}
