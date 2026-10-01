package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DailyDatabase
import com.example.reminder.ReminderManager
import com.example.util.AppSettings
import com.example.util.AppStrings
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
                val db = DailyDatabase.getInstance(context)
                val todayKey = DateUtils.getTodayKey()
                val tasks = db.dailyTaskDao().getAllActiveTasksSync()
                val completions = db.dailyTaskDao().getCompletionsForDateSync(todayKey)
                val weekDays = DateUtils.getDaysInSameWeek(todayKey)
                val weekCompletions = db.dailyTaskDao().getCompletionsForDateKeysSync(weekDays)
                val isoDay = DateUtils.getIsoDayOfWeek(todayKey)

                val completedIdsToday = completions.filter { comp ->
                    val task = tasks.find { it.id == comp.taskId }
                    if (task?.isCounter == true) {
                        comp.count >= task.targetCount
                    } else {
                        true
                    }
                }.map { it.taskId }.toSet()

                val dueTasks = tasks.filter { task ->
                    when (task.frequencyType) {
                        "WEEKDAYS" -> {
                            val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                            allowed.contains(isoDay)
                        }
                        "WEEKLY" -> {
                            val completedCount = weekCompletions.count {
                                it.taskId == task.id && (if (task.isCounter) it.count >= task.targetCount else it.count > 0)
                            }
                            completedCount < task.targetDaysPerWeek
                        }
                        else -> true
                    }
                }

                val incompleteCount = dueTasks.count { !completedIdsToday.contains(it.id) }

                if (incompleteCount > 0) {
                    ReminderManager.showGeneralReminderNotification(context, incompleteCount, dueTasks.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
