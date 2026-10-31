package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reminder.ReminderManager
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("daily_settings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("general_reminder_enabled", false)
        if (!isEnabled) {
            return
        }

        val hour = prefs.getInt("general_reminder_hour", 20)
        val minute = prefs.getInt("general_reminder_minute", 0)

        // Reschedule for tomorrow
        ReminderManager.scheduleGeneralDailyReminder(context, hour, minute)
        ReminderManager.createNotificationChannel(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.example.data.DailyRepository.getInstance(context)
                val todayKey = DateUtils.getTodayKey()
                val tasks = repository.getTasksWithCompletionsSync(todayKey)
                val dueOrCompletedTasks = tasks.filter { (_, isCompleted, _, _, _, isDueToday) -> isDueToday || isCompleted }
                val totalDue = dueOrCompletedTasks.size
                val incompleteCount = dueOrCompletedTasks.count { (_, isCompleted) -> !isCompleted }

                if (incompleteCount > 0) {
                    ReminderManager.showGeneralReminderNotification(context, incompleteCount, totalDue)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
