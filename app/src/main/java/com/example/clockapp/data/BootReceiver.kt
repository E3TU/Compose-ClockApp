package com.example.clockapp.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarms = AppDatabase.get(appContext).alarmDao().getAll()
                AlarmScheduler.rescheduleAll(appContext, alarms)
            } finally {
                pending.finish()
            }
        }
    }
}
