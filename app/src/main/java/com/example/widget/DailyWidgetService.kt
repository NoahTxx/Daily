package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.data.DailyDatabase
import com.example.data.TaskWithCompletion
import com.example.util.AppSettings
import com.example.util.DateUtils

class DailyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return DailyRemoteViewsFactory(applicationContext)
    }
}

class DailyRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TaskWithCompletion> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val repository = com.example.data.DailyRepository.getInstance(context)
            val todayKey = DateUtils.getTodayKey()
            val allTasksWithStatus = repository.getTasksWithCompletionsSync(todayKey)
            // Only show tasks that are due today or already completed today
            items = allTasksWithStatus.filter { it.isDueToday || it.isCompleted }
        } catch (e: Exception) {
            e.printStackTrace()
            items = emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }

        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        val appSettings = AppSettings.getInstance(context)
        val accentColor = appSettings.selectedAccent.value.color.toArgb()

        views.setTextViewText(R.id.widget_item_title, item.task.title)

        if (item.isCompleted) {
            views.setImageViewResource(
                R.id.widget_item_checkbox,
                R.drawable.widget_checkbox_checked
            )
            views.setInt(R.id.widget_item_checkbox, "setColorFilter", accentColor)
            views.setTextColor(R.id.widget_item_title, Color.parseColor("#666672"))
            if (item.task.isCounter) {
                views.setTextColor(R.id.widget_item_counter_badge, accentColor)
            }
        } else {
            views.setImageViewResource(
                R.id.widget_item_checkbox,
                R.drawable.widget_checkbox_unchecked
            )
            views.setInt(R.id.widget_item_checkbox, "setColorFilter", 0)
            views.setTextColor(R.id.widget_item_title, Color.parseColor("#FFFFFF"))
            if (item.task.isCounter) {
                views.setTextColor(R.id.widget_item_counter_badge, Color.parseColor("#A0A0AA"))
            }
        }

        // Subtext (Category)
        if (item.task.category.isNotBlank()) {
            views.setViewVisibility(R.id.widget_item_subtext, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_subtext, item.task.category.uppercase())
        } else {
            views.setViewVisibility(R.id.widget_item_subtext, View.GONE)
        }

        // Reminder Time
        if (item.task.reminderEnabled && item.task.reminderHour != null && item.task.reminderMinute != null) {
            views.setViewVisibility(R.id.widget_item_time, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_item_time,
                DateUtils.formatTime(item.task.reminderHour, item.task.reminderMinute)
            )
        } else {
            views.setViewVisibility(R.id.widget_item_time, View.GONE)
        }

        // Counter Task UI & Quick Increment Button
        if (item.task.isCounter) {
            views.setViewVisibility(R.id.widget_item_counter_badge, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_item_counter_badge,
                "${item.count}/${item.task.targetCount}"
            )

            views.setViewVisibility(R.id.widget_item_counter_btn, View.VISIBLE)
            val step = if (item.task.targetCount >= 100) 10 else 1
            views.setTextViewText(R.id.widget_item_counter_btn, "+$step")
            views.setTextColor(R.id.widget_item_counter_btn, accentColor)

            // Fill-in intent to increment counter
            val incIntent = Intent().apply {
                putExtra(DailyWidgetProvider.EXTRA_TASK_ID, item.task.id)
                putExtra(DailyWidgetProvider.EXTRA_ACTION_TYPE, DailyWidgetProvider.ACTION_TYPE_INCREMENT)
                putExtra(DailyWidgetProvider.EXTRA_AMOUNT, step)
            }
            views.setOnClickFillInIntent(R.id.widget_item_counter_btn, incIntent)
        } else {
            views.setViewVisibility(R.id.widget_item_counter_badge, View.GONE)
            views.setViewVisibility(R.id.widget_item_counter_btn, View.GONE)
        }

        // Fill-in intent to toggle task on click
        val toggleIntent = Intent().apply {
            putExtra(DailyWidgetProvider.EXTRA_TASK_ID, item.task.id)
            putExtra(DailyWidgetProvider.EXTRA_ACTION_TYPE, DailyWidgetProvider.ACTION_TYPE_TOGGLE)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, toggleIntent)
        views.setOnClickFillInIntent(R.id.widget_item_checkbox, toggleIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) items[position].task.id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
