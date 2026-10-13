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
            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, dateKey) ||
                    todayCompletions.any { it.taskId == task.id }
            }

            relevantTasks.map { task ->
                TaskStatusCalculator.evaluate(
                    task = task,
                    dateKey = dateKey,
                    todayCompletions = todayCompletions,
                    weekCompletions = weekCompletions,
                    allCompletions = allCompletions
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
