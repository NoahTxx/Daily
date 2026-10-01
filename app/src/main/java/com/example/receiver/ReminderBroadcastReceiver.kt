package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.DailyDatabase
import com.example.reminder.ReminderManager
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DailyDatabase.getInstance(context)
                val task = db.dailyTaskDao().getTaskById(taskId)
                if (task != null && !task.isArchived && task.reminderEnabled) {
                    val todayKey = DateUtils.getTodayKey()
                    var shouldNotify = true

                    if (task.frequencyType == "WEEKDAYS") {
                        val isoDay = DateUtils.getIsoDayOfWeek(todayKey)
                        val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (!allowed.contains(isoDay)) {
                            shouldNotify = false
                        }
                    } else if (task.frequencyType == "WEEKLY") {
                        val weekDays = DateUtils.getDaysInSameWeek(todayKey)
                        val weekCompletions = db.dailyTaskDao().getCompletionsForDateKeysSync(weekDays)
                        val completedCount = weekCompletions.count { it.taskId == task.id && (if (task.isCounter) it.count >= task.targetCount else it.count > 0) }
                        if (completedCount >= task.targetDaysPerWeek) {
                            shouldNotify = false
                        }
                    }

                    val completion = db.dailyTaskDao().getCompletion(taskId, todayKey)
                    val isDoneToday = if (task.isCounter) (completion?.count ?: 0) >= task.targetCount else completion != null
                    if (isDoneToday) {
                        shouldNotify = false
                    }

                    if (shouldNotify) {
                        ReminderManager.showReminderNotification(context, task)
                    }
                    // Schedule next occurrence for tomorrow
                    ReminderManager.scheduleReminder(context, task)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
