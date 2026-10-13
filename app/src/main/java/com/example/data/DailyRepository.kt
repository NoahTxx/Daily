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
                val countToday = completionToday?.count ?: 0
                val isDoneToday = if (task.isCounter) {
                    countToday >= task.targetCount
                } else {
                    completionToday != null
                }

                val weeklyList = completionsByTaskThisWeek[task.id].orEmpty()
                val weeklyCompletedCount = weeklyList.count { completion ->
                    if (task.isCounter) completion.count >= task.targetCount else completion.count > 0
                }

                val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(
                    task.targetDaysPerWeek,
                    task.createdAt,
                    dateKey
                )

                val isWeeklyGoalMet = when (task.frequencyType) {
                    "WEEKLY" -> weeklyCompletedCount >= effectiveTarget
                    "WEEKDAYS" -> weeklyCompletedCount >= effectiveTarget
                    else -> isDoneToday
                }

                val lastCompletionDateKey = completionToday?.dateKey
                val daysUntilDue = when (task.frequencyType) {
                    "INTERVAL" -> {
                        val intervalDays = task.intervalDays.coerceAtLeast(1)
                        val anchor = task.intervalAnchorDate.ifBlank { dateKey }
                        val lastKey = lastCompletionDateKey ?: anchor
                        val diff = DateUtils.daysBetween(lastKey, dateKey)
                        (intervalDays - diff).coerceAtLeast(0)
                    }
                    else -> 0
                }

                TaskWithCompletion(
                    task = task,
                    isCompleted = isDoneToday,
                    count = countToday,
                    completedAt = completionToday?.completedAt,
                    weeklyCompletedCount = weeklyCompletedCount,
                    isDueToday = true,
                    isWeeklyGoalMet = isWeeklyGoalMet,
                    isMandatoryToday = true,
                    daysUntilDue = daysUntilDue,
                    lastCompletionDateKey = lastCompletionDateKey
                )
            }
        }
    }

    fun getTaskById(id: Long) = dao.getTaskById(id)
    fun getTaskByIdSync(id: Long) = dao.getTaskByIdSync(id)
    fun getAllTasksFlow() = dao.getAllTasksFlow()
    fun getAllActiveTasksFlow() = dao.getAllActiveTasksFlow()
    fun getAllCompletionsFlow() = dao.getAllCompletionsFlow()

    suspend fun deleteTask(task: DailyTask) = dao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = dao.deleteTaskById(id)
    suspend fun insertTask(task: DailyTask) = dao.insertTask(task)
    suspend fun updateTask(task: DailyTask) = dao.updateTask(task)
}
