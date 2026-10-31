package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import com.example.util.DateUtils
import com.example.widget.DailyWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Suppress("DestructuringDeclaration")
class DailyRepository(
    private val dao: DailyTaskDao,
    private val context: Context,
) {

    fun getTasksWithCompletions(dateKey: String): Flow<List<TaskWithCompletion>> {
        val weekDays = DateUtils.getDaysInSameWeek(dateKey)

        return combine(
            dao.getAllActiveTasksFlow(),
            dao.getCompletionsForDateFlow(dateKey),
            dao.getCompletionsForDateKeysFlow(weekDays),
            dao.getAllCompletionsFlow(),
        ) { tasks, todayCompletions, weekCompletions, allCompletions ->
            val completedMapToday = todayCompletions.associateBy { (_, taskId) -> taskId }
            val relevantTasks = tasks.filter { (id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, createdAt) ->
                DateUtils.wasTaskCreatedOnOrBefore(createdAt, dateKey) || completedMapToday.containsKey(id)
            }

            relevantTasks.map { task ->
                TaskStatusCalculator.evaluate(
                    task = task,
                    dateKey = dateKey,
                    todayCompletions = todayCompletions,
                    weekCompletions = weekCompletions,
                    allCompletions = allCompletions,
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    fun getTasksWithCompletionsSync(dateKey: String): List<TaskWithCompletion> {
        val weekDays = DateUtils.getDaysInSameWeek(dateKey)
        val tasks = dao.getAllActiveTasksSync()
        val todayCompletions = dao.getCompletionsForDateSync(dateKey)
        val weekCompletions = dao.getCompletionsForDateKeysSync(weekDays)
        val allCompletions = dao.getAllCompletionsSync()

        val completedMapToday = todayCompletions.associateBy { (_, taskId) -> taskId }
        val relevantTasks = tasks.filter { (id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, createdAt) ->
            DateUtils.wasTaskCreatedOnOrBefore(createdAt, dateKey) || completedMapToday.containsKey(id)
        }

        return relevantTasks.map { task ->
            TaskStatusCalculator.evaluate(
                task = task,
                dateKey = dateKey,
                todayCompletions = todayCompletions,
                weekCompletions = weekCompletions,
                allCompletions = allCompletions,
            )
        }
    }

    fun getAllActiveTasks(): Flow<List<DailyTask>> = dao.getAllActiveTasksFlow()
    @Suppress("unused")
    fun getAllActiveTasksFlow(): Flow<List<DailyTask>> = dao.getAllActiveTasksFlow()

    fun getAllTasks(): Flow<List<DailyTask>> = dao.getAllTasksFlow()
    @Suppress("unused")
    fun getAllTasksFlow(): Flow<List<DailyTask>> = dao.getAllTasksFlow()

    @Suppress("unused")
    suspend fun getTaskById(id: Long): DailyTask? = dao.getTaskById(id)
    fun getTaskByIdSync(id: Long): DailyTask? = dao.getTaskByIdSync(id)

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
        } else if (task?.frequencyType == "INTERVAL") {
            val allCompletions = dao.getAllCompletionsSync()
            val lastComp = allCompletions.asSequence().filter { (_, tId, dKey, count) ->
                (tId == taskId) && (if (task.isCounter) count >= task.targetCount else count > 0) && (dKey < dateKey)
            }.maxOfOrNull { (_, _, dKey) -> dKey } ?: task.intervalAnchorDate.ifBlank { null }
            val isDue = DateUtils.isIntervalTaskDue(task.intervalDays, task.intervalAnchorDate, lastComp, dateKey)
            if (!isDue) {
                if (existing != null) {
                    dao.deleteCompletion(taskId, dateKey)
                    DailyWidgetProvider.notifyDataChanged(context)
                }
                return@withContext false
            }
        }

        val isNowCompleted: Boolean = if (task?.isCounter == true) {
            // For counter task, toggling marks full target or resets to 0
            if ((existing != null) && (existing.count >= task.targetCount)) {
                dao.deleteCompletion(taskId, dateKey)
                false
            } else {
                dao.insertCompletion(
                    TaskCompletion(
                        id = existing?.id ?: 0,
                        taskId = taskId,
                        dateKey = dateKey,
                        count = task.targetCount,
                    ),
                )
                true
            }
        } else {
            if (existing != null) {
                dao.deleteCompletion(taskId, dateKey)
                false
            } else {
                dao.insertCompletion(TaskCompletion(taskId = taskId, dateKey = dateKey, count = 1))
                true
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
                if ((amount < 0) && (current > 0)) {
                    val newCount = (current + amount).coerceAtLeast(0)
                    if (newCount == 0) dao.deleteCompletion(taskId, dateKey)
                    else dao.insertCompletion(TaskCompletion(id = existing?.id ?: 0, taskId = taskId, dateKey = dateKey, count = newCount))
                    DailyWidgetProvider.notifyDataChanged(context)
                    return@withContext newCount
                }
                return@withContext current
            }
        } else if (task.frequencyType == "INTERVAL") {
            val allCompletions = dao.getAllCompletionsSync()
            val lastComp = allCompletions.asSequence().filter { (_, tId, dKey, count) ->
                (tId == taskId) && (if (task.isCounter) count >= task.targetCount else count > 0) && (dKey < dateKey)
            }.maxOfOrNull { (_, _, dKey) -> dKey } ?: task.intervalAnchorDate.ifBlank { null }
            val isDue = DateUtils.isIntervalTaskDue(task.intervalDays, task.intervalAnchorDate, lastComp, dateKey)
            if (!isDue) {
                if ((amount < 0) && (current > 0)) {
                    val newCount = (current + amount).coerceAtLeast(0)
                    if (newCount == 0) dao.deleteCompletion(taskId, dateKey)
                    else dao.insertCompletion(TaskCompletion(id = existing?.id ?: 0, taskId = taskId, dateKey = dateKey, count = newCount))
                    DailyWidgetProvider.notifyDataChanged(context)
                    return@withContext newCount
                }
                return@withContext current
            }
        }

        if ((amount < 0) && (current == 0)) {
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
                    count = newCount,
                ),
            )
        }
        if (task.isCounter && (newCount >= task.targetCount)) {
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
            if (!allowed.contains(isoDay) && (clampedCount > 0)) {
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
                    count = clampedCount,
                ),
            )
        }
        if ((task != null) && task.isCounter && (clampedCount >= task.targetCount)) {
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

    @Suppress("unused")
    suspend fun deleteTaskById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAllCompletionsForTask(id)
        dao.deleteTaskById(id)
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
                    createdAt = 0L,
                ),
                DailyTask(
                    title = "1.5L Wasser bis Mittag",
                    category = "Gesundheit",
                    reminderHour = 12,
                    reminderMinute = 0,
                    reminderEnabled = true,
                    iconName = "water",
                    orderIndex = 1,
                    createdAt = 0L,
                ),
                DailyTask(
                    title = "20 Min Fokus-Lesen",
                    category = "Fokus",
                    reminderHour = 20,
                    reminderMinute = 0,
                    reminderEnabled = false,
                    iconName = "book",
                    durationMinutes = 20,
                    orderIndex = 2,
                    createdAt = 0L,
                ),
                DailyTask(
                    title = "Magnesium vor dem Schlafen",
                    category = "Supplements",
                    reminderHour = 22,
                    reminderMinute = 0,
                    reminderEnabled = true,
                    iconName = "pill",
                    orderIndex = 3,
                    createdAt = 0L,
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
                    createdAt = 0L,
                ),
            )
            defaults.forEach { dao.insertTask(it) }
            DailyWidgetProvider.notifyDataChanged(context)
        } else {
            // Upgrade existing default focus-reading habit if duration was unassigned
            val existingTasks = dao.getAllTasksSync()
            var changed = false
            for (t in existingTasks) {
                if (t.title.contains("Fokus-Lesen", ignoreCase = true) && (t.durationMinutes == 0)) {
                    dao.updateTask(t.copy(durationMinutes = 20))
                    changed = true
                }
            }
            if (changed) {
                DailyWidgetProvider.notifyDataChanged(context)
            }
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val tasks = dao.getAllTasksSync()
        val completions = dao.getAllCompletionsSync()

        val root = org.json.JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val tasksArray = org.json.JSONArray()
        for (t in tasks) {
            val taskObj = org.json.JSONObject()
            taskObj.put("id", t.id)
            taskObj.put("title", t.title)
            taskObj.put("category", t.category)
            taskObj.put("reminderHour", t.reminderHour ?: org.json.JSONObject.NULL)
            taskObj.put("reminderMinute", t.reminderMinute ?: org.json.JSONObject.NULL)
            taskObj.put("reminderEnabled", t.reminderEnabled)
            taskObj.put("iconName", t.iconName)
            taskObj.put("orderIndex", t.orderIndex)
            taskObj.put("isArchived", t.isArchived)
            taskObj.put("isCounter", t.isCounter)
            taskObj.put("targetCount", t.targetCount)
            taskObj.put("unit", t.unit)
            taskObj.put("frequencyType", t.frequencyType)
            taskObj.put("targetDaysPerWeek", t.targetDaysPerWeek)
            taskObj.put("daysOfWeek", t.daysOfWeek)
            taskObj.put("intervalDays", t.intervalDays)
            taskObj.put("intervalAnchorDate", t.intervalAnchorDate)
            taskObj.put("durationMinutes", t.durationMinutes)
            taskObj.put("createdAt", t.createdAt)
            tasksArray.put(taskObj)
        }
        root.put("tasks", tasksArray)

        val completionsArray = org.json.JSONArray()
        for (c in completions) {
            val compObj = org.json.JSONObject()
            compObj.put("id", c.id)
            compObj.put("taskId", c.taskId)
            compObj.put("dateKey", c.dateKey)
            compObj.put("count", c.count)
            compObj.put("completedAt", c.completedAt)
            completionsArray.put(compObj)
        }
        root.put("completions", completionsArray)

        val freezes = dao.getAllStreakFreezesSync()
        val freezesArray = org.json.JSONArray()
        for (f in freezes) {
            val fObj = org.json.JSONObject()
            fObj.put("id", f.id)
            fObj.put("startDate", f.startDate)
            fObj.put("endDate", f.endDate)
            fObj.put("type", f.type)
            fObj.put("note", f.note)
            fObj.put("createdAt", f.createdAt)
            freezesArray.put(fObj)
        }
        root.put("freezes", freezesArray)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = org.json.JSONObject(jsonString)
            val tasksArray = root.optJSONArray("tasks") ?: return@withContext false
            val completionsArray = root.optJSONArray("completions") ?: org.json.JSONArray()
            val freezesArray = root.optJSONArray("freezes")

            val idMapping = mutableMapOf<Long, Long>()

            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val task = DailyTask(
                    id = 0L,
                    title = obj.getString("title"),
                    category = obj.optString("category", "Routine"),
                    reminderHour = if (obj.isNull("reminderHour")) null else obj.optInt("reminderHour"),
                    reminderMinute = if (obj.isNull("reminderMinute")) null else obj.optInt("reminderMinute"),
                    reminderEnabled = obj.optBoolean("reminderEnabled", false),
                    iconName = obj.optString("iconName", "check"),
                    orderIndex = obj.optInt("orderIndex", 0),
                    isArchived = obj.optBoolean("isArchived", false),
                    isCounter = obj.optBoolean("isCounter", false),
                    targetCount = obj.optInt("targetCount", 1),
                    unit = obj.optString("unit", "x"),
                    frequencyType = obj.optString("frequencyType", "DAILY"),
                    targetDaysPerWeek = obj.optInt("targetDaysPerWeek", 1),
                    daysOfWeek = obj.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                    intervalDays = obj.optInt("intervalDays", 1),
                    intervalAnchorDate = obj.optString("intervalAnchorDate", ""),
                    durationMinutes = obj.optInt("durationMinutes", 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                )
                val newId = dao.insertTask(task)
                if (oldId != 0L) {
                    idMapping[oldId] = newId
                }
            }

            for (i in 0 until completionsArray.length()) {
                val obj = completionsArray.getJSONObject(i)
                val oldTaskId = obj.getLong("taskId")
                val newTaskId = idMapping[oldTaskId] ?: oldTaskId
                val comp = TaskCompletion(
                    id = 0L,
                    taskId = newTaskId,
                    dateKey = obj.getString("dateKey"),
                    count = obj.optInt("count", 1),
                    completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                )
                dao.insertCompletion(comp)
            }

            if (freezesArray != null) {
                for (i in 0 until freezesArray.length()) {
                    val obj = freezesArray.getJSONObject(i)
                    val freeze = StreakFreeze(
                        id = 0L,
                        startDate = obj.getString("startDate"),
                        endDate = obj.getString("endDate"),
                        type = obj.optString("type", "SICKNESS"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                    dao.insertStreakFreeze(freeze)
                }
            }

            com.example.reminder.ReminderManager.rescheduleAllReminders(context)
            DailyWidgetProvider.notifyDataChanged(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Suppress("unused")
    fun getRecentCompletions(startDateKey: String): Flow<List<TaskCompletion>> {
        return dao.getRecentCompletionsFlow(startDateKey)
    }

    fun getAllCompletions(): Flow<List<TaskCompletion>> {
        return dao.getAllCompletionsFlow()
    }

    @Suppress("unused")
    fun getAllCompletionsFlow(): Flow<List<TaskCompletion>> {
        return dao.getAllCompletionsFlow()
    }

    fun getAllCompletionDates(): Flow<List<String>> {
        return dao.getAllCompletionDatesFlow()
    }

    // Streak Freezes
    fun getAllStreakFreezesFlow(): Flow<List<StreakFreeze>> = dao.getAllStreakFreezesFlow()
    fun getAllStreakFreezesSync(): List<StreakFreeze> = dao.getAllStreakFreezesSync()
    @Suppress("unused")
    fun getActiveFreezeForDateFlow(dateKey: String): Flow<StreakFreeze?> = dao.getActiveFreezeForDateFlow(dateKey)
    @Suppress("unused")
    suspend fun getActiveFreezeForDate(dateKey: String): StreakFreeze? = dao.getActiveFreezeForDate(dateKey)
    suspend fun insertStreakFreeze(freeze: StreakFreeze): Long {
        val id = dao.insertStreakFreeze(freeze)
        DailyWidgetProvider.notifyDataChanged(context)
        return id
    }
    suspend fun deleteStreakFreeze(freeze: StreakFreeze) {
        dao.deleteStreakFreeze(freeze)
        DailyWidgetProvider.notifyDataChanged(context)
    }
    suspend fun deleteStreakFreezeById(id: Long) {
        dao.deleteStreakFreezeById(id)
        DailyWidgetProvider.notifyDataChanged(context)
    }

    fun getFullyCompletedDates(): Flow<List<String>> {
        return combine(
            dao.getAllActiveTasksFlow(),
            dao.getAllCompletionsFlow(),
        ) { tasks, allCompletions ->
            calculateFullyCompletedDates(tasks, allCompletions)
        }.flowOn(Dispatchers.Default)
    }

    fun getFullyCompletedDatesSync(): List<String> {
        val tasks = dao.getAllActiveTasksSync()
        val allCompletions = dao.getAllCompletionsSync()
        return calculateFullyCompletedDates(tasks, allCompletions)
    }

    private fun calculateFullyCompletedDates(
        tasks: List<DailyTask>,
        allCompletions: List<TaskCompletion>,
    ): List<String> {
        if (tasks.isEmpty() || allCompletions.isEmpty()) return emptyList()

        val completionsByDate = allCompletions.groupBy { it.dateKey }
        val fullyCompletedDates = mutableSetOf<String>()

        val todayKey = DateUtils.getTodayKey()
        val earliestCompDate = allCompletions.minByOrNull { it.dateKey }?.dateKey ?: todayKey
        val earliestTaskDate = tasks.minOfOrNull { DateUtils.getDateKeyFromMillis(it.createdAt) } ?: todayKey
        val startDate = minOf(earliestCompDate, earliestTaskDate)

        var checkDate = startDate
        var currentWeekDays = emptyList<String>()
        var weekCompletionsByTask = emptyMap<Long, List<TaskCompletion>>()
        var isPastWeek = false
        val lastCompletionTracker = mutableMapOf<Long, String>()

        while (checkDate <= todayKey) {
            if (!currentWeekDays.contains(checkDate)) {
                currentWeekDays = DateUtils.getDaysInSameWeek(checkDate)
                val weekCompletions = allCompletions.filter { currentWeekDays.contains(it.dateKey) }
                weekCompletionsByTask = weekCompletions.groupBy { it.taskId }
                isPastWeek = !currentWeekDays.contains(todayKey)
            }

            val dayCompletions = completionsByDate[checkDate] ?: emptyList()
            val dayCompMap = dayCompletions.associateBy { it.taskId }

            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, checkDate) || dayCompMap.containsKey(task.id)
            }

            if (relevantTasks.isNotEmpty()) {
                val mandatoryTasks = relevantTasks.filter { task ->
                    val compToday = dayCompMap[task.id]
                    val isDoneToday = if (task.isCounter) (compToday?.count ?: 0) >= task.targetCount else compToday != null
                    val weekList = weekCompletionsByTask[task.id] ?: emptyList()
                    val weeklyCompletedCount = weekList.count { comp ->
                        if (task.isCounter) comp.count >= task.targetCount else comp.count > 0
                    }
                    val lastCompBeforeDate = lastCompletionTracker[task.id]

                    DateUtils.isTaskMandatoryOnDate(
                        frequencyType = task.frequencyType,
                        daysOfWeek = task.daysOfWeek,
                        targetDaysPerWeek = task.targetDaysPerWeek,
                        intervalDays = task.intervalDays,
                        intervalAnchorDate = task.intervalAnchorDate,
                        lastCompletionDateKey = lastCompBeforeDate,
                        dateKey = checkDate,
                        weeklyCompletedCount = weeklyCompletedCount,
                        isCompletedOnDate = isDoneToday,
                        createdAtMillis = task.createdAt,
                    )
                }

                val hasAnyCompletion = relevantTasks.any { task ->
                    val comp = dayCompMap[task.id]
                    if (task.isCounter) (comp?.count ?: 0) >= task.targetCount else comp != null
                }

                if (mandatoryTasks.isNotEmpty()) {
                    val allMandatoryDone = mandatoryTasks.all { task ->
                        val comp = dayCompMap[task.id]
                        if (task.isCounter) (comp?.count ?: 0) >= task.targetCount
                        else comp != null
                    }
                    if (allMandatoryDone) {
                        fullyCompletedDates.add(checkDate)
                    }
                } else {
                    if (hasAnyCompletion) {
                        fullyCompletedDates.add(checkDate)
                    } else {
                        val weeklyTasks = relevantTasks.filter { it.frequencyType == "WEEKLY" }
                        val weeklyGoalsSatisfied = (!isPastWeek) || weeklyTasks.isEmpty() || weeklyTasks.all { task ->
                            val weekList = weekCompletionsByTask[task.id] ?: emptyList()
                            val count = weekList.count { if (task.isCounter) it.count >= task.targetCount else it.count > 0 }
                            val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(task.targetDaysPerWeek, task.createdAt, checkDate)
                            count >= effectiveTarget
                        }
                        if (weeklyGoalsSatisfied) {
                            fullyCompletedDates.add(checkDate)
                        }
                    }
                }
            }

            relevantTasks.forEach { task ->
                val compToday = dayCompMap[task.id]
                val isDoneToday = if (task.isCounter) (compToday?.count ?: 0) >= task.targetCount else compToday != null
                if (isDoneToday) {
                    lastCompletionTracker[task.id] = checkDate
                }
            }

            checkDate = DateUtils.addDays(checkDate, 1)
        }

        return fullyCompletedDates.sortedDescending()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
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
