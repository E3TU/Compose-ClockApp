package com.example.clockapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).alarmDao()

    val alarms: Flow<List<Alarm>> = dao.observeAll()

    suspend fun add(alarm: Alarm) {
        val id = dao.insert(alarm)
        AlarmScheduler.schedule(context, alarm.copy(id = id))
    }

    suspend fun update(alarm: Alarm) {
        dao.update(alarm)
        AlarmScheduler.schedule(context, alarm)
    }

    suspend fun delete(alarm: Alarm) {
        AlarmScheduler.cancel(context, alarm.id)
        dao.delete(alarm)
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        val updated = alarm.copy(isEnabled = enabled)
        dao.update(updated)
        if (enabled) AlarmScheduler.schedule(context, updated)
        else AlarmScheduler.cancel(context, alarm.id)
    }

    suspend fun rescheduleAll() {
        AlarmScheduler.rescheduleAll(context, dao.getAll())
    }
}
