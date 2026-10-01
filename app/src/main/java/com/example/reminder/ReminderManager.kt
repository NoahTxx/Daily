package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DailyDatabase
import com.example.data.DailyTask
import com.example.receiver.QuickActionReceiver
import com.example.receiver.ReminderBroadcastReceiver
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object ReminderManager {

    const val CHANNEL_ID = "daily_reminders_channel"
    const val EXTRA_TASK_ID = "extra_task_id"

    private const val GENERAL_REMINDER_REQUEST_CODE = 99902
    private const val GENERAL_NOTIFICATION_ID = 99901
    private const val TEST_NOTIFICATION_ID = 99903
    private const val GENERAL_SNOOZE_REQUEST_CODE = 99950
    private const val TEST_SNOOZE_REQUEST_CODE = 99951
    private const val TASK_SNOOZE_REQUEST_CODE_OFFSET = 500000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appSettings = AppSettings.getInstance(context)
            val isEn = appSettings.selectedLanguage.value == AppLanguage.EN
            val name = if (isEn) "Daily Reminders" else context.getString(R.string.notification_channel_name)
            val descriptionText = if (isEn) "Reminds you about pending habits" else context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, task: DailyTask) {
        if (!task.reminderEnabled || task.reminderHour == null || task.reminderMinute == null) {
            cancelReminder(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.reminderHour)
            set(Calendar.MINUTE, task.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        cancelTaskSnooze(context, taskId)
    }

    suspend fun rescheduleAllReminders(context: Context) = withContext(Dispatchers.IO) {
        val db = DailyDatabase.getInstance(context)
        val tasks = db.dailyTaskDao().getTasksWithReminders()
        tasks.forEach { task ->
            scheduleReminder(context, task)
        }
        val appSettings = AppSettings.getInstance(context)
        if (appSettings.generalReminderEnabled.value) {
            scheduleGeneralDailyReminder(
                context,
                appSettings.generalReminderHour.value,
                appSettings.generalReminderMinute.value
            )
        }
    }

    fun showReminderNotification(context: Context, task: DailyTask, isSnoozed: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val appSettings = AppSettings.getInstance(context)
        val language = appSettings.selectedLanguage.value
        val accentColor = appSettings.selectedAccent.value.color.toArgb()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Complete Task
        val completeIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, task.id.toInt())
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + 20000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10 Minutes
        val snooze10PendingIntent = createSnoozeTaskPendingIntent(
            context = context,
            taskId = task.id,
            minutes = 10,
            notificationId = task.id.toInt(),
            requestCode = task.id.toInt() + 30000
        )

        // Action 3: Snooze 30 Minutes
        val snooze30PendingIntent = createSnoozeTaskPendingIntent(
            context = context,
            taskId = task.id,
            minutes = 30,
            notificationId = task.id.toInt(),
            requestCode = task.id.toInt() + 40000
        )

        // Action 4: Snooze 60 Minutes
        val snooze60PendingIntent = createSnoozeTaskPendingIntent(
            context = context,
            taskId = task.id,
            minutes = 60,
            notificationId = task.id.toInt(),
            requestCode = task.id.toInt() + 50000
        )

        val rawTitle = AppStrings.get("notif_task_reminder_title", language)
        val title = rawTitle
        val baseBody = String.format(AppStrings.get("notif_task_reminder_body", language), task.title)
        val body = if (isSnoozed) {
            "$baseBody • ${AppStrings.get("notif_snoozed_indicator", language)}"
        } else {
            baseBody
        }

        val actionTextDone = AppStrings.get("notif_mark_done", language)
        val actionText10 = AppStrings.get("notif_snooze_10m", language)
        val actionText30 = AppStrings.get("notif_snooze_30m", language)
        val actionText60 = AppStrings.get("notif_snooze_60m", language)

        val largeAppIcon = getAppLargeIcon(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(task.category)
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .addAction(
                R.drawable.widget_checkbox_checked,
                actionTextDone,
                completePendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                actionText10,
                snooze10PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                actionText30,
                snooze30PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                actionText60,
                snooze60PendingIntent
            )

        if (largeAppIcon != null) {
            notificationBuilder.setLargeIcon(largeAppIcon)
        }

        val notification = notificationBuilder.build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(task.id.toInt(), notification)
    }

    fun showGeneralReminderNotification(
        context: Context,
        incompleteCount: Int,
        totalCount: Int,
        isSnoozed: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val appSettings = AppSettings.getInstance(context)
        val language = appSettings.selectedLanguage.value
        val accentColor = appSettings.selectedAccent.value.color.toArgb()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            GENERAL_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze actions (10 / 30 / 60 min)
        val snooze10Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_GENERAL
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, GENERAL_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            99910,
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_GENERAL
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, GENERAL_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            99930,
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze60Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_GENERAL
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, GENERAL_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 60)
        }
        val snooze60PendingIntent = PendingIntent.getBroadcast(
            context,
            99960,
            snooze60Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rawTitle = AppStrings.get("notif_general_title", language)
        val title = rawTitle
        val baseMessage = String.format(AppStrings.get("notif_general_body", language), incompleteCount, totalCount)
        val message = if (isSnoozed) {
            "$baseMessage • ${AppStrings.get("notif_snoozed_indicator", language)}"
        } else {
            baseMessage
        }

        val largeAppIcon = getAppLargeIcon(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_10m", language),
                snooze10PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_30m", language),
                snooze30PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_60m", language),
                snooze60PendingIntent
            )

        if (largeAppIcon != null) {
            notificationBuilder.setLargeIcon(largeAppIcon)
        }

        val notification = notificationBuilder.build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GENERAL_NOTIFICATION_ID, notification)
    }

    private fun createSnoozeTaskPendingIntent(
        context: Context,
        taskId: Long,
        minutes: Int,
        notificationId: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleTaskSnooze(context: Context, taskId: Long, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_FIRE_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val requestCode = TASK_SNOOZE_REQUEST_CODE_OFFSET + taskId.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskSnooze(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_FIRE_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val requestCode = TASK_SNOOZE_REQUEST_CODE_OFFSET + taskId.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleGeneralDailyReminder(context: Context, hour: Int, minute: Int) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, com.example.receiver.GeneralReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelGeneralDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, com.example.receiver.GeneralReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        cancelGeneralSnooze(context)
    }

    fun scheduleGeneralSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_FIRE_GENERAL_SNOOZE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelGeneralSnooze(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_FIRE_GENERAL_SNOOZE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GENERAL_SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTestSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_FIRE_TEST_SNOOZE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun triggerTestNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val appSettings = AppSettings.getInstance(context)
        val language = appSettings.selectedLanguage.value
        val accentColor = appSettings.selectedAccent.value.color.toArgb()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze actions for test notification
        val snooze10Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_TEST
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, TEST_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            99971,
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_TEST
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, TEST_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            99972,
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze60Intent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_SNOOZE_TEST
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, TEST_NOTIFICATION_ID)
            putExtra(QuickActionReceiver.EXTRA_SNOOZE_MINUTES, 60)
        }
        val snooze60PendingIntent = PendingIntent.getBroadcast(
            context,
            99973,
            snooze60Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = AppStrings.get("notif_test_title", language)
        val body = AppStrings.get("notif_test_body", language)

        val largeAppIcon = getAppLargeIcon(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_10m", language),
                snooze10PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_30m", language),
                snooze30PendingIntent
            )
            .addAction(
                R.drawable.widget_dot_gray,
                AppStrings.get("notif_snooze_60m", language),
                snooze60PendingIntent
            )

        if (largeAppIcon != null) {
            notificationBuilder.setLargeIcon(largeAppIcon)
        }

        val notification = notificationBuilder.build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun getAppLargeIcon(context: Context): android.graphics.Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.daily_nothing_icon)
        } catch (e: Exception) {
            null
        } ?: try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
            val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e2: Exception) {
            null
        }
    }
}
