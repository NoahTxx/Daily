package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["isArchived", "orderIndex", "id"], name = "index_daily_tasks_isArchived_orderIndex"),
        Index(value = ["isArchived", "reminderEnabled"], name = "index_daily_tasks_reminderEnabled")
    ]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Supplements", // e.g. Supplements, Gesundheit, Fokus, Sport
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val reminderEnabled: Boolean = false,
    val iconName: String = "pill",
    val orderIndex: Int = 0,
    val isCounter: Boolean = false,
    val targetCount: Int = 1,
    val unit: String = "",
    val frequencyType: String = "DAILY", // "DAILY", "WEEKLY", "WEEKDAYS", "INTERVAL"
    val targetDaysPerWeek: Int = 1, // 1..7 for WEEKLY frequency
    val daysOfWeek: String = "1,2,3,4,5,6,7", // comma-separated ISO day numbers (1=Mon..7=Sun) for WEEKDAYS
    val intervalDays: Int = 1, // 1..365 for INTERVAL frequency (e.g. every 5 days)
    val intervalAnchorDate: String = "", // anchor dateKey (e.g. "2026-10-04")
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    val reminderTimeString: String?
        get() = if (reminderEnabled && reminderHour != null && reminderMinute != null) {
            String.format(java.util.Locale.US, "%02d:%02d", reminderHour, reminderMinute)
        } else null
}
