package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.data.DailyDatabase
import com.example.data.TaskCompletion
import com.example.reminder.ReminderManager
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.widget.DailyWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.ACTION_SNOOZE_TASK"
        const val ACTION_SNOOZE_GENERAL = "com.example.ACTION_SNOOZE_GENERAL"
        const val ACTION_SNOOZE_TEST = "com.example.ACTION_SNOOZE_TEST"
        const val ACTION_FIRE_TASK_SNOOZE = "com.example.ACTION_FIRE_TASK_SNOOZE"
        const val ACTION_FIRE_GENERAL_SNOOZE = "com.example.ACTION_FIRE_GENERAL_SNOOZE"
        const val ACTION_FIRE_TEST_SNOOZE = "com.example.ACTION_FIRE_TEST_SNOOZE"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }

                if (taskId != -1L) {
                    ReminderManager.cancelTaskSnooze(context, taskId)
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = DailyDatabase.getInstance(context)
                            val todayKey = DateUtils.getTodayKey()
                            val task = db.dailyTaskDao().getTaskById(taskId)
                            val count = if (task?.isCounter == true) task.targetCount else 1
                            db.dailyTaskDao().insertCompletion(
                                TaskCompletion(taskId = taskId, dateKey = todayKey, count = count)
                            )
                            DailyWidgetProvider.notifyDataChanged(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_SNOOZE_TASK -> {
                val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }

                if (taskId != -1L) {
                    ReminderManager.scheduleTaskSnooze(context, taskId, minutes)
                    showSnoozeToast(context, minutes)
                }
            }

            ACTION_FIRE_TASK_SNOOZE -> {
                val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
                if (taskId == -1L) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = DailyDatabase.getInstance(context)
                        val task = db.dailyTaskDao().getTaskById(taskId)
                        if (task != null && !task.isArchived && task.reminderEnabled) {
                            val todayKey = DateUtils.getTodayKey()
                            val completion = db.dailyTaskDao().getCompletion(taskId, todayKey)
                            val isDoneToday = if (task.isCounter) {
                                (completion?.count ?: 0) >= task.targetCount
                            } else {
                                completion != null
                            }
                            if (!isDoneToday) {
                                ReminderManager.showReminderNotification(context, task, isSnoozed = true)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_GENERAL -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 99901)
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                ReminderManager.scheduleGeneralSnooze(context, minutes)
                showSnoozeToast(context, minutes)
            }

            ACTION_FIRE_GENERAL_SNOOZE -> {
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
                            ReminderManager.showGeneralReminderNotification(
                                context = context,
                                incompleteCount = incompleteCount,
                                totalCount = dueTasks.size,
                                isSnoozed = true
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_TEST -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 99903)
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                ReminderManager.scheduleTestSnooze(context, minutes)
                showSnoozeToast(context, minutes)
            }

            ACTION_FIRE_TEST_SNOOZE -> {
                ReminderManager.triggerTestNotification(context)
            }
        }
    }

    private fun showSnoozeToast(context: Context, minutes: Int) {
        val appSettings = AppSettings.getInstance(context)
        val language = appSettings.selectedLanguage.value
        val msg = String.format(AppStrings.get("notif_snooze_toast", language), minutes)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
