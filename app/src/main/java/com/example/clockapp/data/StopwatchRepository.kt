package com.example.clockapp.data

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Move to data/settings if more preferences are added
data class StopwatchState(
    val accumulatedMillis: Long = 0,
    // SystemClock.elapsedRealtime() baseline while running. 0 = stopped.
    val startedAtElapsed: Long = 0,
    // Recorded lap totals in millis, oldest first.
    val laps: List<Long> = emptyList()
) {
    val isRunning: Boolean get() = startedAtElapsed > 0

    fun elapsedAt(nowElapsed: Long): Long =
        accumulatedMillis + if (isRunning) (nowElapsed - startedAtElapsed).coerceAtLeast(0) else 0
}

private val Context.stopwatchDataStore by preferencesDataStore(name = "stopwatch")

private val AccumulatedKey = longPreferencesKey("accumulated_millis")
private val StartedAtKey = longPreferencesKey("started_at_elapsed")
private val LapsKey = stringPreferencesKey("laps")

class StopwatchRepository(context: Context) {
    private val dataStore = context.applicationContext.stopwatchDataStore

    val state: Flow<StopwatchState> = dataStore.data.map { preferences ->
        val accumulated = preferences[AccumulatedKey] ?: 0L
        val storedStart = preferences[StartedAtKey] ?: 0L
        // After a reboot elapsedRealtime restarts near 0; treat a stale start as paused.
        val startedAt = if (storedStart > SystemClock.elapsedRealtime()) 0L else storedStart
        val laps = preferences[LapsKey].orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
        StopwatchState(accumulated, startedAt, laps)
    }

    suspend fun startOrResume() {
        dataStore.edit { preferences ->
            val now = SystemClock.elapsedRealtime()
            val started = preferences[StartedAtKey] ?: 0L
            if (started == 0L || started > now) {
                preferences[StartedAtKey] = now
            }
        }
    }

    suspend fun pause() {
        bankElapsed(reanchor = false)
    }

    /** Re-anchor persistence while keeping the displayed elapsed identical. */
    suspend fun heartbeat() {
        bankElapsed(reanchor = true)
    }

    private suspend fun bankElapsed(reanchor: Boolean) {
        dataStore.edit { preferences ->
            val accumulated = preferences[AccumulatedKey] ?: 0L
            val started = preferences[StartedAtKey] ?: 0L
            val now = SystemClock.elapsedRealtime()
            if (started in 1..now) {
                preferences[AccumulatedKey] = accumulated + (now - started)
                preferences[StartedAtKey] = if (reanchor) now else 0L
            }
        }
    }

    suspend fun lap() {
        dataStore.edit { preferences ->
            val accumulated = preferences[AccumulatedKey] ?: 0L
            val started = preferences[StartedAtKey] ?: 0L
            val now = SystemClock.elapsedRealtime()
            val elapsed = accumulated + if (started in 1..now) now - started else 0L
            val laps = preferences[LapsKey].orEmpty()
                .split(',')
                .mapNotNull { it.toLongOrNull() } + elapsed
            preferences[LapsKey] = laps.joinToString(",")
        }
    }

    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences[AccumulatedKey] = 0L
            preferences[StartedAtKey] = 0L
            preferences[LapsKey] = ""
        }
    }
}
