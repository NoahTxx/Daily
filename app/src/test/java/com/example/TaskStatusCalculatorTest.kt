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
}
