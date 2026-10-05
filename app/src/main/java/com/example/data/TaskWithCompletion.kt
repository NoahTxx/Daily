package com.example.data

import androidx.compose.runtime.Immutable

@Immutable
data class TaskWithCompletion(
    val task: DailyTask,
    val isCompleted: Boolean,
    val count: Int = 0,
    val completedAt: Long? = null,
    val weeklyCompletedCount: Int = 0,
    val isDueToday: Boolean = true,
    val isWeeklyGoalMet: Boolean = false,
    val isMandatoryToday: Boolean = true,
    val daysUntilDue: Int = 0,
    val lastCompletionDateKey: String? = null
)
