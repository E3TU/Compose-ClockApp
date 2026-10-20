package com.example.clockapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "clock_app.db")
                    .build()
                    .also { Instance = it }
            }
    }
}
