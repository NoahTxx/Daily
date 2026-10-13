package com.example.data

import com.example.util.DateUtils

object TaskStatusCalculator {
    fun evaluate(
        task: DailyTask,
        dateKey: String,
        todayCompletions: List<TaskCompletion>,
        weekCompletions: List<TaskCompletion>,
        allCompletions: List<TaskCompletion> = emptyList()
    ): TaskWithCompletion {
        val completionToday = todayCompletions.firstOrNull { it.taskId == task.id }
        val countToday = completionToday?.count ?: 0
        val isCompleted = isCountSatisfied(task, countToday)

        val weeklyCompletedCount = weekCompletions
            .filter { it.taskId == task.id }
            .count { isCountSatisfied(task, it.count) }

        val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(
            task.targetDaysPerWeek,
            task.createdAt,
            dateKey
        )

        val lastCompletionDateKey = allCompletions
            .asSequence()
            .filter { it.taskId == task.id && it.dateKey < dateKey }
            .maxByOrNull { it.dateKey }
            ?.dateKey
            ?: task.intervalAnchorDate.ifBlank { null }

        val isWeeklyGoalMet = when (task.frequencyType) {
            "WEEKLY", "WEEKDAYS" -> weeklyCompletedCount >= effectiveTarget
            else -> isCompleted
        }

        val isDueToday = DateUtils.isTaskDueOnDate(
            frequencyType = task.frequencyType,
            daysOfWeek = task.daysOfWeek,
            targetDaysPerWeek = task.targetDaysPerWeek,
            dateKey = dateKey,
            weeklyCompletedCount = weeklyCompletedCount,
            isCompletedOnDate = isCompleted,
            intervalDays = task.intervalDays,
            intervalAnchorDate = task.intervalAnchorDate,
            lastCompletionDateKey = lastCompletionDateKey,
            createdAtMillis = task.createdAt
        )

        val isMandatoryToday = DateUtils.isTaskMandatoryOnDate(
            frequencyType = task.frequencyType,
            daysOfWeek = task.daysOfWeek,
            targetDaysPerWeek = task.targetDaysPerWeek,
            intervalDays = task.intervalDays,
            intervalAnchorDate = task.intervalAnchorDate,
            lastCompletionDateKey = lastCompletionDateKey,
            dateKey = dateKey,
            weeklyCompletedCount = weeklyCompletedCount,
            isCompletedOnDate = isCompleted,
            createdAtMillis = task.createdAt
        )

        val daysUntilDue = when (task.frequencyType) {
            "INTERVAL" -> {
                DateUtils.getDaysUntilIntervalDue(
                    intervalDays = task.intervalDays.coerceAtLeast(1),
                    anchorDateKey = task.intervalAnchorDate.ifBlank { dateKey },
                    lastCompletionDateKey = lastCompletionDateKey,
                    queryDateKey = dateKey
                ).coerceAtLeast(0)
            }
            else -> 0
        }

        return TaskWithCompletion(
            task = task,
            isCompleted = isCompleted,
            count = countToday,
            completedAt = completionToday?.completedAt,
            weeklyCompletedCount = weeklyCompletedCount,
            isDueToday = isDueToday,
            isWeeklyGoalMet = isWeeklyGoalMet,
            isMandatoryToday = isMandatoryToday,
            daysUntilDue = daysUntilDue,
            lastCompletionDateKey = lastCompletionDateKey
        )
    }

    private fun isCountSatisfied(task: DailyTask, count: Int): Boolean {
        return if (task.isCounter) count >= task.targetCount else count > 0
    }
}
