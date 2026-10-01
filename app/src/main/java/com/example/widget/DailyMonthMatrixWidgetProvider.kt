package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.data.DailyDatabase
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyMonthMatrixWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyMonthMatrixWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val provider = DailyMonthMatrixWidgetProvider()
                for (id in appWidgetIds) {
                    provider.updateMonthWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateMonthWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    fun updateMonthWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_month_matrix_layout)
        val appSettings = AppSettings.getInstance(context)
        val accentColor = appSettings.selectedAccent.value.color.toArgb()
        val language = appSettings.selectedLanguage.value
        val isEn = language == AppLanguage.EN

        views.setTextColor(R.id.widget_month_streak_num, accentColor)
        val headerDotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(24, accentColor)
        views.setImageViewBitmap(R.id.widget_month_header_dot, headerDotBitmap)
        views.setInt(R.id.widget_month_header_dot, "setColorFilter", accentColor)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 3000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_month_root, openAppPendingIntent)

        // Immediately render initial views with correct accent color
        appWidgetManager.updateAppWidget(appWidgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DailyDatabase.getInstance(context)
                val repository = com.example.data.DailyRepository.getInstance(context)
                val todayKey = DateUtils.getTodayKey()
                val tasks = db.dailyTaskDao().getAllActiveTasksSync()
                val completions = db.dailyTaskDao().getCompletionsForDateSync(todayKey)
                val weekDays = DateUtils.getDaysInSameWeek(todayKey)
                val weekCompletions = db.dailyTaskDao().getCompletionsForDateKeysSync(weekDays)
                val fullyCompletedDates = repository.getFullyCompletedDatesSync().toSet()

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

                val streak = calculateStreak(fullyCompletedDates)

                val cal = Calendar.getInstance()
                val locale = if (isEn) Locale.US else Locale.GERMAN
                val monthFormat = SimpleDateFormat("MMMM", locale)
                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val currentMonthName = monthFormat.format(cal.time).uppercase()
                val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                views.setTextViewText(R.id.widget_month_name, currentMonthName)
                views.setTextViewText(R.id.widget_month_streak_num, streak.toString())

                if (total == 0) {
                    views.setTextViewText(R.id.widget_month_today_status, AppStrings.get("widget_zero_habits", language))
                } else if (done == total) {
                    views.setTextViewText(R.id.widget_month_today_status, AppStrings.get("widget_today_all_done", language))
                } else {
                    views.setTextViewText(
                        R.id.widget_month_today_status,
                        String.format(AppStrings.get("widget_today_status", language), done, total)
                    )
                }

                val dotIds = listOf(
                    R.id.mdot_1, R.id.mdot_2, R.id.mdot_3, R.id.mdot_4, R.id.mdot_5,
                    R.id.mdot_6, R.id.mdot_7, R.id.mdot_8, R.id.mdot_9, R.id.mdot_10,
                    R.id.mdot_11, R.id.mdot_12, R.id.mdot_13, R.id.mdot_14, R.id.mdot_15,
                    R.id.mdot_16, R.id.mdot_17, R.id.mdot_18, R.id.mdot_19, R.id.mdot_20,
                    R.id.mdot_21, R.id.mdot_22, R.id.mdot_23, R.id.mdot_24, R.id.mdot_25,
                    R.id.mdot_26, R.id.mdot_27, R.id.mdot_28, R.id.mdot_29, R.id.mdot_30,
                    R.id.mdot_31
                )

                val dayCal = Calendar.getInstance()
                val achievedDotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(20, accentColor)
                val ringDotBitmap = WidgetBitmapUtils.createRingBitmap(20, accentColor, 3f)

                for (day in 1..31) {
                    val dotViewId = dotIds[day - 1]
                    if (day > maxDaysInMonth) {
                        views.setViewVisibility(dotViewId, View.GONE)
                        continue
                    } else {
                        views.setViewVisibility(dotViewId, View.VISIBLE)
                    }

                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    val dateKey = keyFormat.format(dayCal.time)
                    val isAchieved = fullyCompletedDates.contains(dateKey)

                    when {
                        day < currentDayOfMonth -> {
                            if (isAchieved) {
                                views.setImageViewBitmap(dotViewId, achievedDotBitmap)
                            } else {
                                views.setImageViewResource(dotViewId, R.drawable.widget_dot_dark)
                            }
                        }
                        day == currentDayOfMonth -> {
                            if (isAchieved) {
                                views.setImageViewBitmap(dotViewId, achievedDotBitmap)
                            } else {
                                views.setImageViewBitmap(dotViewId, ringDotBitmap)
                            }
                        }
                        else -> {
                            views.setImageViewResource(dotViewId, R.drawable.widget_dot_faint)
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateStreak(completionDates: Set<String>): Int {
        if (completionDates.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        var checkKey = format.format(cal.time)
        if (!completionDates.contains(checkKey)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
            if (!completionDates.contains(checkKey)) {
                return 0
            }
        }

        var streak = 0
        while (completionDates.contains(checkKey)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
        }
        return streak
    }
}
