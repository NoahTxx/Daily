package com.example

import com.example.data.DailyTask
import com.example.data.TaskCompletion
import com.example.data.TaskStatusCalculator
import org.junit.Assert.*
import org.junit.Test

class TaskStatusCalculatorTest {

    @Test
    fun testWeeklyHabitDoesNotBreakDailyStreakOnNonRequiredDays() {
        val monday = "2026-09-28"

        val daily = DailyTask(id = 1L, title = "Daily Vitamin", category = "Supplements", createdAt = 0L)
        val weekly = DailyTask(id = 2L, title = "Clean Room", category = "Routine", frequencyType = "WEEKLY", targetDaysPerWeek = 1, createdAt = 0L)

        val completions = listOf(
            TaskCompletion(taskId = daily.id, dateKey = monday, count = 1)
        )

        val todayCompletions = completions.filter { it.dateKey == monday }
        val weekCompletions = completions

        val dailyStatus = TaskStatusCalculator.evaluate(daily, monday, todayCompletions, weekCompletions, completions)
        val weeklyStatus = TaskStatusCalculator.evaluate(weekly, monday, todayCompletions, weekCompletions, completions)

        assertTrue(dailyStatus.isCompleted)
        assertFalse(weeklyStatus.isMandatoryToday)
        assertFalse(weeklyStatus.isDueToday)
    }

    @Test
    fun testWeeklyHabitDueOnlyOnMandatoryDays() {
        val fri = "2026-10-02"
        val weekly = DailyTask(id = 3L, title = "Weekly Goal", frequencyType = "WEEKLY", targetDaysPerWeek = 3, createdAt = 0L)

        val completions: List<TaskCompletion> = emptyList()
        val todayCompletions = completions.filter { it.dateKey == fri }
        val weekCompletions = completions

        val status = TaskStatusCalculator.evaluate(weekly, fri, todayCompletions, weekCompletions, completions)

        assertTrue(status.isMandatoryToday || status.isDueToday)
    }

    @Test
    fun testIntervalDaysUntilDue() {
        val anchor = "2026-10-01"
        val query = "2026-10-04"
        val intervalTask = DailyTask(id = 4L, title = "Every 5 days", frequencyType = "INTERVAL", intervalDays = 5, intervalAnchorDate = anchor, createdAt = 0L)

        val completions: List<TaskCompletion> = emptyList()
        val todayCompletions = completions.filter { it.dateKey == query }
        val weekCompletions = completions

        val status = TaskStatusCalculator.evaluate(intervalTask, query, todayCompletions, weekCompletions, completions)

        assertEquals(2, status.daysUntilDue)
    }

    @Test
    fun testCounterTaskSatisfiesCompletionThreshold() {
        val date = "2026-10-05"
        val task = DailyTask(id = 5L, title = "Water", isCounter = true, targetCount = 3, createdAt = 0L)

        val completions = listOf(TaskCompletion(taskId = task.id, dateKey = date, count = 3))
        val status = TaskStatusCalculator.evaluate(task, date, completions, completions, completions)

        assertTrue(status.isCompleted)
        assertEquals(3, status.count)
    }

