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
        val weekCompletions = completions // week contains only the daily

        val dailyStatus = TaskStatusCalculator.evaluate(daily, monday, todayCompletions, weekCompletions, completions)
        val weeklyStatus = TaskStatusCalculator.evaluate(weekly, monday, todayCompletions, weekCompletions, completions)

        // Daily should be completed
        assertTrue(dailyStatus.isCompleted)
        // Weekly (1/week) on Monday with 7 days left should NOT be mandatory/due
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

        assertTrue("Weekly task should be due on mandatory day", status.isMandatoryToday || status.isDueToday)
    }

    @Test
    fun testIntervalDaysUntilDue() {
        val anchor = "2026-10-01"
        val query = "2026-10-04" // 3 days after anchor
        val intervalTask = DailyTask(id = 4L, title = "Every 5 days", frequencyType = "INTERVAL", intervalDays = 5, intervalAnchorDate = anchor, createdAt = 0L)

        val completions: List<TaskCompletion> = emptyList()
        val todayCompletions = completions.filter { it.dateKey == query }
        val weekCompletions = completions

        val status = TaskStatusCalculator.evaluate(intervalTask, query, todayCompletions, weekCompletions, completions)

        // Days until due should be 2 (5 - 3)
        assertEquals(2, status.daysUntilDue)
    }
}
