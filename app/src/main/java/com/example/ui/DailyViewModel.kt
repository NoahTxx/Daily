package com.example.ui

import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StreakStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDays: Int = 0,
    val isTodaySecured: Boolean = false
)

data class HabitDayStatus(
    val dateKey: String,
    val dayLabel: String,
    val dayNumber: String,
    val isCompleted: Boolean,
    val isToday: Boolean,
    val isDue: Boolean
)

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

@OptIn(ExperimentalCoroutinesApi::class)
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<DailyTask>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletions: StateFlow<List<TaskCompletion>> = repository.getAllCompletions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitStatsList: StateFlow<List<HabitStats>> = combine(
        allActiveTasks,
        allCompletions
    ) { tasks, completions ->
        calculateHabitStatsList(tasks, completions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksWithCompletions: StateFlow<List<TaskWithCompletion>> = _selectedDateKey
        .flatMapLatest { dateKey ->
            repository.getTasksWithCompletions(dateKey)
        }
        .combine(_selectedCategory) { list, category ->
            if (category == null) list
            else list.filter { isCategoryEquivalent(it.task.category, category) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletionDates: StateFlow<List<String>> = repository.getAllCompletionDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullyCompletedDates: StateFlow<List<String>> = repository.getFullyCompletedDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakStats: StateFlow<StreakStats> = fullyCompletedDates.map { dates ->
        calculateFullStreakStats(dates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakStats())

    val currentStreak: StateFlow<Int> = streakStats.map { it.currentStreak }
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

        // 1. Current streak calculation
        val cal = Calendar.getInstance()
        var checkKey = format.format(cal.time)
        var currentStreak = 0

        if (!dateSet.contains(checkKey)) {
            // Check yesterday
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

        // 2. Longest streak calculation across all historic completion dates
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

    internal fun calculateHabitStatsList(
        tasks: List<DailyTask>,
        completions: List<TaskCompletion>
    ): List<HabitStats> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = DateUtils.getTodayKey()
        val completionsByTask = completions.groupBy { it.taskId }

        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val monthDates = (1..daysInMonth).map { String.format(Locale.US, "%s-%02d", monthPrefix, it) }
        val recent7DateKeys = DateUtils.getPastDays(7)

        return tasks.map { task ->
            val taskComps = completionsByTask[task.id] ?: emptyList()

            // Completion valid if count >= targetCount for counter, or count > 0 for standard
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

            // Today status
            val todayComp = taskComps.firstOrNull { it.dateKey == todayKey }
            val isTodayCompleted = if (task.isCounter) {
                (todayComp?.count ?: 0) >= task.targetCount
            } else {
                todayComp != null && todayComp.count > 0
            }

            val isTodayDue = DateUtils.isTaskDueOnDate(
                frequencyType = task.frequencyType,
                daysOfWeek = task.daysOfWeek,
                targetDaysPerWeek = task.targetDaysPerWeek,
                dateKey = todayKey,
                weeklyCompletedCount = 0,
                isCompletedOnDate = isTodayCompleted
            )

            // Monthly stats
            val monthlyCompletedDays = monthDates.count { completedDateSet.contains(it) }
            val monthlyDueDays = monthDates.count { dKey ->
                DateUtils.isTaskDueOnDate(
                    frequencyType = task.frequencyType,
                    daysOfWeek = task.daysOfWeek,
                    targetDaysPerWeek = task.targetDaysPerWeek,
                    dateKey = dKey,
                    weeklyCompletedCount = 0,
                    isCompletedOnDate = completedDateSet.contains(dKey)
                )
            }.coerceAtLeast(1)

            val monthlyAdherencePercent = ((monthlyCompletedDays * 100) / monthlyDueDays).coerceIn(0, 100)

            // Recent 7 days (including today)
            val recent7 = recent7DateKeys.map { dKey ->
                val isDone = completedDateSet.contains(dKey)
                val isTod = dKey == todayKey
                val due = DateUtils.isTaskDueOnDate(
                    frequencyType = task.frequencyType,
                    daysOfWeek = task.daysOfWeek,
                    targetDaysPerWeek = task.targetDaysPerWeek,
                    dateKey = dKey,
                    weeklyCompletedCount = 0,
                    isCompletedOnDate = isDone
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

            // Calculate current streak and longest streak for this habit
            val (currentStreak, longestStreak) = calculateHabitStreak(task, completedDateSet, todayKey)

            HabitStats(
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
                val allowedDays = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                if (allowedDays.isEmpty()) return calculateDailyHabitStreak(completedDates, todayKey)

                // Current streak on scheduled weekdays
                var curStreak = 0
                val checkCal = Calendar.getInstance()
                for (i in 0..365) {
                    val key = format.format(checkCal.time)
                    val isoDay = DateUtils.getIsoDayOfWeek(key)
                    if (allowedDays.contains(isoDay)) {
                        val isDone = completedDates.contains(key)
                        if (key == todayKey && !isDone) {
                            // Today is due but not done yet, continue looking backwards
                        } else if (isDone) {
                            curStreak++
                        } else {
                            break
                        }
                    }
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Longest streak on scheduled weekdays
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
            else -> {
                return calculateDailyHabitStreak(completedDates, todayKey)
            }
        }
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
            // Check yesterday
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

        // Longest streak
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