    @Test
    fun testRestDayEvaluationForWeeklyAndIntervalHabits() {
        val tuesday = "2026-09-29"

        // 1. Weekday habit scheduled only on Monday/Wednesday (1, 3)
        val weekdayTask = DailyTask(id = 10L, title = "Gym", frequencyType = "WEEKDAYS", daysOfWeek = "1,3", createdAt = 0L)
        val weekdayStatus = TaskStatusCalculator.evaluate(weekdayTask, tuesday, emptyList(), emptyList(), emptyList())
        // Tuesday is not scheduled:
        assertFalse(weekdayStatus.isDueToday)
        assertFalse(weekdayStatus.isMandatoryToday)
        assertFalse(weekdayStatus.isCompleted)

        // 2. Weekly habit with goal already met (2/2 done this week)
        val weeklyTask = DailyTask(id = 11L, title = "Running", frequencyType = "WEEKLY", targetDaysPerWeek = 2, createdAt = 0L)
        val completions = listOf(
            TaskCompletion(taskId = 11L, dateKey = "2026-09-28", count = 1),
            TaskCompletion(taskId = 11L, dateKey = "2026-09-29", count = 1)
        )
        // Check Wednesday (2026-09-30): weekly goal met, not completed on Wednesday
        val wednesdayStatus = TaskStatusCalculator.evaluate(weeklyTask, "2026-09-30", emptyList(), completions, completions)
        assertTrue(wednesdayStatus.isWeeklyGoalMet)
        assertFalse(wednesdayStatus.isCompleted)
        assertFalse(wednesdayStatus.isDueToday)

        // 3. Interval habit every 4 days, done on 2026-09-28
        val intervalTask = DailyTask(id = 12L, title = "Deep clean", frequencyType = "INTERVAL", intervalDays = 4, intervalAnchorDate = "2026-09-28", createdAt = 0L)
        val intervalCompletions = listOf(TaskCompletion(taskId = 12L, dateKey = "2026-09-28", count = 1))
        // Check Tuesday (2026-09-29) - 1 day after completion, not due yet
        val intervalStatus = TaskStatusCalculator.evaluate(intervalTask, tuesday, emptyList(), intervalCompletions, intervalCompletions)
        assertFalse(intervalStatus.isDueToday)
        assertFalse(intervalStatus.isCompleted)
        assertTrue(intervalStatus.daysUntilDue > 0)
    }

    @Test
    fun testWidgetFilteringForTasksNotDueToday() {
        val tuesday = "2026-09-29"

        // 1. Task not due today: weekday task only scheduled on Mon/Fri (1, 5)
        val weekdayTask = DailyTask(id = 20L, title = "Yoga", frequencyType = "WEEKDAYS", daysOfWeek = "1,5", createdAt = 0L)
        val weekdayStatus = TaskStatusCalculator.evaluate(weekdayTask, tuesday, emptyList(), emptyList(), emptyList())
        // Must NOT appear in widget as due today
        assertFalse(weekdayStatus.isDueToday || weekdayStatus.isCompleted)

        // 2. Task not due today: interval task every 3 days, done yesterday
        val intervalTask = DailyTask(id = 21L, title = "Water Plants", frequencyType = "INTERVAL", intervalDays = 3, intervalAnchorDate = "2026-09-28", createdAt = 0L)
        val intervalCompletions = listOf(TaskCompletion(taskId = 21L, dateKey = "2026-09-28", count = 1))
        val intervalStatus = TaskStatusCalculator.evaluate(intervalTask, tuesday, emptyList(), intervalCompletions, intervalCompletions)
        // Must NOT appear in widget as due today
        assertFalse(intervalStatus.isDueToday || intervalStatus.isCompleted)

        // 3. Task not due today: weekly task 1x/week, not mandatory on Tuesday
        val weeklyTask = DailyTask(id = 22L, title = "Vacuum", frequencyType = "WEEKLY", targetDaysPerWeek = 1, createdAt = 0L)
        val weeklyStatus = TaskStatusCalculator.evaluate(weeklyTask, tuesday, emptyList(), emptyList(), emptyList())
        // User does not have to do it today -> must NOT appear as due in widget
        assertFalse(weeklyStatus.isDueToday || weeklyStatus.isCompleted)

        // 4. Task completed today: MUST appear in widget
        val completedTask = DailyTask(id = 23L, title = "Reading", frequencyType = "DAILY", createdAt = 0L)
        val todayCompletions = listOf(TaskCompletion(taskId = 23L, dateKey = tuesday, count = 1))
        val completedStatus = TaskStatusCalculator.evaluate(completedTask, tuesday, todayCompletions, todayCompletions, todayCompletions)
        assertTrue(completedStatus.isDueToday || completedStatus.isCompleted)
    }
}
