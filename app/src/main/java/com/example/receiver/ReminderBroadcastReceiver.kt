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
                val repository = com.example.data.DailyRepository.getInstance(context)
                val task = repository.getTaskByIdSync(taskId)
                if (task != null && !task.isArchived && task.reminderEnabled) {
                    val todayKey = DateUtils.getTodayKey()
                    val tasksWithCompletion = repository.getTasksWithCompletionsSync(todayKey)
                    val taskStatus = tasksWithCompletion.firstOrNull { it.task.id == taskId }

                    // Only notify if the task is due today and not completed yet
                    val shouldNotify = taskStatus != null && taskStatus.isDueToday && !taskStatus.isCompleted

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
