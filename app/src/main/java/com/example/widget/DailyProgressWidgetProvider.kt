package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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

class DailyProgressWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyProgressWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DailyProgressWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateProgressWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateProgressWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateProgressWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appSettings = AppSettings.getInstance(context)
        val accentColor = appSettings.selectedAccent.value.color.toArgb()
        val language = appSettings.selectedLanguage.value
        val isEn = language == AppLanguage.EN

        // Initial placeholder render
        val initialViews = createResponsiveViews(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            done = 0,
            total = 0,
            percent = 0,
            accentColor = accentColor,
            isEn = isEn
        )
        appWidgetManager.updateAppWidget(appWidgetId, initialViews)

        // Load data in background and update
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
                val percent = if (total > 0) ((done.toFloat() / total.toFloat()) * 100f).toInt() else 0

                val updatedViews = createResponsiveViews(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    done = done,
                    total = total,
                    percent = percent,
                    accentColor = accentColor,
                    isEn = isEn
                )
                appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createResponsiveViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        done: Int,
        total: Int,
        percent: Int,
        accentColor: Int,
        isEn: Boolean
    ): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val rvCompact = buildCompactLayout(context, appWidgetId, done, total, percent, accentColor, isEn)
            val rvWide = buildWideLayout(context, appWidgetId, done, total, percent, accentColor, isEn)
            val rvLarge = buildLargeLayout(context, appWidgetId, done, total, percent, accentColor, isEn)

            val viewMapping = mapOf(
                SizeF(40f, 40f) to rvCompact,    // 1x1: compact percent + fraction
                SizeF(40f, 100f) to rvCompact,   // 1x2 (tall thin): compact
                SizeF(105f, 40f) to rvWide,      // 2x1: horizontal bar + text
                SizeF(105f, 100f) to rvLarge     // 2x2: large gauge ring + detailed status
            )
            return RemoteViews(viewMapping)
        }

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0

        return when {
            minWidth in 1..95 -> buildCompactLayout(context, appWidgetId, done, total, percent, accentColor, isEn)
            minHeight >= 95 || maxHeight >= 120 -> buildLargeLayout(context, appWidgetId, done, total, percent, accentColor, isEn)
            else -> buildWideLayout(context, appWidgetId, done, total, percent, accentColor, isEn)
        }
    }

    private fun buildCompactLayout(
        context: Context,
        appWidgetId: Int,
        done: Int,
        total: Int,
        percent: Int,
        accentColor: Int,
        isEn: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_progress_compact_layout)
        val dotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(16, accentColor)
        views.setImageViewBitmap(R.id.widget_progress_compact_dot, dotBitmap)

        views.setTextViewText(R.id.widget_progress_compact_percent, "$percent%")
        views.setTextColor(R.id.widget_progress_compact_percent, accentColor)

        val fractionText = if (total == 0) {
            "-"
        } else if (done >= total) {
            if (isEn) "ALL DONE" else "ALLES"
        } else {
            "$done/$total"
        }
        views.setTextViewText(R.id.widget_progress_compact_fraction, fractionText)

        attachAppClick(context, views, R.id.widget_progress_root, appWidgetId + 3100)
        return views
    }

    private fun buildWideLayout(
        context: Context,
        appWidgetId: Int,
        done: Int,
        total: Int,
        percent: Int,
        accentColor: Int,
        isEn: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_progress_wide_layout)
        val dotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(20, accentColor)
        views.setImageViewBitmap(R.id.widget_progress_wide_dot, dotBitmap)

        views.setTextViewText(R.id.widget_progress_wide_percent, "$percent%")
        views.setTextColor(R.id.widget_progress_wide_percent, accentColor)

        val completedText = if (total == 0) {
            AppStrings.get("widget_progress_no_habits", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else {
            String.format(
                AppStrings.get("widget_progress_completed", if (isEn) AppLanguage.EN else AppLanguage.DE),
                done,
                total
            )
        }
        views.setTextViewText(R.id.widget_progress_wide_text, completedText)

        val remaining = (total - done).coerceAtLeast(0)
        val statusText = if (total == 0) {
            if (isEn) "NO HABITS TODAY" else "KEINE AUFGABEN HEUTE"
        } else if (remaining == 0) {
            AppStrings.get("widget_progress_all_done", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else if (remaining == 1) {
            AppStrings.get("widget_progress_left_singular", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else {
            String.format(
                AppStrings.get("widget_progress_left_plural", if (isEn) AppLanguage.EN else AppLanguage.DE),
                remaining
            )
        }
        views.setTextViewText(R.id.widget_progress_wide_sub, statusText)

        // Custom drawn rounded progress bar bitmap
        val progressFraction = if (total > 0) (done.toFloat() / total.toFloat()) else 0f
        val barBitmap = WidgetBitmapUtils.createProgressBarBitmap(
            widthPx = 280,
            heightPx = 16,
            progress = progressFraction,
            accentColor = accentColor
        )
        views.setImageViewBitmap(R.id.widget_progress_wide_bar, barBitmap)

        attachAppClick(context, views, R.id.widget_progress_root, appWidgetId + 3200)
        return views
    }

    private fun buildLargeLayout(
        context: Context,
        appWidgetId: Int,
        done: Int,
        total: Int,
        percent: Int,
        accentColor: Int,
        isEn: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_progress_large_layout)
        val dotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(20, accentColor)
        views.setImageViewBitmap(R.id.widget_progress_large_dot, dotBitmap)

        val titleText = AppStrings.get("widget_progress_title", if (isEn) AppLanguage.EN else AppLanguage.DE)
        views.setTextViewText(R.id.widget_progress_large_title, titleText)

        views.setTextViewText(R.id.widget_progress_large_percent, "$percent%")
        views.setTextColor(R.id.widget_progress_large_percent, accentColor)

        views.setTextViewText(R.id.widget_progress_large_fraction, "$done / $total")

        // Draw circular gauge
        val progressFraction = if (total > 0) (done.toFloat() / total.toFloat()) else 0f
        val gaugeBitmap = WidgetBitmapUtils.createCircularProgressBitmap(
            sizePx = 200,
            progress = progressFraction,
            strokeWidthPx = 16f,
            accentColor = accentColor
        )
        views.setImageViewBitmap(R.id.widget_progress_large_gauge, gaugeBitmap)

        val remaining = (total - done).coerceAtLeast(0)
        val statusText = if (total == 0) {
            AppStrings.get("widget_progress_no_habits", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else if (remaining == 0) {
            AppStrings.get("widget_progress_all_done", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else if (remaining == 1) {
            AppStrings.get("widget_progress_left_singular", if (isEn) AppLanguage.EN else AppLanguage.DE)
        } else {
            String.format(
                AppStrings.get("widget_progress_left_plural", if (isEn) AppLanguage.EN else AppLanguage.DE),
                remaining
            )
        }
        views.setTextViewText(R.id.widget_progress_large_status, statusText)

        attachAppClick(context, views, R.id.widget_progress_root, appWidgetId + 3300)
        return views
    }

    private fun attachAppClick(context: Context, views: RemoteViews, viewId: Int, requestCode: Int) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, openAppPendingIntent)
    }
}
