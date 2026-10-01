package com.example.data

import android.content.Context
import com.example.util.DateUtils
import com.example.widget.DailyWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

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
            dao.getCompletionsForDateKeysFlow(weekDays)
        ) { tasks, todayCompletions, weekCompletions ->
            val completedMapToday = todayCompletions.associateBy { it.taskId }
            val completionsByTaskThisWeek = weekCompletions.groupBy { it.taskId }

            // Only show tasks that existed on or before dateKey, or have an existing completion for that date
            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, dateKey) || completedMapToday.containsKey(task.id)
            }

            relevantTasks.map { task ->
                val completionToday = completedMapToday[task.id]
                val currentCount = completionToday?.count ?: 0
                val isDoneToday = if (task.isCounter) currentCount >= task.targetCount else completionToday != null

                val weekList = completionsByTaskThisWeek[task.id] ?: emptyList()
                val weeklyCompletedCount = weekList.count { comp ->
                    if (task.isCounter) comp.count >= task.targetCount else comp.count > 0
                }
                val isWeeklyGoalMet = when (task.frequencyType) {
                    "WEEKLY" -> weeklyCompletedCount >= task.targetDaysPerWeek
                    else -> isDoneToday
                }

                val isDueToday = when (task.frequencyType) {
                    "WEEKDAYS" -> {
                        val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                        allowed.contains(isoDay)
                    }
                    "WEEKLY" -> {
                        !isWeeklyGoalMet || isDoneToday
                    }
                    else -> true
                }

                TaskWithCompletion(
                    task = task,
                    isCompleted = isDoneToday,
                    count = currentCount,
                    completedAt = completionToday?.completedAt,
                    weeklyCompletedCount = weeklyCompletedCount,
                    isDueToday = isDueToday,
                    isWeeklyGoalMet = isWeeklyGoalMet
                )
            }
        }
    }

    fun getAllActiveTasks(): Flow<List<DailyTask>> = dao.getAllActiveTasksFlow()

    fun getAllTasks(): Flow<List<DailyTask>> = dao.getAllTasksFlow()

    suspend fun getTaskById(id: Long): DailyTask? = dao.getTaskById(id)

    suspend fun toggleTaskCompletion(taskId: Long, dateKey: String): Boolean = withContext(Dispatchers.IO) {
        val task = dao.getTaskById(taskId)
        val existing = dao.getCompletion(taskId, dateKey)

        // If task is scheduled for specific weekdays, verify it is due on dateKey
        if (task?.frequencyType == "WEEKDAYS") {
            val isoDay = DateUtils.getIsoDayOfWeek(dateKey)
            val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!allowed.contains(isoDay)) {
                // Not due on this day: cannot check off. If previously completed, allow unchecking.
                if (existing != null) {
                    dao.deleteCompletion(taskId, dateKey)
                    DailyWidgetProvider.notifyDataChanged(context)
                }
                return@withContext false
            }
        }

        val isNowCompleted: Boolean
        if (task?.isCounter == true) {
            // For counter task, toggling marks full target or resets to 0
            if (existing != null && existing.count >= task.targetCount) {
                dao.deleteCompletion(taskId, dateKey)
                isNowCompleted = false
            } else {
                dao.insertCompletion(
                    TaskCompletion(
                        id = existing?.id ?: 0,
                        taskId = taskId,
                        dateKey = dateKey,
                        count = task.targetCount
                    )
                )
                isNowCompleted = true
            }
        } else {
            if (existing != null) {
                dao.deleteCompletion(taskId, dateKey)
                isNowCompleted = false
            } else {
                dao.insertCompletion(TaskCompletion(taskId = taskId, dateKey = dateKey, count = 1))
                isNowCompleted = true
            }
        }
        if (isNowCompleted) {
            com.example.reminder.ReminderManager.cancelTaskSnooze(context, taskId)
        }
        // Update widget
        DailyWidgetProvider.notifyDataChanged(context)
        isNowCompleted
    }

    suspend fun incrementTaskCounter(taskId: Long, dateKey: String, amount: Int): Int = withContext(Dispatchers.IO) {
        val task = dao.getTaskById(taskId) ?: return@withContext 0
        val existing = dao.getCompletion(taskId, dateKey)
        val current = existing?.count ?: 0

        // If task is not due on dateKey, do not allow incrementing
        if (task.frequencyType == "WEEKDAYS") {
            val isoDay = DateUtils.getIsoDayOfWeek(dateKey)
            val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!allowed.contains(isoDay)) {
                if (amount < 0 && current > 0) {
                    val newCount = (current + amount).coerceAtLeast(0)
                    if (newCount == 0) dao.deleteCompletion(taskId, dateKey)
                    else dao.insertCompletion(TaskCompletion(id = existing?.id ?: 0, taskId = taskId, dateKey = dateKey, count = newCount))
                    DailyWidgetProvider.notifyDataChanged(context)
                    return@withContext newCount
                }
                return@withContext current
            }
        }

        if (amount < 0 && current == 0) {
            return@withContext 0
        }
        val newCount = (current + amount).coerceAtLeast(0)
        if (newCount == 0) {
            dao.deleteCompletion(taskId, dateKey)
        } else {
            dao.insertCompletion(
                TaskCompletion(
                    id = existing?.id ?: 0,
                    taskId = taskId,
                    dateKey = dateKey,
                    count = newCount
                )
            )
        }
        if (task.isCounter && newCount >= task.targetCount) {
            com.example.reminder.ReminderManager.cancelTaskSnooze(context, taskId)
        }
        DailyWidgetProvider.notifyDataChanged(context)
        newCount
    }

    suspend fun setTaskCounter(taskId: Long, dateKey: String, count: Int): Int = withContext(Dispatchers.IO) {
        val task = dao.getTaskById(taskId)
        val existing = dao.getCompletion(taskId, dateKey)
        val clampedCount = count.coerceAtLeast(0)

        if (task?.frequencyType == "WEEKDAYS") {
            val isoDay = DateUtils.getIsoDayOfWeek(dateKey)
            val allowed = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!allowed.contains(isoDay) && clampedCount > 0) {
                return@withContext existing?.count ?: 0
            }
        }

        if (clampedCount == 0) {
            dao.deleteCompletion(taskId, dateKey)
        } else {
            dao.insertCompletion(
                TaskCompletion(
                    id = existing?.id ?: 0,
                    taskId = taskId,
                    dateKey = dateKey,
                    count = clampedCount
                )
            )
        }
        if (task != null && task.isCounter && clampedCount >= task.targetCount) {
            com.example.reminder.ReminderManager.cancelTaskSnooze(context, taskId)
        }
        DailyWidgetProvider.notifyDataChanged(context)
        clampedCount
    }

    suspend fun insertTask(task: DailyTask): Long = withContext(Dispatchers.IO) {
        val id = dao.insertTask(task)
        DailyWidgetProvider.notifyDataChanged(context)
        id
    }

    suspend fun updateTask(task: DailyTask) = withContext(Dispatchers.IO) {
        dao.updateTask(task)
        DailyWidgetProvider.notifyDataChanged(context)
    }

    suspend fun deleteTask(task: DailyTask) = withContext(Dispatchers.IO) {
        dao.deleteAllCompletionsForTask(task.id)
        dao.deleteTask(task)
        DailyWidgetProvider.notifyDataChanged(context)
    }

    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getTaskCount() == 0) {
            val defaults = listOf(
                DailyTask(
                    title = "Vitamin D3 + K2 & Omega-3",
                    category = "Supplements",
                    reminderHour = 8,
                    reminderMinute = 30,
                    reminderEnabled = true,
                    iconName = "pill",
                    orderIndex = 0,
                    createdAt = 0L
                ),
                DailyTask(
                    title = "1.5L Wasser bis Mittag",
                    category = "Gesundheit",
                    reminderHour = 12,
                    reminderMinute = 0,
                    reminderEnabled = true,
                    iconName = "water",
                    orderIndex = 1,
                    createdAt = 0L
                ),
                DailyTask(
                    title = "20 Min Fokus-Lesen",
                    category = "Fokus",
                    reminderHour = 20,
                    reminderMinute = 0,
                    reminderEnabled = false,
                    iconName = "book",
                    orderIndex = 2,
                    createdAt = 0L
                ),
                DailyTask(
                    title = "Magnesium vor dem Schlafen",
                    category = "Supplements",
                    reminderHour = 22,
                    reminderMinute = 0,
                    reminderEnabled = true,
                    iconName = "pill",
                    orderIndex = 3,
                    createdAt = 0L
                ),
                DailyTask(
                    title = "100 Liegestütze",
                    category = "Sport",
                    reminderHour = 18,
                    reminderMinute = 0,
                    reminderEnabled = true,
                    iconName = "fitness",
                    orderIndex = 4,
                    isCounter = true,
                    targetCount = 100,
                    unit = "Wdh.",
                    createdAt = 0L
                )
            )
            defaults.forEach { dao.insertTask(it) }
            DailyWidgetProvider.notifyDataChanged(context)
        }
    }

    fun getRecentCompletions(startDateKey: String): Flow<List<TaskCompletion>> {
        return dao.getRecentCompletionsFlow(startDateKey)
    }

    fun getAllCompletions(): Flow<List<TaskCompletion>> {
        return dao.getAllCompletionsFlow()
    }

    fun getAllCompletionDates(): Flow<List<String>> {
        return dao.getAllCompletionDatesFlow()
    }

    fun getFullyCompletedDates(): Flow<List<String>> {
        return combine(
            dao.getAllActiveTasksFlow(),
            dao.getAllCompletionsFlow()
        ) { tasks, allCompletions ->
            calculateFullyCompletedDates(tasks, allCompletions)
        }
    }

    fun getFullyCompletedDatesSync(): List<String> {
        val tasks = dao.getAllActiveTasksSync()
        val allCompletions = dao.getAllCompletionsSync()
        return calculateFullyCompletedDates(tasks, allCompletions)
    }

    private fun calculateFullyCompletedDates(
        tasks: List<DailyTask>,
        allCompletions: List<TaskCompletion>
    ): List<String> {
        if (tasks.isEmpty() || allCompletions.isEmpty()) return emptyList()

        val completionsByDate = allCompletions.groupBy { it.dateKey }
        val fullyCompletedDates = mutableListOf<String>()

        for ((dateKey, dayCompletions) in completionsByDate) {
            val weekDays = DateUtils.getDaysInSameWeek(dateKey)
            val weekCompletions = allCompletions.filter { weekDays.contains(it.dateKey) }
            val weekCompletionsByTask = weekCompletions.groupBy { it.taskId }
            val dayCompMap = dayCompletions.associateBy { it.taskId }

            // ONLY consider tasks that actually existed on or before this dateKey!
            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, dateKey) || dayCompMap.containsKey(task.id)
            }
            if (relevantTasks.isEmpty()) continue

            val dueTasks = relevantTasks.filter { task ->
                val compToday = dayCompMap[task.id]
                val isDoneToday = if (task.isCounter) (compToday?.count ?: 0) >= task.targetCount else compToday != null
                val weekList = weekCompletionsByTask[task.id] ?: emptyList()
                val weeklyCompletedCount = weekList.count { comp ->
                    if (task.isCounter) comp.count >= task.targetCount else comp.count > 0
                }
                DateUtils.isTaskDueOnDate(
                    frequencyType = task.frequencyType,
                    daysOfWeek = task.daysOfWeek,
                    targetDaysPerWeek = task.targetDaysPerWeek,
                    dateKey = dateKey,
                    weeklyCompletedCount = weeklyCompletedCount,
                    isCompletedOnDate = isDoneToday
                )
            }

            if (dueTasks.isNotEmpty()) {
                val allDueCompleted = dueTasks.all { task ->
                    val comp = dayCompMap[task.id]
                    if (task.isCounter) (comp?.count ?: 0) >= task.targetCount
                    else comp != null
                }
                if (allDueCompleted) {
                    fullyCompletedDates.add(dateKey)
                }
            }
        }

        return fullyCompletedDates.sortedDescending()
    }

    companion object {
        @Volatile
        private var instance: DailyRepository? = null

        fun setInstance(repo: DailyRepository) {
            instance = repo
        }

        fun getInstance(context: Context): DailyRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val app = context.applicationContext as? com.example.DailyApplication
                    val repo = app?.repository ?: run {
                        val db = DailyDatabase.getInstance(context)
                        DailyRepository(db.dailyTaskDao(), context.applicationContext)
                    }
                    instance = repo
                    repo
                }
            }
        }
    }
}
