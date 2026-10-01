package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.data.DailyDatabase
import com.example.data.TaskCompletion
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.ACTION_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val EXTRA_AMOUNT = "extra_amount"
        const val ACTION_TYPE_TOGGLE = "toggle"
        const val ACTION_TYPE_INCREMENT = "increment"

        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // Update main task widget
            val componentName = ComponentName(context, DailyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DailyWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
            }

            // Also update the small streak widget, month matrix widget & progress widget
            DailyStreakWidgetProvider.notifyDataChanged(context)
            DailyMonthMatrixWidgetProvider.notifyDataChanged(context)
            DailyProgressWidgetProvider.notifyDataChanged(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.daily_widget_layout)
        val appSettings = AppSettings.getInstance(context)
        val accentColor = appSettings.selectedAccent.value.color.toArgb()
        val language = appSettings.selectedLanguage.value
        val isEn = language == AppLanguage.EN

        // Apply accent color and localized text
        views.setTextColor(R.id.widget_progress_text, accentColor)
        val headerDotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(18, accentColor)
        views.setImageViewBitmap(R.id.widget_header_dot, headerDotBitmap)
        views.setTextViewText(R.id.widget_empty_text, AppStrings.get("widget_no_habits", language))

        // Set adapter intent for ListView with unique URI
        val serviceIntent = Intent(context, DailyWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("daily://widget/tasks/$appWidgetId/${System.currentTimeMillis()}")
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

        // PendingIntent template for row clicks (toggle task)
        val toggleIntent = Intent(context, DailyWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_TASK
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, togglePendingIntent)

        // PendingIntent for Open App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 500,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_progress_text, openAppPendingIntent)

        // Load count numbers in background and update
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DailyDatabase.getInstance(context)
                val todayKey = DateUtils.getTodayKey()
                val tasks = db.dailyTaskDao().getAllActiveTasksSync()
                val completions = db.dailyTaskDao().getCompletionsForDateSync(todayKey)
                val weekDays = DateUtils.getDaysInSameWeek(todayKey)
                val weekCompletions = db.dailyTaskDao().getCompletionsForDateKeysSync(weekDays)

                val compMap = completions.associateBy { it.taskId }
                val completionsByTaskThisWeek = weekCompletions.groupBy { it.taskId }

                val dueTasks = tasks.filter { task ->
                    val comp = compMap[task.id]
                    val isDone = if (task.isCounter) (comp?.count ?: 0) >= task.targetCount else comp != null
                    val weekList = completionsByTaskThisWeek[task.id] ?: emptyList()
                    val weeklyCompletedCount = weekList.count { compItem ->
                        if (task.isCounter) compItem.count >= task.targetCount else compItem.count > 0
                    }
                    DateUtils.isTaskDueOnDate(
                        frequencyType = task.frequencyType,
                        daysOfWeek = task.daysOfWeek,
                        targetDaysPerWeek = task.targetDaysPerWeek,
                        dateKey = todayKey,
                        weeklyCompletedCount = weeklyCompletedCount,
                        isCompletedOnDate = isDone
                    )
                }

                val total = dueTasks.size
                val done = dueTasks.count { task ->
                    val comp = compMap[task.id]
                    if (task.isCounter) (comp?.count ?: 0) >= task.targetCount
                    else comp != null
                }

                views.setTextViewText(R.id.widget_progress_text, "$done / $total")

                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_TASK) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_TYPE_TOGGLE
            val amount = intent.getIntExtra(EXTRA_AMOUNT, 1)

            if (taskId != -1L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = DailyDatabase.getInstance(context)
                        val todayKey = DateUtils.getTodayKey()
                        val task = db.dailyTaskDao().getTaskById(taskId)
                        if (task != null) {
                            val isoDay = DateUtils.getIsoDayOfWeek(todayKey)
                            val isDue = when (task.frequencyType) {
                                "WEEKDAYS" -> {
                                    val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                                    allowed.contains(isoDay)
                                }
                                else -> true
                            }
                            if (isDue) {
                                val repository = com.example.data.DailyRepository.getInstance(context)
                                if (actionType == ACTION_TYPE_INCREMENT) {
                                    repository.incrementTaskCounter(taskId, todayKey, amount)
                                } else {
                                    repository.toggleTaskCompletion(taskId, todayKey)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
