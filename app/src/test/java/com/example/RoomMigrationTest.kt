package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyDatabase
import com.example.data.DailyTask
import com.example.data.StreakFreeze
import com.example.data.TaskCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    @Test
    fun testDatabaseSingletonAndSchemaValidation() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val context = ApplicationProvider.getApplicationContext<Context>()

                // 1. Initialize singleton database instance
                val db = DailyDatabase.getInstance(context)
                assertNotNull(db)
                val dao = db.dailyTaskDao()
                assertNotNull(dao)

                // 2. Perform insert and query operations on active schema
                val taskId = dao.insertTask(
                    DailyTask(
                        title = "Test Hydration",
                        category = "Health",
                        isCounter = true,
                        targetCount = 8,
                        unit = "Glasses",
                        durationMinutes = 15
                    )
                )
                assertTrue(taskId > 0)

                val loadedTask = dao.getTaskById(taskId)
                assertNotNull(loadedTask)
                assertEquals("Test Hydration", loadedTask?.title)
                assertEquals(8, loadedTask?.targetCount)
                assertEquals(15, loadedTask?.durationMinutes)

                // 3. Perform completion transaction
                val completionId = dao.insertCompletion(
                    TaskCompletion(taskId = taskId, dateKey = "2026-10-15", count = 8)
                )
                assertTrue(completionId > 0)

                val completion = dao.getCompletion(taskId, "2026-10-15")
                assertNotNull(completion)
                assertEquals(8, completion?.count)

                // 4. Test StreakFreeze operations
                dao.getAllStreakFreezesSync().forEach { dao.deleteStreakFreezeById(it.id) }

                val freezeId = dao.insertStreakFreeze(
                    StreakFreeze(
                        startDate = "2026-10-10",
                        endDate = "2026-10-12",
                        type = "SICKNESS",
                        note = "Flu recovery"
                    )
                )
                assertTrue(freezeId > 0)

                val freezes = dao.getAllStreakFreezesSync()
                assertEquals(1, freezes.size)
                assertEquals("SICKNESS", freezes[0].type)

                // Cleanup
                dao.deleteStreakFreezeById(freezeId)
                dao.deleteTask(loadedTask!!)
            }
        }
    }
}
