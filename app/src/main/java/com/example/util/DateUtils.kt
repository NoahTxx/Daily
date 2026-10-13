package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateUtils {
    private val keyFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val keyFormat: SimpleDateFormat get() = keyFormatThreadLocal.get()!!

    @Volatile
    private var cachedTodayDayMillis: Long = 0L
    @Volatile
    private var cachedTodayKey: String = ""

    private val isoDayOfWeekCache = ConcurrentHashMap<String, Int>(64)
    private val displayDateCache = ConcurrentHashMap<String, String>(64)
    private val shortDisplayCache = ConcurrentHashMap<String, String>(64)
    private val daysOfWeekShortCache = ConcurrentHashMap<String, String>(64)
    private val dayNumberCache = ConcurrentHashMap<String, String>(64)
    private val daysEn = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val daysDe = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")

    fun getTodayKey(): String {
        val now = System.currentTimeMillis()
        if (now - cachedTodayDayMillis in 0..5000L && cachedTodayKey.isNotEmpty()) {
            return cachedTodayKey
        }
        val key = keyFormat.format(java.util.Date(now))
        cachedTodayKey = key
        cachedTodayDayMillis = now
        return key
    }

    fun isToday(dateKey: String): Boolean {
        return dateKey == getTodayKey()
    }

    fun formatDisplayDate(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        val isTodayDate = isToday(dateKey)
        val cacheKey = "$dateKey-$language-$isTodayDate"
        displayDateCache[cacheKey]?.let { return it }

        return try {
            val date = keyFormat.parse(dateKey) ?: return dateKey
            val isEn = language == AppLanguage.EN
            val result = if (isTodayDate) {
                if (isEn) {
                    "TODAY, " + SimpleDateFormat("MMMM d", Locale.US).format(date).uppercase()
                } else {
                    "HEUTE, " + SimpleDateFormat("d. MMMM", Locale.GERMAN).format(date).uppercase()
                }
            } else {
                if (isEn) {
                    SimpleDateFormat("EEEE, MMMM d", Locale.US).format(date).uppercase()
                } else {
                    SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN).format(date).uppercase()
                }
            }
            displayDateCache[cacheKey] = result
            result
        } catch (e: Exception) {
            dateKey
        }
    }

    fun formatShortDisplay(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        val cacheKey = "$dateKey-$language"
        shortDisplayCache[cacheKey]?.let { return it }

        return try {
            val date = keyFormat.parse(dateKey) ?: return dateKey
            val locale = if (language == AppLanguage.EN) Locale.US else Locale.GERMAN
            val pattern = if (language == AppLanguage.EN) "EEE, MMM d" else "EE, d. MMM"
            val result = SimpleDateFormat(pattern, locale).format(date).uppercase()
            shortDisplayCache[cacheKey] = result
            result
        } catch (e: Exception) {
            dateKey
        }
    }

    fun getDayOfWeek(dateKey: String, language: AppLanguage = AppLanguage.DE): String {
        val iso = getIsoDayOfWeek(dateKey)
        if (iso in 1..7) {
            return if (language == AppLanguage.EN) daysEn[iso - 1] else daysDe[iso - 1]
        }
        return ""
    }

    fun getDayNumber(dateKey: String): String {
        dayNumberCache[dateKey]?.let { return it }
        val result = if (dateKey.length >= 10) {
            val d1 = dateKey[8]
            val d2 = dateKey[9]
            if (d1 == '0') d2.toString() else "$d1$d2"
        } else {
            dateKey
        }
        dayNumberCache[dateKey] = result
        return result
    }

    fun getPastDays(daysCount: Int): List<String> {
        val list = ArrayList<String>(daysCount)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(daysCount - 1))
        for (i in 0 until daysCount) {
            list.add(keyFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun getIsoDayOfWeek(dateKey: String): Int {
        val cached = isoDayOfWeekCache[dateKey]
        if (cached != null) return cached

        return try {
            val date = keyFormat.parse(dateKey) ?: 1
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                time = date as java.util.Date
            }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            val res = if (day == Calendar.SUNDAY) 7 else day - 1
            isoDayOfWeekCache[dateKey] = res
            res
        } catch (e: Exception) {
            1
        }
    }

    fun parseWeekdaySet(daysOfWeek: String): Set<Int> {
        if (daysOfWeek.isBlank()) return emptySet()
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun getDaysInSameWeek(dateKey: String): List<String> {
        return try {
            val date = keyFormat.parse(dateKey) ?: return listOf(dateKey)
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                time = date
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val list = ArrayList<String>(7)
            for (i in 0 until 7) {
                list.add(keyFormat.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            list
        } catch (e: Exception) {
            listOf(dateKey)
        }
    }

    fun formatDaysOfWeekShort(daysOfWeekCsv: String, language: AppLanguage = AppLanguage.DE): String {
        val cacheKey = "$daysOfWeekCsv-$language"
        daysOfWeekShortCache[cacheKey]?.let { return it }

        val days = parseWeekdaySet(daysOfWeekCsv).toList().sorted()
        val result = if (days.size == 7) {
            if (language == AppLanguage.EN) "Daily" else "Täglich"
        } else if (days == listOf(1, 2, 3, 4, 5)) {
            if (language == AppLanguage.EN) "Weekdays" else "Werktage"
        } else if (days == listOf(6, 7)) {
            if (language == AppLanguage.EN) "Weekend" else "Wochenende"
        } else {
            val namesEn = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
            val namesDe = mapOf(1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do", 5 to "Fr", 6 to "Sa", 7 to "So")
            val nameMap = if (language == AppLanguage.EN) namesEn else namesDe
            days.mapNotNull { nameMap[it] }.joinToString(", ")
        }
        daysOfWeekShortCache[cacheKey] = result
        return result
    }

    fun daysBetween(fromKey: String, toKey: String): Int {
        if (fromKey == toKey) return 0
        return try {
            val d1 = keyFormat.parse(fromKey) ?: return 0
            val d2 = keyFormat.parse(toKey) ?: return 0
            val diffMs = d2.time - d1.time
            (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun addDays(dateKey: String, days: Int): String {
        if (days == 0) return dateKey
        return try {
            val cal = Calendar.getInstance().apply {
                time = keyFormat.parse(dateKey) ?: return dateKey
                add(Calendar.DAY_OF_YEAR, days)
            }
            keyFormat.format(cal.time)
        } catch (e: Exception) {
            dateKey
        }
    }

    fun getRemainingDaysInWeek(dateKey: String): Int {
        val isoDay = getIsoDayOfWeek(dateKey) // 1..7 (1=Mon, 7=Sun)
        return (7 - isoDay + 1).coerceIn(1, 7)
    }

    fun isIntervalTaskDue(
        intervalDays: Int,
        anchorDateKey: String,
        lastCompletionDateKey: String?,
        queryDateKey: String
    ): Boolean {
        val safeInterval = intervalDays.coerceAtLeast(1)
        if (!lastCompletionDateKey.isNullOrBlank()) {
            val nextDue = addDays(lastCompletionDateKey, safeInterval)
            return queryDateKey >= nextDue
        }
        val anchor = if (anchorDateKey.isNotBlank()) anchorDateKey else queryDateKey
        return queryDateKey >= anchor
    }

    fun getDaysUntilIntervalDue(
        intervalDays: Int,
        anchorDateKey: String,
        lastCompletionDateKey: String?,
        queryDateKey: String
    ): Int {
        val safeInterval = intervalDays.coerceAtLeast(1)
        val nextDue = if (!lastCompletionDateKey.isNullOrBlank()) {
            addDays(lastCompletionDateKey, safeInterval)
        } else {
            if (anchorDateKey.isNotBlank()) anchorDateKey else queryDateKey
        }
        return daysBetween(queryDateKey, nextDue)
    }

    fun getEffectiveWeeklyTarget(
        targetDaysPerWeek: Int,
        createdAtMillis: Long,
        queryDateKey: String
    ): Int {
        if (createdAtMillis <= 0L) return targetDaysPerWeek
        val createdKey = getDateKeyFromMillis(createdAtMillis)
        val queryWeekDays = getDaysInSameWeek(queryDateKey)
        if (queryWeekDays.contains(createdKey)) {
            val createdIsoDay = getIsoDayOfWeek(createdKey)
            val daysAvailable = (7 - createdIsoDay + 1).coerceIn(1, 7)
            return minOf(targetDaysPerWeek, daysAvailable)
        }
        return targetDaysPerWeek
    }

    fun isTaskMandatoryOnDate(
        frequencyType: String,
        daysOfWeek: String,
        targetDaysPerWeek: Int,
        intervalDays: Int,
        intervalAnchorDate: String,
        lastCompletionDateKey: String?,
        dateKey: String,
        weeklyCompletedCount: Int,
        isCompletedOnDate: Boolean,
        createdAtMillis: Long = 0L
    ): Boolean {
        return when (frequencyType) {
            "DAILY" -> true
            "WEEKDAYS" -> {
                val isoDay = getIsoDayOfWeek(dateKey)
                parseWeekdaySet(daysOfWeek).contains(isoDay)
            }
            "INTERVAL" -> {
                isIntervalTaskDue(intervalDays, intervalAnchorDate, lastCompletionDateKey, dateKey)
            }
            "WEEKLY" -> {
                val effectiveTarget = getEffectiveWeeklyTarget(targetDaysPerWeek, createdAtMillis, dateKey)
                if (weeklyCompletedCount >= effectiveTarget) {
                    false
                } else {
                    val remainingDays = getRemainingDaysInWeek(dateKey)
                    val needed = effectiveTarget - weeklyCompletedCount
                    remainingDays <= needed
                }
            }
            else -> true
        }
    }

    fun isTaskDueOnDate(
        frequencyType: String,
        daysOfWeek: String,
        targetDaysPerWeek: Int,
        dateKey: String,
        weeklyCompletedCount: Int = 0,
        isCompletedOnDate: Boolean = false,
        intervalDays: Int = 1,
        intervalAnchorDate: String = "",
        lastCompletionDateKey: String? = null,
        createdAtMillis: Long = 0L
    ): Boolean {
        return when (frequencyType) {
            "WEEKDAYS" -> {
                val isoDay = getIsoDayOfWeek(dateKey)
                parseWeekdaySet(daysOfWeek).contains(isoDay)
            }
            "WEEKLY" -> {
                val effectiveTarget = getEffectiveWeeklyTarget(targetDaysPerWeek, createdAtMillis, dateKey)
                val isGoalMet = weeklyCompletedCount >= effectiveTarget
                val remainingDays = getRemainingDaysInWeek(dateKey)
                val needed = effectiveTarget - weeklyCompletedCount
                val isMandatory = !isGoalMet && needed > 0 && remainingDays <= needed
                isMandatory || isCompletedOnDate
            }
            "INTERVAL" -> {
                isIntervalTaskDue(intervalDays, intervalAnchorDate, lastCompletionDateKey, dateKey) || isCompletedOnDate
            }
            else -> true
        }
    }

    fun getDateKeyFromMillis(millis: Long): String {
        return try {
            keyFormat.format(java.util.Date(millis))
        } catch (e: Exception) {
            getTodayKey()
        }
    }

    fun wasTaskCreatedOnOrBefore(createdAtMillis: Long, dateKey: String): Boolean {
        if (createdAtMillis <= 0L) return true
        val createdKey = getDateKeyFromMillis(createdAtMillis)
        if (createdKey.isBlank()) return true
        return createdKey <= dateKey
    }
}

""" ,{"content":"package com.example.data

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

    private fun isCompletionSatisfied(task: DailyTask, count: Int): Boolean {
        return if (task.isCounter) count >= task.targetCount else count > 0
    }

    private fun isMatchingWeekday(task: DailyTask, dateKey: String): Boolean {
        val isoDay = DateUtils.getIsoDayOfWeek(dateKey)
        return DateUtils.parseWeekdaySet(task.daysOfWeek).contains(isoDay)
    }

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
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, dateKey) || completedMapToday.containsKey(task.id)
            }

            relevantTasks.map { task ->
                val completionToday = completedMapToday[task.id]
                val currentCount = completionToday?.count ?: 0
                val isDoneToday = isCompletionSatisfied(task, currentCount)

                val weekList = completionsByTaskThisWeek[task.id] ?: emptyList()
                val weeklyCompletedCount = weekList.count { comp -> isCompletionSatisfied(task, comp.count) }
                val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(task.targetDaysPerWeek, task.createdAt, dateKey)
                val isWeeklyGoalMet = when (task.frequencyType) {
                    "WEEKLY" -> weeklyCompletedCount >= effectiveTarget
                    else -> isDoneToday
                }

                val allTaskComps = completionsByTask[task.id] ?: emptyList()
                val lastCompBeforeDate = allTaskComps
                    .filter { it.dateKey < dateKey && isCompletionSatisfied(task, it.count) }
                    .maxOfOrNull { it.dateKey }

                val isDueToday: Boolean
                val isMandatoryToday: Boolean
                val daysUntilDue: Int

                when (task.frequencyType) {
                    "WEEKDAYS" -> {
                        val allowed = DateUtils.parseWeekdaySet(task.daysOfWeek)
                        isDueToday = allowed.contains(isoDay)
                        isMandatoryToday = isDueToday
                        daysUntilDue = 0
                    }
                    "WEEKLY" -> {
                        val remainingDays = DateUtils.getRemainingDaysInWeek(dateKey)
                        val needed = effectiveTarget - weeklyCompletedCount
                        isMandatoryToday = !isWeeklyGoalMet && needed > 0 && remainingDays <= needed
                        isDueToday = isMandatoryToday || isDoneToday
                        daysUntilDue = 0
                    }
                    "INTERVAL" -> {
                        val intervalDue = DateUtils.isIntervalTaskDue(
                            intervalDays = task.intervalDays,
                            anchorDateKey = task.intervalAnchorDate,
                            lastCompletionDateKey = lastCompBeforeDate,
                            queryDateKey = dateKey
                        )
                        isDueToday = intervalDue || isDoneToday
                        isMandatoryToday = intervalDue
                        daysUntilDue = DateUtils.getDaysUntilIntervalDue(
                            intervalDays = task.intervalDays,
                            anchorDateKey = task.intervalAnchorDate,
                            lastCompletionDateKey = lastCompBeforeDate,
                            queryDateKey = dateKey
                        )
                    }
                    else -> {
                        isDueToday = true
                        isMandatoryToday = true
                        daysUntilDue = 0
                    }
                }

                TaskWithCompletion(
                    task = task,
                    isCompleted = isDoneToday,
                    count = currentCount,
                    completedAt = completionToday?.completedAt,
                    weeklyCompletedCount = weeklyCompletedCount,
                    isDueToday = isDueToday,
                    isWeeklyGoalMet = isWeeklyGoalMet,
                    isMandatoryToday = isMandatoryToday,
                    daysUntilDue = daysUntilDue,
                    lastCompletionDateKey = lastCompBeforeDate
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

        if (task?.frequencyType == "WEEKDAYS") {
            if (!isMatchingWeekday(task, dateKey)) {
                if (existing != null) {
                    dao.deleteCompletion(taskId, dateKey)
                    DailyWidgetProvider.notifyDataChanged(context)
                }
                return@withContext false
            }
        }

        val isNowCompleted: Boolean
        if (task?.isCounter == true) {
            if (existing != null && isCompletionSatisfied(task, existing.count)) {
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
        DailyWidgetProvider.notifyDataChanged(context)
        isNowCompleted
    }

    suspend fun incrementTaskCounter(taskId: Long, dateKey: String, amount: Int): Int = withContext(Dispatchers.IO) {
        val task = dao.getTaskById(taskId) ?: return@withContext 0
        val existing = dao.getCompletion(taskId, dateKey)
        val current = existing?.count ?: 0

        if (task.frequencyType == "WEEKDAYS" && !isMatchingWeekday(task, dateKey)) {
            if (amount < 0 && current > 0) {
                val newCount = (current + amount).coerceAtLeast(0)
                if (newCount == 0) dao.deleteCompletion(taskId, dateKey)
                else dao.insertCompletion(TaskCompletion(id = existing?.id ?: 0, taskId = taskId, dateKey = dateKey, count = newCount))
                DailyWidgetProvider.notifyDataChanged(context)
                return@withContext newCount
            }
            return@withContext current
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

        if (task?.frequencyType == "WEEKDAYS" && !isMatchingWeekday(task, dateKey) && clampedCount > 0) {
            return@withContext existing?.count ?: 0
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
        val completionsByTask = allCompletions.groupBy { it.taskId }
        val fullyCompletedDates = mutableSetOf<String>()

        val todayKey = DateUtils.getTodayKey()
        val earliestCompDate = allCompletions.map { it.dateKey }.minOrNull() ?: todayKey
        val earliestTaskDate = tasks.map { DateUtils.getDateKeyFromMillis(it.createdAt) }.minOrNull() ?: todayKey
        val startDate = minOf(earliestCompDate, earliestTaskDate)

        var checkDate = startDate
        while (checkDate <= todayKey) {
            val dayCompletions = completionsByDate[checkDate] ?: emptyList()
            val dayCompMap = dayCompletions.associateBy { it.taskId }
            val weekDays = DateUtils.getDaysInSameWeek(checkDate)
            val weekCompletions = allCompletions.filter { weekDays.contains(it.dateKey) }
            val weekCompletionsByTask = weekCompletions.groupBy { it.taskId }

            val relevantTasks = tasks.filter { task ->
                DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, checkDate) || dayCompMap.containsKey(task.id)
            }

            if (relevantTasks.isNotEmpty()) {
                val mandatoryTasks = relevantTasks.filter { task ->
                    val compToday = dayCompMap[task.id]
                    val isDoneToday = isCompletionSatisfied(task, compToday?.count ?: 0)
                    val weekList = weekCompletionsByTask[task.id] ?: emptyList()
                    val weeklyCompletedCount = weekList.count { comp -> isCompletionSatisfied(task, comp.count) }
                    val allTaskComps = completionsByTask[task.id] ?: emptyList()
                    val lastCompBeforeDate = allTaskComps
                        .filter { it.dateKey < checkDate && isCompletionSatisfied(task, it.count) }
                        .maxOfOrNull { it.dateKey }

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
                        createdAtMillis = task.createdAt
                    )
                }

                val hasAnyCompletion = relevantTasks.any { task ->
                    val comp = dayCompMap[task.id]
                    isCompletionSatisfied(task, comp?.count ?: 0)
                }

                if (mandatoryTasks.isNotEmpty()) {
                    val allMandatoryDone = mandatoryTasks.all { task ->
                        val comp = dayCompMap[task.id]
                        isCompletionSatisfied(task, comp?.count ?: 0)
                    }
                    if (allMandatoryDone) {
                        fullyCompletedDates.add(checkDate)
                    }
                } else {
                    if (hasAnyCompletion) {
                        fullyCompletedDates.add(checkDate)
                    } else {
                        val isPastWeek = !weekDays.contains(todayKey)
                        val weeklyTasks = relevantTasks.filter { it.frequencyType == "WEEKLY" }
                        val weeklyGoalsSatisfied = if (isPastWeek && weeklyTasks.isNotEmpty()) {
                            weeklyTasks.all { task ->
                                val weekList = weekCompletionsByTask[task.id] ?: emptyList()
                                val count = weekList.count { comp -> isCompletionSatisfied(task, comp.count) }
                                val effectiveTarget = DateUtils.getEffectiveWeeklyTarget(task.targetDaysPerWeek, task.createdAt, checkDate)
                                count >= effectiveTarget
                            }
                        } else {
                            true
                        }
                        if (weeklyGoalsSatisfied) {
                            fullyCompletedDates.add(checkDate)
                        }
                    }
                }
            }
            checkDate = DateUtils.addDays(checkDate, 1)
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

""" ,{"content":"package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DailyApplication
import com.example.data.DailyRepository
import com.example.data.DailyTask
import com.example.data.TaskCompletion
import com.example.data.TaskWithCompletion
import com.example.reminder.ReminderManager
import com.example.util.AppLanguage
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Immutable
data class StreakStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDays: Int = 0,
    val isTodaySecured: Boolean = false
)

@Immutable
data class HabitDayStatus(
    val dateKey: String,
    val dayLabel: String,
    val dayNumber: String,
    val isCompleted: Boolean,
    val isToday: Boolean,
    val isDue: Boolean
)

@Immutable
data class HabitStats(
    val task: DailyTask,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val totalCountSum: Long,
    val monthlyAdherencePercent: Int,
    val monthlyCompletedDays: Int,
    val monthlyDueDays: Int,
    val isTodayCompleted: Boolean,
    val isTodayDue: Boolean,
    val completedDateSet: Set<String>,
    val recent7Days: List<HabitDayStatus>
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DailyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DailyRepository = (application as DailyApplication).repository

    private val _selectedDateKey = MutableStateFlow(DateUtils.getTodayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedHabitIdForStats = MutableStateFlow<Long?>(null)
    val selectedHabitIdForStats: StateFlow<Long?> = _selectedHabitIdForStats.asStateFlow()

    fun selectHabitForStats(taskId: Long?) {
        _selectedHabitIdForStats.value = taskId
    }

    val allActiveTasks: StateFlow<List<DailyTask>> = repository.getAllActiveTasks()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<DailyTask>> = repository.getAllTasks()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletions: StateFlow<List<TaskCompletion>> = repository.getAllCompletions()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitStatsList: StateFlow<List<HabitStats>> = combine(
        allActiveTasks,
        allCompletions
    ) { tasks, completions ->
        tasks to completions
    }
        .distinctUntilChanged()
        .debounce(100L)
        .map { (tasks, completions) ->
            withContext(Dispatchers.Default) {
                calculateHabitStatsList(tasks, completions)
            }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksWithCompletions: StateFlow<List<TaskWithCompletion>> = _selectedDateKey
        .flatMapLatest { dateKey ->
            repository.getTasksWithCompletions(dateKey)
        }
        .combine(_selectedCategory) { list, category ->
            if (category == null) list
            else list.filter { isCategoryEquivalent(it.task.category, category) }
        }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletionDates: StateFlow<List<String>> = repository.getAllCompletionDates()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullyCompletedDates: StateFlow<List<String>> = repository.getFullyCompletedDates()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakStats: StateFlow<StreakStats> = fullyCompletedDates.map { dates ->
        withContext(Dispatchers.Default) {
            calculateFullStreakStats(dates)
        }
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakStats())

    val currentStreak: StateFlow<Int> = streakStats.map { it.currentStreak }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val routinesResetInfoDismissed: StateFlow<Boolean> =
        com.example.util.AppSettings.getInstance(application).routinesResetInfoDismissed

    fun dismissRoutinesResetInfo() {
        com.example.util.AppSettings.getInstance(getApplication()).dismissRoutinesResetInfo()
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun selectCategory(category: String?) {
        val current = _selectedCategory.value
        _selectedCategory.value = if (current != null && category != null && isCategoryEquivalent(current, category)) null else category
    }

    fun toggleTask(taskId: Long) {
        if (!DateUtils.isToday(_selectedDateKey.value)) return
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, _selectedDateKey.value)
        }
    }

    fun toggleTaskActive(task: DailyTask) {
        viewModelScope.launch {
            val updated = task.copy(isArchived = !task.isArchived)
            repository.updateTask(updated)
            val context = getApplication<Application>()
            if (updated.isArchived) {
                ReminderManager.cancelReminder(context, updated.id)
            } else if (updated.reminderEnabled) {
                ReminderManager.scheduleReminder(context, updated)
            }
        }
    }

    fun incrementCounter(taskId: Long, amount: Int) {
        if (!DateUtils.isToday(_selectedDateKey.value)) return
        viewModelScope.launch {
            repository.incrementTaskCounter(taskId, _selectedDateKey.value, amount)
        }
    }

    fun setCounter(taskId: Long, count: Int) {
        if (!DateUtils.isToday(_selectedDateKey.value)) return
        viewModelScope.launch {
            repository.setTaskCounter(taskId, _selectedDateKey.value, count)
        }
    }

    fun saveTask(task: DailyTask) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (task.id == 0L) {
                val newId = repository.insertTask(task)
                val savedTask = task.copy(id = newId)
                if (savedTask.reminderEnabled) {
                    ReminderManager.scheduleReminder(context, savedTask)
                }
            } else {
                repository.updateTask(task)
                if (task.reminderEnabled) {
                    ReminderManager.scheduleReminder(context, task)
                } else {
                    ReminderManager.cancelReminder(context, task.id)
                }
            }
        }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            ReminderManager.cancelReminder(context, task.id)
            repository.deleteTask(task)
        }
    }

    private fun calculateFullStreakStats(completionDates: List<String>): StreakStats {
        if (completionDates.isEmpty()) return StreakStats()

        val dateSet = completionDates.toSet()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = DateUtils.getTodayKey()
        val isTodaySecured = dateSet.contains(todayKey)

        val cal = Calendar.getInstance()
        var checkKey = format.format(cal.time)
        var currentStreak = 0

        if (!dateSet.contains(checkKey)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
            if (!dateSet.contains(checkKey)) {
                currentStreak = 0
            } else {
                while (dateSet.contains(checkKey)) {
                    currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    checkKey = format.format(cal.time)
                }
            }
        } else {
            while (dateSet.contains(checkKey)) {
                currentStreak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                checkKey = format.format(cal.time)
            }
        }

        val sortedDates = completionDates.distinct().sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevCal: Calendar? = null

        for (dStr in sortedDates) {
            val currentD = Calendar.getInstance().apply {
                time = format.parse(dStr) ?: return@apply
            }

            if (prevCal == null) {
                tempStreak = 1
            } else {
                val diffDays = ((currentD.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    tempStreak++
                } else if (diffDays > 1) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevCal = currentD
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
        }

        return StreakStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDays = dateSet.size,
            isTodaySecured = isTodaySecured
        )
    }

    private data class HabitStatsCacheKey(
        val task: DailyTask,
        val completions: List<TaskCompletion>,
        val todayKey: String
    )
    private val habitStatsCache = java.util.concurrent.ConcurrentHashMap<Long, Pair<HabitStatsCacheKey, HabitStats>>()

    internal fun calculateHabitStatsList(
        tasks: List<DailyTask>,
        completions: List<TaskCompletion>
    ): List<HabitStats> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = DateUtils.getTodayKey()
        val completionsByTask = completions.groupBy { it.taskId }

        val activeIds = tasks.map { it.id }.toSet()
        habitStatsCache.keys.retainAll(activeIds)

        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val monthDates = (1..daysInMonth).map { String.format(Locale.US, "%s-%02d", monthPrefix, it) }
        val recent7DateKeys = DateUtils.getPastDays(7)

        return tasks.map { task ->
            val taskComps = completionsByTask[task.id] ?: emptyList()

            val cached = habitStatsCache[task.id]
            if (cached != null && cached.first.task == task && cached.first.completions == taskComps && cached.first.todayKey == todayKey) {
                return@map cached.second
            }

            val validCompletions = taskComps.filter { comp ->
                if (task.isCounter) comp.count >= task.targetCount else comp.count > 0
            }
            val completedDateSet = validCompletions.map { it.dateKey }.toSet()
            val totalCompletions = completedDateSet.size

            val totalCountSum: Long = if (task.isCounter) {
                taskComps.sumOf { it.count.toLong() }
            } else {
                totalCompletions.toLong()
            }

            val todayComp = taskComps.firstOrNull { it.dateKey == todayKey }
            val isTodayCompleted = if (task.isCounter) {
                (todayComp?.count ?: 0) >= task.targetCount
            } else {
                todayComp != null && todayComp.count > 0
            }

            val lastCompBeforeToday = validCompletions
                .map { it.dateKey }
                .filter { it < todayKey }
                .maxOrNull()

            val currentWeekDays = DateUtils.getDaysInSameWeek(todayKey)
            val weeklyCompletedCountThisWeek = validCompletions.count { currentWeekDays.contains(it.dateKey) }

            val isTodayDue = DateUtils.isTaskDueOnDate(
                frequencyType = task.frequencyType,
                daysOfWeek = task.daysOfWeek,
                targetDaysPerWeek = task.targetDaysPerWeek,
                dateKey = todayKey,
                weeklyCompletedCount = weeklyCompletedCountThisWeek,
                isCompletedOnDate = isTodayCompleted,
                intervalDays = task.intervalDays,
                intervalAnchorDate = task.intervalAnchorDate,
                lastCompletionDateKey = lastCompBeforeToday,
                createdAtMillis = task.createdAt
            )

            val monthlyCompletedDays = monthDates.count { completedDateSet.contains(it) }
            val monthlyDueDays = monthDates.count { dKey ->
                val dWeekDays = DateUtils.getDaysInSameWeek(dKey)
                val dWeekCount = validCompletions.count { dWeekDays.contains(it.dateKey) }
                DateUtils.isTaskDueOnDate(
                    frequencyType = task.frequencyType,
                    daysOfWeek = task.daysOfWeek,
                    targetDaysPerWeek = task.targetDaysPerWeek,
                    dateKey = dKey,
                    weeklyCompletedCount = dWeekCount,
                    isCompletedOnDate = completedDateSet.contains(dKey),
                    createdAtMillis = task.createdAt
                )
            }.coerceAtLeast(1)

            val monthlyAdherencePercent = ((monthlyCompletedDays * 100) / monthlyDueDays).coerceIn(0, 100)

            val recent7 = recent7DateKeys.map { dKey ->
                val isDone = completedDateSet.contains(dKey)
                val isTod = dKey == todayKey
                val dWeekDays = DateUtils.getDaysInSameWeek(dKey)
                val dWeekCount = validCompletions.count { dWeekDays.contains(it.dateKey) }
                val due = DateUtils.isTaskDueOnDate(
                    frequencyType = task.frequencyType,
                    daysOfWeek = task.daysOfWeek,
                    targetDaysPerWeek = task.targetDaysPerWeek,
                    dateKey = dKey,
                    weeklyCompletedCount = dWeekCount,
                    isCompletedOnDate = isDone,
                    createdAtMillis = task.createdAt
                )
                val dayLabel = DateUtils.getDayOfWeek(dKey, AppLanguage.DE)
                val dayNum = DateUtils.getDayNumber(dKey)
                HabitDayStatus(
                    dateKey = dKey,
                    dayLabel = dayLabel,
                    dayNumber = dayNum,
                    isCompleted = isDone,
                    isToday = isTod,
                    isDue = due
                )
            }

            val (currentStreak, longestStreak) = calculateHabitStreak(task, completedDateSet, todayKey)

            val computedStats = HabitStats(
                task = task,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalCompletions = totalCompletions,
                totalCountSum = totalCountSum,
                monthlyAdherencePercent = monthlyAdherencePercent,
                monthlyCompletedDays = monthlyCompletedDays,
                monthlyDueDays = monthlyDueDays,
                isTodayCompleted = isTodayCompleted,
                isTodayDue = isTodayDue,
                completedDateSet = completedDateSet,
                recent7Days = recent7
            )
            habitStatsCache[task.id] = Pair(HabitStatsCacheKey(task, taskComps, todayKey), computedStats)
            computedStats
        }
    }

    private fun calculateHabitStreak(
        task: DailyTask,
        completedDates: Set<String>,
        todayKey: String
    ): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        when (task.frequencyType) {
            "WEEKDAYS" -> {
                val allowedDays = DateUtils.parseWeekdaySet(task.daysOfWeek)
                if (allowedDays.isEmpty()) return calculateDailyHabitStreak(completedDates, todayKey)

                var curStreak = 0
                val checkCal = Calendar.getInstance()
                for (i in 0..365) {
                    val key = format.format(checkCal.time)
                    val isoDay = DateUtils.getIsoDayOfWeek(key)
                    if (allowedDays.contains(isoDay)) {
                        val isDone = completedDates.contains(key)
                        if (key == todayKey && !isDone) {
                        } else if (isDone) {
                            curStreak++
                        } else {
                            break
                        }
                    }
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                }

                val sorted = completedDates.sorted()
                var longest = 0
                if (sorted.isNotEmpty()) {
                    val startCal = Calendar.getInstance().apply {
                        time = format.parse(sorted.first()) ?: return Pair(curStreak, curStreak)
                    }
                    val endCal = Calendar.getInstance()
                    var temp = 0
                    while (!startCal.after(endCal)) {
                        val key = format.format(startCal.time)
                        val isoDay = DateUtils.getIsoDayOfWeek(key)
                        if (allowedDays.contains(isoDay)) {
                            if (completedDates.contains(key)) {
                                temp++
                                if (temp > longest) longest = temp
                            } else {
                                temp = 0
                            }
                        }
                        startCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                return Pair(curStreak, maxOf(longest, curStreak))
            }
            "WEEKLY" -> {
                return calculateWeeklyHabitStreak(task, completedDates)
            }
            "INTERVAL" -> {
                return calculateIntervalHabitStreak(task, completedDates, todayKey)
            }
            else -> {
                return calculateDailyHabitStreak(completedDates, todayKey)
            }
        }
    }

    private fun calculateWeeklyHabitStreak(
        task: DailyTask,
        completedDates: Set<String>
    ): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calForDate = Calendar.getInstance()
        val completionsByWeek = mutableMapOf<String, Int>()

        for (dStr in completedDates) {
            try {
                calForDate.time = format.parse(dStr) ?: continue
                val year = calForDate.get(Calendar.YEAR)
                val week = calForDate.get(Calendar.WEEK_OF_YEAR)
                val key = "$year-W$week"
                completionsByWeek[key] = (completionsByWeek[key] ?: 0) + 1
            } catch (_: Exception) {}
        }

        val todayKey = DateUtils.getTodayKey()
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val curWeekKey = "$curYear-W$curWeek"
        val curWeekComps = completionsByWeek[curWeekKey] ?: 0
        val curWeekTarget = DateUtils.getEffectiveWeeklyTarget(task.targetDaysPerWeek, task.createdAt, todayKey)

        var currentStreak = 0
        val checkCal = Calendar.getInstance()

        if (curWeekComps >= curWeekTarget) {
            currentStreak = 1
            checkCal.add(Calendar.WEEK_OF_YEAR, -1)
        } else {
            checkCal.add(Calendar.WEEK_OF_YEAR, -1)
        }

        while (true) {
            val y = checkCal.get(Calendar.YEAR)
            val w = checkCal.get(Calendar.WEEK_OF_YEAR)
            val weekKey = "$y-W$w"
            val sampleDate = format.format(checkCal.time)
            if (!DateUtils.wasTaskCreatedOnOrBefore(task.createdAt, sampleDate)) {
                break
            }
            val count = completionsByWeek[weekKey] ?: 0
            val weekTarget = DateUtils.getEffectiveWeeklyTarget(task.targetDaysPerWeek, task.createdAt, sampleDate)
            if (count >= weekTarget) {
                currentStreak++
                checkCal.add(Calendar.WEEK_OF_YEAR, -1)
            } else {
                break
            }
        }

        return Pair(currentStreak, currentStreak)
    }

    private fun calculateIntervalHabitStreak(
        task: DailyTask,
        completedDates: Set<String>,
        todayKey: String
    ): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)
        val sortedDates = completedDates.sorted()
        val lastDate = sortedDates.last()
        val daysSinceLast = DateUtils.daysBetween(lastDate, todayKey)
        val safeInterval = task.intervalDays.coerceAtLeast(1)

        var currentStreak = 0
        if (daysSinceLast <= safeInterval) {
            currentStreak = 1
            for (i in sortedDates.size - 1 downTo 1) {
                val curr = sortedDates[i]
                val prev = sortedDates[i - 1]
                val diff = DateUtils.daysBetween(prev, curr)
                if (diff <= safeInterval + 1) {
                    currentStreak++
                } else {
                    break
                }
            }
        }

        var longest = currentStreak
        var temp = 1
        for (i in 1 until sortedDates.size) {
            val diff = DateUtils.daysBetween(sortedDates[i - 1], sortedDates[i])
            if (diff <= safeInterval + 1) {
                temp++
                if (temp > longest) longest = temp
            } else {
                temp = 1
            }
        }

        return Pair(currentStreak, maxOf(longest, currentStreak))
    }

    private fun calculateDailyHabitStreak(
        completedDates: Set<String>,
        todayKey: String
    ): Pair<Int, Int> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        var checkKey = format.format(cal.time)
        var currentStreak = 0

        if (!completedDates.contains(checkKey)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = format.format(cal.time)
            if (completedDates.contains(checkKey)) {
                while (completedDates.contains(checkKey)) {
                    currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    checkKey = format.format(cal.time)
                }
            }
        } else {
            while (completedDates.contains(checkKey)) {
                currentStreak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                checkKey = format.format(cal.time)
            }
        }

        val sortedDates = completedDates.distinct().sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevCal: Calendar? = null

        for (dStr in sortedDates) {
            val currentD = Calendar.getInstance().apply {
                time = format.parse(dStr) ?: return@apply
            }
            if (prevCal == null) {
                tempStreak = 1
            } else {
                val diffDays = ((currentD.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    tempStreak++
                } else if (diffDays > 1) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevCal = currentD
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
        }

        return Pair(currentStreak, longestStreak)
    }

    companion object {
        fun normalizeCategory(category: String): String = when (category.trim().lowercase()) {
            "gesundheit", "health" -> "health"
            "sport", "workout" -> "workout"
            "fokus", "focus" -> "focus"
            "supplements", "supplemente" -> "supplements"
            "routine", "routinen", "routines" -> "routine"
            else -> category.trim().lowercase()
        }

        fun isCategoryEquivalent(categoryA: String?, categoryB: String?): Boolean {
            if (categoryA == null || categoryB == null) return categoryA == categoryB
            if (categoryA.equals(categoryB, ignoreCase = true)) return true
            return normalizeCategory(categoryA) == normalizeCategory(categoryB)
        }
    }
}

""
}],