package com.example

import android.app.Application
import com.example.data.DailyDatabase
import com.example.data.DailyRepository
import com.example.reminder.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyApplication : Application() {

    val database: DailyDatabase by lazy { DailyDatabase.getInstance(this) }

    val repository: DailyRepository by lazy { DailyRepository(database.dailyTaskDao(), this) }

    override fun onCreate() {
        super.onCreate()
        DailyRepository.setInstance(repository)

        ReminderManager.createNotificationChannel(this)

        // Seed defaults if app opened for the first time
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedDefaultsIfEmpty()
            ReminderManager.rescheduleAllReminders(this@DailyApplication)
        }
    }
}
