package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyDatabase
import com.example.data.DailyTask
import com.example.data.TaskCompletion
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: DailyDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DailyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Daily", appName)
    }

    @Test
    fun testDailyTaskPersistenceAndDayReset() = runBlocking {
        val dao = db.dailyTaskDao()

        // 1. Insert daily task (e.g. Vitamin D3)
        val taskId = dao.insertTask(
            DailyTask(
                title = "Vitamin D3",
                category = "Supplements",
                reminderHour = 8,
                reminderMinute = 30,
                reminderEnabled = true
            )
        )

        // 2. Mark completed for today
        val todayKey = "2026-09-28"
        dao.insertCompletion(TaskCompletion(taskId = taskId, dateKey = todayKey))

        // Verify it is completed today
        val todayCompletion = dao.getCompletion(taskId, todayKey)
        assertNotNull(todayCompletion)

        // 3. Check tomorrow ("2026-09-29") - should be open (reset) without deleting today's completion!
        val tomorrowKey = "2026-09-29"
        val tomorrowCompletion = dao.getCompletion(taskId, tomorrowKey)
        assertNull(tomorrowCompletion)

        // Verify yesterday/today's completion is still intact for history/streaks
        val historyCompletion = dao.getCompletion(taskId, todayKey)
        assertNotNull(historyCompletion)

        // Test distinct dates query for streaks
        val allDates = dao.getAllCompletionDatesSync()
        assertEquals(1, allDates.size)
        assertTrue(allDates.contains("2026-09-28"))

        // Test fully completed dates: With 1 task and 1 completion, today is fully completed
        val fullyDone1 = dao.getFullyCompletedDatesSync()
        assertEquals(1, fullyDone1.size)
        assertEquals("2026-09-28", fullyDone1[0])

        // Add a second task that is NOT completed today
        val task2Id = dao.insertTask(
            DailyTask(title = "Wasser trinken", category = "Gesundheit")
        )
        // Now total active tasks = 2, but only 1 is completed today:
        val fullyDone2 = dao.getFullyCompletedDatesSync()
        // Should be empty because not all tasks were completed!
        assertEquals(0, fullyDone2.size)

        // Now complete the second task as well
        dao.insertCompletion(TaskCompletion(taskId = task2Id, dateKey = todayKey))
        val fullyDone3 = dao.getFullyCompletedDatesSync()
        // Now all 2 tasks are completed, so today qualifies for the streak!
        assertEquals(1, fullyDone3.size)
        assertEquals("2026-09-28", fullyDone3[0])
    }

    @Test
    fun testCounterTaskBehaviorAndZeroClamp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.DailyRepository(db.dailyTaskDao(), context)
        val todayKey = "2026-09-28"

        // 1. Create counter task (target 100 pushups)
        val taskId = repo.insertTask(
            DailyTask(
                title = "100 Liegestütze",
                category = "Sport",
                isCounter = true,
                targetCount = 100,
                unit = "Wdh."
            )
        )

        // 2. Initial state: count is 0
        val initialComp = db.dailyTaskDao().getCompletion(taskId, todayKey)
        assertNull(initialComp)

        // 3. Subtracting 10 when count is 0: MUST STAY 0, NOT COMPLETE OR OVERFLOW!
        val countAfterSub = repo.incrementTaskCounter(taskId, todayKey, -10)
        assertEquals(0, countAfterSub)
        val compAfterSub = db.dailyTaskDao().getCompletion(taskId, todayKey)
        assertNull(compAfterSub)

        // 4. Increment by 10
        val count1 = repo.incrementTaskCounter(taskId, todayKey, 10)
        assertEquals(10, count1)
        val comp1 = db.dailyTaskDao().getCompletion(taskId, todayKey)
        assertNotNull(comp1)
        assertEquals(10, comp1!!.count)

        // 5. Increment to full target (add 90)
        val count2 = repo.incrementTaskCounter(taskId, todayKey, 90)
        assertEquals(100, count2)

        // 6. Decrement by 10 back to 90
        val count3 = repo.incrementTaskCounter(taskId, todayKey, -10)
        assertEquals(90, count3)

        // 7. Decrement all the way down to 0
        val count4 = repo.incrementTaskCounter(taskId, todayKey, -90)
        assertEquals(0, count4)

        // 8. Subtracting again when 0 must stay 0
        val count5 = repo.incrementTaskCounter(taskId, todayKey, -10)
        assertEquals(0, count5)
    }

    @Test
    fun testStreakWidgetLayoutInflation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_streak_layout)
        val view = views.apply(context, null)
        assertNotNull(view)
        val countTv = view.findViewById<android.widget.TextView>(R.id.widget_streak_count)
        val topTv = view.findViewById<android.widget.TextView>(R.id.widget_streak_label_top)
        val bottomTv = view.findViewById<android.widget.TextView>(R.id.widget_streak_label_bottom)
        assertNotNull(countTv)
        assertNotNull(topTv)
        assertNotNull(bottomTv)

        // Test compact (1x1, 1x2) layout inflation
        val compactViews = android.widget.RemoteViews(context.packageName, R.layout.widget_streak_compact_layout)
        val compactView = compactViews.apply(context, null)
        assertNotNull(compactView)
        val compactCountTv = compactView.findViewById<android.widget.TextView>(R.id.widget_streak_count)
        assertNotNull(compactCountTv)

        // Test 2x2 layout inflation
        val views2x2 = android.widget.RemoteViews(context.packageName, R.layout.widget_streak_2x2_layout)
        val view2x2 = views2x2.apply(context, null)
        assertNotNull(view2x2)
        val count2x2 = view2x2.findViewById<android.widget.TextView>(R.id.widget_streak_count)
        val top2x2 = view2x2.findViewById<android.widget.TextView>(R.id.widget_streak_label_top)
        val bottom2x2 = view2x2.findViewById<android.widget.TextView>(R.id.widget_streak_label_bottom)
        assertNotNull(count2x2)
        assertNotNull(top2x2)
        assertNotNull(bottom2x2)
    }

    @Test
    fun testRoutinesResetInfoDismissal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appSettings = com.example.util.AppSettings.getInstance(context)

        // Initially not dismissed
        val initialDismissed = appSettings.routinesResetInfoDismissed.value
        assertEquals(false, initialDismissed)

        // Dismiss it
        appSettings.dismissRoutinesResetInfo()
        assertEquals(true, appSettings.routinesResetInfoDismissed.value)

        // Re-read settings instance to ensure persistence
        val reloadedSettings = com.example.util.AppSettings.getInstance(context)
        assertEquals(true, reloadedSettings.routinesResetInfoDismissed.value)
    }

    @Test
    fun testWeeklyAndWeekdayHabitPersistence() = runBlocking {
        val dao = db.dailyTaskDao()

        // 1. Weekly habit (e.g. 2x per week)
        val weeklyTaskId = dao.insertTask(
            DailyTask(
                title = "Sauna & Regeneration",
                category = "Gesundheit",
                frequencyType = "WEEKLY",
                targetDaysPerWeek = 2
            )
        )
        val loadedWeekly = dao.getTaskById(weeklyTaskId)
        assertNotNull(loadedWeekly)
        assertEquals("WEEKLY", loadedWeekly!!.frequencyType)
        assertEquals(2, loadedWeekly.targetDaysPerWeek)

        // 2. Specific weekdays habit (e.g. Monday, Wednesday, Friday: "1,3,5")
        val weekdayTaskId = dao.insertTask(
            DailyTask(
                title = "Gym Workout",
                category = "Sport",
                frequencyType = "WEEKDAYS",
                daysOfWeek = "1,3,5"
            )
        )
        val loadedWeekday = dao.getTaskById(weekdayTaskId)
        assertNotNull(loadedWeekday)
        assertEquals("WEEKDAYS", loadedWeekday!!.frequencyType)
        assertEquals("1,3,5", loadedWeekday.daysOfWeek)
    }

    @Test
    fun testDailyProgressWidgetLayoutInflation() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Compact (1x1)
        val compactViews = android.widget.RemoteViews(context.packageName, R.layout.widget_progress_compact_layout)
        val compactView = compactViews.apply(context, null)
        assertNotNull(compactView)
        val compactPercent = compactView.findViewById<android.widget.TextView>(R.id.widget_progress_compact_percent)
        val compactFraction = compactView.findViewById<android.widget.TextView>(R.id.widget_progress_compact_fraction)
        assertNotNull(compactPercent)
        assertNotNull(compactFraction)

        // 2. Wide (2x1)
        val wideViews = android.widget.RemoteViews(context.packageName, R.layout.widget_progress_wide_layout)
        val wideView = wideViews.apply(context, null)
        assertNotNull(wideView)
        val widePercent = wideView.findViewById<android.widget.TextView>(R.id.widget_progress_wide_percent)
        val wideText = wideView.findViewById<android.widget.TextView>(R.id.widget_progress_wide_text)
        val wideBar = wideView.findViewById<android.widget.ImageView>(R.id.widget_progress_wide_bar)
        assertNotNull(widePercent)
        assertNotNull(wideText)
        assertNotNull(wideBar)

        // 3. Large (2x2)
        val largeViews = android.widget.RemoteViews(context.packageName, R.layout.widget_progress_large_layout)
        val largeView = largeViews.apply(context, null)
        assertNotNull(largeView)
        val largeGauge = largeView.findViewById<android.widget.ImageView>(R.id.widget_progress_large_gauge)
        val largePercent = largeView.findViewById<android.widget.TextView>(R.id.widget_progress_large_percent)
        val largeStatus = largeView.findViewById<android.widget.TextView>(R.id.widget_progress_large_status)
        assertNotNull(largeGauge)
        assertNotNull(largePercent)
        assertNotNull(largeStatus)
    }

    @Test
    fun testWidgetBitmapUtils() {
        val circleBmp = com.example.widget.WidgetBitmapUtils.createColoredCircleBitmap(24, android.graphics.Color.RED)
        assertNotNull(circleBmp)
        assertEquals(24, circleBmp.width)
        assertEquals(24, circleBmp.height)

        val gaugeBmp = com.example.widget.WidgetBitmapUtils.createCircularProgressBitmap(100, 0.75f, 8f, android.graphics.Color.RED)
        assertNotNull(gaugeBmp)
        assertEquals(100, gaugeBmp.width)
        assertEquals(100, gaugeBmp.height)

        val barBmp = com.example.widget.WidgetBitmapUtils.createProgressBarBitmap(200, 16, 0.5f, android.graphics.Color.RED)
        assertNotNull(barBmp)
        assertEquals(200, barBmp.width)
        assertEquals(16, barBmp.height)
    }

    @Test
    fun testSnoozeSchedulingAndCancellation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Verify scheduleTaskSnooze and cancelTaskSnooze execute cleanly without throwing
        com.example.reminder.ReminderManager.scheduleTaskSnooze(context, taskId = 42L, minutes = 10)
        com.example.reminder.ReminderManager.cancelTaskSnooze(context, taskId = 42L)

        com.example.reminder.ReminderManager.scheduleGeneralSnooze(context, minutes = 30)
        com.example.reminder.ReminderManager.cancelGeneralSnooze(context)
    }

    @Test
    fun testVibrationSettingsAndHaptics() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appSettings = com.example.util.AppSettings.getInstance(context)

        // Vibration is off by default
        assertEquals(false, appSettings.vibrationFeedbackEnabled.value)

        // Enable vibration
        appSettings.setVibrationFeedbackEnabled(true)
        assertEquals(true, appSettings.vibrationFeedbackEnabled.value)

        // Completion animation is enabled by default
        assertEquals(true, appSettings.completionAnimationEnabled.value)

        // Test safe execution of HapticUtils
        com.example.util.HapticUtils.vibrateTick(context)
        com.example.util.HapticUtils.vibrateSuccess(context)
    }

    @Test
    fun testNewTaskCreatedTodayDoesNotBreakPastStreak() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.DailyRepository(db.dailyTaskDao(), context)
        val yesterday = "2026-09-28"
        val today = com.example.util.DateUtils.getTodayKey()

        // 1. Task created yesterday
        val task1Id = repo.insertTask(
            DailyTask(title = "Morning Run", category = "Sport", createdAt = 0L)
        )
        db.dailyTaskDao().insertCompletion(
            TaskCompletion(taskId = task1Id, dateKey = yesterday, count = 1)
        )

        // Yesterday was fully completed
        val fullyDoneBefore = repo.getFullyCompletedDatesSync()
        assertTrue(fullyDoneBefore.contains(yesterday))

        // 2. Today, a brand new task is created
        val task2Id = repo.insertTask(
            DailyTask(title = "Read 20 pages", category = "Fokus", createdAt = System.currentTimeMillis())
        )

        // 3. Yesterday's streak must NOT be broken!
        val fullyDoneAfter = repo.getFullyCompletedDatesSync()
        assertTrue(fullyDoneAfter.contains(yesterday))

        // 4. Querying tasks for yesterday should only return task1, not task2
        val yesterdayTasks = repo.getTasksWithCompletions(yesterday)
        val firstList = yesterdayTasks.first()
        assertEquals(1, firstList.size)
        assertEquals(task1Id, firstList[0].task.id)
    }

    @Test
    fun testPastDaysCannotBeModifiedInViewModel() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<DailyApplication>()
        val viewModel = com.example.ui.DailyViewModel(app)
        val yesterday = "2026-09-28"

        val taskId = app.repository.insertTask(
            DailyTask(title = "Meditation", category = "Fokus", createdAt = 0L)
        )

        // Select yesterday in ViewModel
        viewModel.selectDate(yesterday)

        // Attempt to toggle task on past day
        viewModel.toggleTask(taskId)
        viewModel.incrementCounter(taskId, 1)
        viewModel.setCounter(taskId, 5)

        // Verify that yesterday has NO completion recorded
        val comp = db.dailyTaskDao().getCompletion(taskId, yesterday)
        assertNull(comp)
    }

    @Test
    fun testIndividualHabitStatsCalculation() {
        val app = ApplicationProvider.getApplicationContext<DailyApplication>()
        val viewModel = com.example.ui.DailyViewModel(app)

        val habit1 = DailyTask(id = 101L, title = "Wasser 2L", category = "Gesundheit", isCounter = false)
        val habit2 = DailyTask(id = 102L, title = "Pushups", category = "Sport", isCounter = true, targetCount = 50, unit = "Reps")

        val completions = listOf(
            TaskCompletion(id = 1, taskId = 101L, dateKey = "2026-09-28", count = 1),
            TaskCompletion(id = 2, taskId = 101L, dateKey = "2026-09-29", count = 1),
            TaskCompletion(id = 3, taskId = 102L, dateKey = "2026-09-28", count = 50),
            TaskCompletion(id = 4, taskId = 102L, dateKey = "2026-09-29", count = 25)
        )

        val statsList = viewModel.calculateHabitStatsList(listOf(habit1, habit2), completions)

        // Verify habit1 stats
        val habit1Stats = statsList.firstOrNull { it.task.id == 101L }
        assertNotNull(habit1Stats)
        assertEquals(2, habit1Stats!!.totalCompletions)

        // Verify habit2 stats (only 1 full completion because count 25 < target 50)
        val habit2Stats = statsList.firstOrNull { it.task.id == 102L }
        assertNotNull(habit2Stats)
        assertEquals(1, habit2Stats!!.totalCompletions)
        assertEquals(75L, habit2Stats.totalCountSum)
    }

    @Test
    fun testCategoryEquivalenceAcrossLanguages() {
        assertTrue(com.example.ui.DailyViewModel.isCategoryEquivalent("Gesundheit", "Health"))
        assertTrue(com.example.ui.DailyViewModel.isCategoryEquivalent("Sport", "Workout"))
        assertTrue(com.example.ui.DailyViewModel.isCategoryEquivalent("Fokus", "Focus"))
        assertTrue(com.example.ui.DailyViewModel.isCategoryEquivalent("Supplements", "Supplements"))
        assertTrue(com.example.ui.DailyViewModel.isCategoryEquivalent("Routine", "Routines"))
    }
}
