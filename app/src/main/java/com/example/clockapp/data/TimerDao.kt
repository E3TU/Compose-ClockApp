package com.example.clockapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY isRunning DESC, endsAtMillis ASC")
    fun observeAll(): Flow<List<Timer>>

    @Query("SELECT * FROM timers")
    suspend fun getAll(): List<Timer>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getById(id: Long): Timer?

    @Insert
    suspend fun insert(timer: Timer): Long

    @Update
    suspend fun update(timer: Timer)

    @Delete
    suspend fun delete(timer: Timer)
}
