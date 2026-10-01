package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

class DailyStreakWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyStreakWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, DailyStreakWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }

            DailyMonthMatrixWidgetProvider.notifyDataChanged(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateStreakWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateStreakWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateStreakWidget(
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
            streak = 0,
            accentColor = accentColor,
            isEn = isEn
        )
        appWidgetManager.updateAppWidget(appWidgetId, initialViews)

        // Load data in background and update
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.example.data.DailyRepository.getInstance(context)
                val fullyCompletedDates = repository.getFullyCompletedDatesSync()
                val streak = calculateStreak(fullyCompletedDates)

                val updatedViews = createResponsiveViews(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    streak = streak,
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
        streak: Int,
        accentColor: Int,
        isEn: Boolean
    ): RemoteViews {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val rvCompact = buildSingleLayout(context, R.layout.widget_streak_compact_layout, streak, accentColor, isEn, appWidgetId)
            val rvWide = buildSingleLayout(context, R.layout.widget_streak_layout, streak, accentColor, isEn, appWidgetId)
            val rv2x2 = buildSingleLayout(context, R.layout.widget_streak_2x2_layout, streak, accentColor, isEn, appWidgetId)

            val viewMapping = mapOf(
                android.util.SizeF(40f, 40f) to rvCompact,    // 1x1: number only
                android.util.SizeF(40f, 100f) to rvCompact,   // 1x2 (width 1, height 2): number only
                android.util.SizeF(105f, 40f) to rvWide,      // 2x1: number + text
                android.util.SizeF(105f, 100f) to rv2x2       // 2x2: number + text
            )
            return RemoteViews(viewMapping)
        }

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0

        val layoutId = when {
            minWidth in 1..104 -> R.layout.widget_streak_compact_layout
            minHeight >= 100 || maxHeight >= 130 -> R.layout.widget_streak_2x2_layout
            else -> R.layout.widget_streak_layout
        }

        return buildSingleLayout(context, layoutId, streak, accentColor, isEn, appWidgetId)
    }

    private fun buildSingleLayout(
        context: Context,
        layoutId: Int,
        streak: Int,
        accentColor: Int,
        isEn: Boolean,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutId)
        views.setTextColor(R.id.widget_streak_count, accentColor)
        val dotBitmap = WidgetBitmapUtils.createColoredCircleBitmap(24, accentColor)
        views.setImageViewBitmap(R.id.widget_streak_dot, dotBitmap)
        views.setTextViewText(R.id.widget_streak_count, streak.toString())

        val (topText, bottomText) = if (streak == 1) {
            (if (isEn) "DAY" else "TAG") to "STREAK"
        } else {
            (if (isEn) "DAYS" else "TAGE") to "STREAK"
        }
        views.setTextViewText(R.id.widget_streak_label_top, topText)
        views.setTextViewText(R.id.widget_streak_label_bottom, bottomText)
        views.setTextViewText(R.id.widget_streak_label, "$topText $bottomText")

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 2000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_streak_root, openAppPendingIntent)

        return views
    }

    private fun calculateStreak(completionDates: List<String>): Int {
        if (completionDates.isEmpty()) return 0
        val dateSet = completionDates.toSet()
        val cal = java.util.Calendar.getInstance()
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

        var checkKey = format.format(cal.time)
        if (!dateSet.contains(checkKey)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
            if (!dateSet.contains(checkKey)) {
                return 0
            }
        }

        var streak = 0
        while (dateSet.contains(checkKey)) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
        }
        return streak
    }
}
