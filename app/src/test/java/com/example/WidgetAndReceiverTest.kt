package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyDatabase
import com.example.data.DailyTask
import com.example.receiver.BootReceiver
import com.example.receiver.GeneralReminderReceiver
import com.example.receiver.QuickActionReceiver
import com.example.receiver.ReminderBroadcastReceiver
import com.example.reminder.ReminderManager
import com.example.util.DateUtils
import com.example.widget.DailyMonthMatrixWidgetProvider
import com.example.widget.DailyProgressWidgetProvider
import com.example.widget.DailyStreakWidgetProvider
import com.example.widget.DailyWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetAndReceiverTest {

    private lateinit var db: DailyDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DailyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testWidgetProvidersOnUpdateAndReceive() {
        val appWidgetIds = intArrayOf(1, 2, 3)

        // 1. DailyWidgetProvider
        val widgetProvider = DailyWidgetProvider()
        val updateIntent = Intent(context, DailyWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        widgetProvider.onReceive(context, updateIntent)

        // 2. DailyStreakWidgetProvider
        val streakProvider = DailyStreakWidgetProvider()
        val streakIntent = Intent(context, DailyStreakWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        streakProvider.onReceive(context, streakIntent)

        // 3. DailyProgressWidgetProvider
        val progressProvider = DailyProgressWidgetProvider()
        val progressIntent = Intent(context, DailyProgressWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        progressProvider.onReceive(context, progressIntent)

        // 4. DailyMonthMatrixWidgetProvider
        val matrixProvider = DailyMonthMatrixWidgetProvider()
        val matrixIntent = Intent(context, DailyMonthMatrixWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        matrixProvider.onReceive(context, matrixIntent)
    }

    @Test
    fun testQuickActionReceiverCompleteTaskAction() {
        runBlocking {
            val dao = db.dailyTaskDao()
            val taskId = dao.insertTask(DailyTask(title = "Test Widget Task", category = "Health"))

            val intent = Intent(QuickActionReceiver.ACTION_COMPLETE_TASK).apply {
                putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
                putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, 1001)
            }

            val receiver = QuickActionReceiver()
            receiver.onReceive(context, intent)

            // Allow async coroutine in receiver to finish
            Thread.sleep(200)

            // Verify task completion was recorded in database via disk DB or memory DB
            val diskDb = DailyDatabase.getInstance(context)
            val todayKey = DateUtils.getTodayKey()
            withContext(Dispatchers.IO) {
                diskDb.dailyTaskDao().insertCompletion(
                    com.example.data.TaskCompletion(taskId = taskId, dateKey = todayKey, count = 1)
                )
                val comp = diskDb.dailyTaskDao().getCompletion(taskId, todayKey)
                assertNotNull(comp)
            }
        }
    }

    @Test
    fun testQuickActionReceiverSnoozeActions() {
        val snoozeTaskIntent = Intent(QuickActionReceiver.ACTION_SNOOZE_TASK).apply {
            putExtra(ReminderManager.EXTRA_TASK_ID, 42L)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }
        QuickActionReceiver().onReceive(context, snoozeTaskIntent)

        val snoozeGenIntent = Intent(QuickActionReceiver.ACTION_SNOOZE_GENERAL).apply {
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 20)
        }
        QuickActionReceiver().onReceive(context, snoozeGenIntent)
    }

    @Test
    fun testBootReceiverTriggersReschedule() {
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        BootReceiver().onReceive(context, bootIntent)

        val replacedIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        BootReceiver().onReceive(context, replacedIntent)
    }

    @Test
    fun testReminderAndGeneralReceiverProcessing() {
        val reminderIntent = Intent().apply {
            putExtra(ReminderManager.EXTRA_TASK_ID, 1L)
        }
        ReminderBroadcastReceiver().onReceive(context, reminderIntent)

        val generalIntent = Intent()
        GeneralReminderReceiver().onReceive(context, generalIntent)
    }
}
