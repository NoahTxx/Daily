package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyTask
import com.example.data.StreakFreeze
import com.example.ui.DailyViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyViewModelIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: DailyApplication
    private lateinit var viewModel: DailyViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        viewModel = DailyViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelSaveTaskAndToggleStateFlows() = runBlocking {
        withContext(Dispatchers.IO) {
            val repo = app.repository
            val taskId = repo.insertTask(DailyTask(title = "Morning Meditation", category = "Focus"))
            assertTrue(taskId > 0)

            val loadedTask = repo.getTaskByIdSync(taskId)
            assertNotNull(loadedTask)
            assertEquals("Morning Meditation", loadedTask?.title)

            val todayKey = DateUtils.getTodayKey()
            repo.toggleTaskCompletion(taskId, todayKey)

            val tasksWithComp = repo.getTasksWithCompletionsSync(todayKey)
            val item = tasksWithComp.firstOrNull { (t) -> t.id == taskId }
            assertNotNull(item)
            assertTrue(item!!.isCompleted)
        }
    }

    @Test
    fun testViewModelCategorySelectionAndDateNavigation() = runBlocking {
        viewModel.selectCategory("Health")
        assertEquals("Health", viewModel.selectedCategory.value)

        val todayKey = DateUtils.getTodayKey()
        val yesterday = DateUtils.addDays(todayKey, -1)

        viewModel.selectDate(yesterday)
        assertEquals(yesterday, viewModel.selectedDateKey.value)
    }

    @Test
    fun testBackupExportAndImportStateRestoration() = runBlocking {
        withContext(Dispatchers.IO) {
            val repo = app.repository
            repo.insertTask(DailyTask(title = "Read 10 pages", category = "Focus"))
            repo.insertStreakFreeze(
                StreakFreeze(startDate = "2026-11-10", endDate = "2026-11-12", type = "VACATION", note = "Trip")
            )

            // Export backup
            val json = repo.exportBackupJson()
            assertTrue(json.contains("Read 10 pages"))
            assertTrue(json.contains("VACATION"))

            // Import backup
            val success = repo.importBackupJson(json)
            assertTrue(success)

            val tasksAfter = app.database.dailyTaskDao().getAllTasksSync()
            assertTrue(tasksAfter.any { it.title == "Read 10 pages" })
        }
    }
}
