package com.example.data

import android.content.Context
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DailyRepository(
    private val dao: DailyTaskDao,
    private val context: Context
) {

    fun getTasksWithCompletions(dateKey: String): Flow<List<TaskWithCompletion>> {
        val weekDays = DateUtils.getDaysInSameWeek(dateKey)
        val isoDay = DateUtils.getIsoDayOfWeek(dateKey)

        return combine(
            dao.getAllActiveTasksFlow(),
            dao.getCompletionsForDateFlow(dateKey),
            dao.getCompletionsForDateKeysFlow(weekDays),
            dao.getAllCompletionsFlow()
        ) { tasks, todayCompletions, weekCompletions, allCompletions ->
            val completedMapToday = todayCompletions.associateBy { it.taskId }
            val completionsByTaskThisWeek = weekCompletions.groupBy { it.taskId }
            val completionsByTask = allCompletions.groupBy { it.taskId }

            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, dateKey) ||
                    completedMapToday.containsKey(task.id)
            }

            relevantTasks.map { task ->
                val completionToday = completedMapToday[task.id]
                val currentCount = completionToday?.count ?: 0
                val isDoneToday = if (task.isCounter) {
                    currentCount >= task.targetCount
                } else {
                    completionToday != null
                }

                val weeklyCompletedCount = (completionsByTaskThisWeek[task.id] ?: emptyList())
                    .count { completion ->
                        if (task.isCounter) completion.count >= task.targetCount else completion.count > 0
                    }

                val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(
                    task.targetDaysPerWeek,
                    task.createdAt,
                    dateKey
                )
                val isWeeklyGoalMet = when (task.frequencyType) {
                    "WEEKLY" -> weeklyCompletedCount >= effectiveTarget
                    else -> isDoneToday
                }

                val allTaskCompletions = completionsByTask[task.id] ?: emptyList()
                val lastCompBeforeDate = allTaskCompletions
                    .asSequence()
                    .filter { comp ->
                        comp.dateKey < dateKey &&
                            (if (task.isCounter) comp.count >= task.targetCount else comp.count > 0)
                    }
                    .maxByOrNull { it.dateKey }
                    ?.dateKey

                val dueState = computeDueState(
                    task = task,
                    isoDay = isoDay,
                    dateKey = dateKey,
                    weeklyCompletedCount = weeklyCompletedCount,
                    effectiveTarget = effectiveTarget,
                    isDoneToday = isDoneToday,
                    isWeeklyGoalMet = isWeeklyGoalMet,
                    lastCompletionDateKey = lastCompBeforeDate
                )

                TaskWithCompletion(
                    task = task,
                    isCompleted = isDoneToday,
                    count = currentCount,
                    completedAt = completionToday?.completedAt,
                    weeklyCompletedCount = weeklyCompletedCount,
                    isDueToday = dueState.isDueToday,
                    isWeeklyGoalMet = isWeeklyGoalMet,
                    isMandatoryToday = dueState.isMandatoryToday,
                    daysUntilDue = dueState.daysUntilDue,
                    lastCompletionDateKey = lastCompBeforeDate
                )
            }
        }
    }

    private fun computeDueState(
        task: DailyTask,
        isoDay: Int,
        dateKey: String,
        weeklyCompletedCount: Int,
        effectiveTarget: Int,
        isDoneToday: Boolean,
        isWeeklyGoalMet: Boolean,
        lastCompletionDateKey: String?
    ): DueState {
        return when (task.frequencyType) {
            "WEEKDAYS" -> {
                val allowed = task.daysOfWeek
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()

                DueState(
                    isDueToday = allowed.contains(isoDay),
                    isMandatoryToday = allowed.contains(isoDay),
                    daysUntilDue = 0
                )
            }
            "WEEKLY" -> {
                val remainingDays = DateUtils.getRemainingDaysInWeek(dateKey)
                val needed = effectiveTarget - weeklyCompletedCount
                val isMandatoryToday = !isWeeklyGoalMet && needed > 0 && remainingDays <= needed

                DueState(
                    isDueToday = isMandatoryToday || isDoneToday,
                    isMandatoryToday = isMandatoryToday,
                    daysUntilDue = 0
                )
            }
            "INTERVAL" -> {
                val intervalDue = DateUtils.isIntervalTaskDue(
                    intervalDays = task.intervalDays,
                    anchorDateKey = task.intervalAnchorDate,
                    lastCompletionDateKey = lastCompBeforeDate,
                    queryDateKey = dateKey
                )

                DueState(
                    isDueToday = intervalDue || isDoneToday,
                    isMandatoryToday = intervalDue,
                    daysUntilDue = DateUtils.getDaysUntilIntervalDue(
                        intervalDays = task.intervalDays,
                        anchorDateKey = task.intervalAnchorDate,
                        lastCompletionDateKey = lastCompBeforeDate,
                        queryDateKey = dateKey
                    )
                )
            }
            else -> {
                DueState(
                    isDueToday = true,
                    isMandatoryToday = true,
                    daysUntilDue = 0
                )
            }
        }
    }

    private data class DueState(
        val isDueToday: Boolean,
        val isMandatoryToday: Boolean,
        val daysUntilDue: Int
    )
}
