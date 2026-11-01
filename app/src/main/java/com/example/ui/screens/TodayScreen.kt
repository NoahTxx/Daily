package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.DailyTask
import com.example.data.TaskWithCompletion
import com.example.ui.DailyViewModel
import com.example.ui.components.CompletionCelebrationEffect
import com.example.ui.components.DotMatrixProgress
import com.example.ui.components.FocusTimerSheet
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingCheckbox
import com.example.ui.components.NothingHeader
import com.example.ui.components.NothingPill
import com.example.ui.components.NotificationPermissionCard
import com.example.ui.components.StreakFreezeDialog
import com.example.ui.components.TaskSectionHeader
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.util.HapticUtils
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily
import com.example.util.TaskIcons

@Composable
fun TodayScreen(
    viewModel: DailyViewModel,
    selectedDateKey: String,
    tasks: List<TaskWithCompletion>,
    selectedCategory: String?,
    currentStreak: Int,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (DailyTask) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") completionDates: List<String> = emptyList(),
    fullyCompletedDates: List<String> = emptyList(),
) {
    val context = LocalContext.current
    var showPermissionCard by remember {
        mutableStateOf(
            value = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ),
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        showPermissionCard = !isGranted
    }

    val activeFreeze by viewModel.activeFreezeToday.collectAsStateWithLifecycle()
    val pendingTimerTaskId by viewModel.pendingTimerTaskId.collectAsStateWithLifecycle()
    var activeTimerTask by remember { mutableStateOf<DailyTask?>(null) }
    var showFreezeDialog by remember { mutableStateOf(value = false) }

    LaunchedEffect(pendingTimerTaskId, tasks) {
        val targetId = pendingTimerTaskId
        if (targetId != null) {
            val matchingTask = tasks.firstOrNull { (task) -> task.id == targetId }?.task
            if (matchingTask != null) {
                activeTimerTask = matchingTask
                viewModel.clearPendingTimer()
            }
        }
    }

    val (openDueTasks, completedTasks, notDueTasks) = remember(tasks) {
        val open = mutableListOf<TaskWithCompletion>()
        val completed = mutableListOf<TaskWithCompletion>()
        val notDue = mutableListOf<TaskWithCompletion>()

        for (task in tasks) {
            val isDone = task.isCompleted || ((task.task.frequencyType == "WEEKLY") && task.isWeeklyGoalMet)
            if (isDone) {
                completed.add(task)
            } else if (task.isDueToday) {
                open.add(task)
            } else {
                notDue.add(task)
            }
        }

        // Sort: Tasks without time come first (ganz oben).
        // Tasks with reminder time follow in chronological order (e.g. 16:00 before 20:30).
        val timeComparator = compareBy<TaskWithCompletion> { (t) ->
            if (t.reminderEnabled && (t.reminderHour != null) && (t.reminderMinute != null)) {
                1
            } else {
                0
            }
        }.thenBy { (t) ->
            if (t.reminderEnabled && (t.reminderHour != null) && (t.reminderMinute != null)) {
                (t.reminderHour * 60) + t.reminderMinute
            } else {
                t.orderIndex
            }
        }.thenBy { (t) -> t.orderIndex }.thenBy { (t) -> t.id }

        Triple(
            open.sortedWith(timeComparator),
            completed.sortedWith(timeComparator),
            notDue.sortedWith(timeComparator),
        )
    }

    val completedCount = remember(completedTasks) { completedTasks.size }
    val requiredTodayTasks = remember(tasks) {
        val mandatory = ArrayList<TaskWithCompletion>(tasks.size)
        val extraCompleted = ArrayList<TaskWithCompletion>(tasks.size)
        for (i in tasks.indices) {
            val item = tasks[i]
            val isWeeklyMet = (item.task.frequencyType == "WEEKLY") && item.isWeeklyGoalMet
            val isDueAndNotMet = item.isDueToday && (!isWeeklyMet)
            val isMand = (item.task.frequencyType == "DAILY") || item.isMandatoryToday || isDueAndNotMet
            if (isMand) {
                mandatory.add(item)
            } else if (item.isCompleted || isWeeklyMet) {
                extraCompleted.add(item)
            }
        }
        mandatory + extraCompleted
    }
    val totalCount = remember(requiredTodayTasks) { requiredTodayTasks.size }
    val progressPct = remember(completedCount, totalCount) { if (totalCount > 0) ((completedCount * 100) / totalCount).coerceIn(0, 100) else 0 }

    val recentDays = remember(DateUtils.getTodayKey()) { DateUtils.getPastDays(7) }
    val isViewingToday = DateUtils.isToday(selectedDateKey)
    val fullyCompletedDatesSet = remember(fullyCompletedDates) { fullyCompletedDates.toSet() }

    val language = LocalAppLanguage.current
    val accentColor = LocalAccentColor.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current
    fun t(key: String): String = AppStrings.get(key, language)
    val isEn = language == AppLanguage.EN

    val categories = remember(isEn) {
        if (isEn) {
            listOf("ALL", "SUPPLEMENTS", "HEALTH", "FOCUS", "WORKOUT", "ROUTINE")
        } else {
            listOf("ALLE", "SUPPLEMENTS", "GESUNDHEIT", "FOKUS", "SPORT", "ROUTINE")
        }
    }

    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val vibrationEnabled by appSettings.vibrationFeedbackEnabled.collectAsStateWithLifecycle()
    val animationEnabled by appSettings.completionAnimationEnabled.collectAsStateWithLifecycle()

    var showCelebration by remember { mutableStateOf(value = false) }
    var userTriggeredAction by remember { mutableStateOf(value = false) }
    var previousCompletedCount by remember { mutableIntStateOf(completedCount) }

    // Track whether today is fully completed right now, to maintain responsive state
    // even when the user navigates to inspect past days in the date strip.
    var isTodayCompletedLocally by remember {
        mutableStateOf<Boolean?>(null)
    }

    if (isViewingToday && (selectedCategory == null) && (totalCount > 0)) {
        isTodayCompletedLocally = (completedCount >= totalCount)
    }

    // Reset user action flag whenever the selected date changes so that date navigation
    // never triggers the completion celebration
    LaunchedEffect(selectedDateKey) {
        userTriggeredAction = false
    }

    LaunchedEffect(completedCount, totalCount) {
        if (userTriggeredAction &&
            (totalCount > 0) &&
            (completedCount == totalCount) &&
            (previousCompletedCount < totalCount) &&
            isViewingToday &&
            (selectedCategory == null)
        ) {
            if (animationEnabled) {
                showCelebration = true
            }
            if (vibrationEnabled) {
                HapticUtils.vibrateSuccess(context)
            }
            userTriggeredAction = false
        } else if (completedCount < totalCount) {
            userTriggeredAction = false
        }
        previousCompletedCount = completedCount
    }

    LaunchedEffect(fullyCompletedDates) {
        val todayKey = DateUtils.getTodayKey()
        if (userTriggeredAction && isViewingToday && fullyCompletedDatesSet.contains(todayKey)) {
            if (animationEnabled) {
                showCelebration = true
            }
            if (vibrationEnabled) {
                HapticUtils.vibrateSuccess(context)
            }
            userTriggeredAction = false
        }
    }

    val currentTasksState = androidx.compose.runtime.rememberUpdatedState(tasks)
    val currentIsViewingToday = androidx.compose.runtime.rememberUpdatedState(isViewingToday)
    val currentVibrationEnabled = androidx.compose.runtime.rememberUpdatedState(vibrationEnabled)

    val onToggleTask: (Long) -> Unit = remember(viewModel, context, language) {
        { taskId ->
            if (!currentIsViewingToday.value) {
                Toast.makeText(context, AppStrings.get("past_day_readonly_toast", language), Toast.LENGTH_SHORT).show()
            } else {
                val currentTasks = currentTasksState.value
                val taskItem = currentTasks.firstOrNull { (t) -> t.id == taskId }
                val isItemBlocked = (taskItem != null) && !taskItem.isCompleted && when (taskItem.task.frequencyType) {
                    "WEEKDAYS" -> !taskItem.isDueToday
                    "INTERVAL" -> !taskItem.isDueToday
                    "WEEKLY" -> taskItem.isWeeklyGoalMet
                    else -> false
                }
                if (isItemBlocked) {
                    val toastMsg = when (taskItem.task.frequencyType) {
                        "WEEKDAYS" -> String.format(AppStrings.get("not_due_toast", language), DateUtils.formatDaysOfWeekShort(taskItem.task.daysOfWeek, language))
                        "INTERVAL" -> if (language == AppLanguage.EN) "Due in ${taskItem.daysUntilDue} days" else "In ${taskItem.daysUntilDue} Tagen fällig"
                        "WEEKLY" -> if (language == AppLanguage.EN) "Weekly goal reached (${taskItem.weeklyCompletedCount}/${taskItem.task.targetDaysPerWeek})" else "Wochenziel erreicht (${taskItem.weeklyCompletedCount}/${taskItem.task.targetDaysPerWeek})"
                        else -> AppStrings.get("not_due_cant_check", language)
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                } else {
                    userTriggeredAction = true
                    if (currentVibrationEnabled.value) HapticUtils.vibrateTick(context)
                    viewModel.toggleTask(taskId)
                }
            }
        }
    }

    val onIncrementTask: (Long, Int) -> Unit = remember(viewModel, context, language) {
        { taskId, delta ->
            if (!currentIsViewingToday.value) {
                Toast.makeText(context, AppStrings.get("past_day_readonly_toast", language), Toast.LENGTH_SHORT).show()
            } else {
                val currentTasks = currentTasksState.value
                val taskItem = currentTasks.firstOrNull { (t) -> t.id == taskId }
                val isItemBlocked = (taskItem != null) && !taskItem.isCompleted && when (taskItem.task.frequencyType) {
                    "WEEKDAYS" -> !taskItem.isDueToday
                    "INTERVAL" -> !taskItem.isDueToday
                    "WEEKLY" -> taskItem.isWeeklyGoalMet
                    else -> false
                }
                if (isItemBlocked && (delta > 0)) {
                    val toastMsg = when (taskItem.task.frequencyType) {
                        "WEEKDAYS" -> String.format(AppStrings.get("not_due_toast", language), DateUtils.formatDaysOfWeekShort(taskItem.task.daysOfWeek, language))
                        "INTERVAL" -> if (language == AppLanguage.EN) "Due in ${taskItem.daysUntilDue} days" else "In ${taskItem.daysUntilDue} Tagen fällig"
                        "WEEKLY" -> if (language == AppLanguage.EN) "Weekly goal reached (${taskItem.weeklyCompletedCount}/${taskItem.task.targetDaysPerWeek})" else "Wochenziel erreicht (${taskItem.weeklyCompletedCount}/${taskItem.task.targetDaysPerWeek})"
                        else -> AppStrings.get("not_due_cant_check", language)
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                } else {
                    userTriggeredAction = true
                    if (currentVibrationEnabled.value) HapticUtils.vibrateTick(context)
                    viewModel.incrementCounter(taskId, delta)
                }
            }
        }
    }

    val onStartTimerTask: (DailyTask) -> Unit = remember {
        { task -> activeTimerTask = task }
    }

    val onEditTask: (DailyTask) -> Unit = remember(onEditTaskClick) {
        { task -> onEditTaskClick(task) }
    }

    val displayDate = remember(selectedDateKey, language) {
        DateUtils.formatDisplayDate(selectedDateKey, language)
    }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with noticeably enlarged "0/5" badge on the top right
            item(key = "header", contentType = "header") {
                NothingHeader(
                    title = "DAILY",
                    subtitle = displayDate,
                    badgeText = "$completedCount / $totalCount",
                    badgeFontSize = 26.sp,
                )
            }

            // Notification Permission Card (if permission not granted)
            if (showPermissionCard) {
                item(key = "permission_card", contentType = "permission") {
                    NotificationPermissionCard(
                        onGrantClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDismissClick = { showPermissionCard = false },
                    )
                }
            }

            // Horizontal Date Strip (Past 6 days + Today)
            item(key = "date_strip", contentType = "date_strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    recentDays.forEach { dateKey ->
                        val isDateToday = DateUtils.isToday(dateKey)
                        val isStreakMet = if (isDateToday) {
                            if (isViewingToday && (selectedCategory == null)) {
                                totalCount in (1..completedCount)
                            } else {
                                isTodayCompletedLocally ?: fullyCompletedDatesSet.contains(dateKey)
                            }
                        } else if ((dateKey == selectedDateKey) && (selectedCategory == null)) {
                            (totalCount in (1..completedCount)) || fullyCompletedDatesSet.contains(dateKey)
                        } else {
                            fullyCompletedDatesSet.contains(dateKey)
                        }

                        DateStripItem(
                            dateKey = dateKey,
                            isSelected = dateKey == selectedDateKey,
                            isToday = isDateToday,
                            isStreakMet = isStreakMet,
                            language = language,
                            accentColor = accentColor,
                            onSelectDate = {
                                userTriggeredAction = false
                                viewModel.selectDate(it)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Summary Card (Progress + Streak - Vertically centered)
            item(key = "summary_card", contentType = "summary") {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = com.example.ui.components.ShapeCardLarge,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            val summaryTitle = if (totalCount == 0) t("no_habits_title")
                            else if (completedCount == totalCount) t("all_done_title")
                            else String.format(t("progress_completed_of"), completedCount, totalCount)

                            Text(
                                text = summaryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(t("progress_percent"), progressPct),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Dot Matrix Progress
                            DotMatrixProgress(
                                total = totalCount,
                                completed = completedCount,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Actions: Compact Freeze Button & Streak Counter Pill (clean single-line, perfectly vertically centered)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Freeze / Pause Icon Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeFreeze != null) accentColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .border(
                                        1.dp,
                                        if (activeFreeze != null) accentColor else MaterialTheme.colorScheme.outline,
                                        CircleShape,
                                    )
                                    .clickable { showFreezeDialog = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (activeFreeze != null) Icons.Default.AcUnit else Icons.Default.Pause,
                                    contentDescription = t("freeze_button_label"),
                                    tint = if (activeFreeze != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            // Streak Counter Pill (Always single-line, never wraps)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(34.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, com.example.ui.components.ShapePill)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, com.example.ui.components.ShapePill)
                                    .padding(horizontal = 10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val dayWord = if (language == AppLanguage.EN) {
                                    if (currentStreak == 1) "DAY" else "DAYS"
                                } else {
                                    if (currentStreak == 1) "TAG" else "TAGE"
                                }
                                Text(
                                    text = "$currentStreak $dayWord",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = headingFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    ),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Active Freeze Banner
            if (activeFreeze != null) {
                item(key = "active_freeze_banner", contentType = "freeze_banner") {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = com.example.ui.components.ShapeCardMedium,
                        backgroundColor = accentColor.copy(alpha = 0.1f),
                        borderColor = accentColor.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (activeFreeze?.type == "VACATION") t("freeze_banner_vacation") else t("freeze_banner_sickness"),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = headingFont,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = accentColor,
                                    )
                                    val formattedEndDate = activeFreeze?.endDate?.let { DateUtils.formatDateDmy(it) } ?: ""
                                    Text(
                                        text = String.format(t("freeze_banner_until"), formattedEndDate),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        activeFreeze?.let { viewModel.cancelStreakFreeze(it) }
                                        HapticUtils.vibrateTick(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = t("freeze_cancel_btn"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = bodyFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                    color = accentColor,
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Pills (LazyRow for zero-conflict silky smooth scrolling)
            item(key = "category_filter", contentType = "category_filter") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = categories,
                        key = { it },
                        contentType = { _ -> "category_filter_pill" },
                    ) { cat ->
                        val isSelected = when (cat) {
                            "ALL", "ALLE" -> selectedCategory == null
                            else -> (selectedCategory != null) && DailyViewModel.isCategoryEquivalent(selectedCategory, cat)
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface,
                                    com.example.ui.components.ShapePill,
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                    com.example.ui.components.ShapePill,
                                )
                                .clickable {
                                    val catToSelect = when (cat) {
                                        "ALL", "ALLE" -> null
                                        else -> cat
                                    }
                                    viewModel.selectCategory(catToSelect)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Past Day Read-Only Indicator
            if (!isViewingToday) {
                item(key = "past_day_indicator", contentType = "indicator") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingPill(
                            text = "• " + t("past_day_tag") + " •",
                            isAccent = false,
                        )
                    }
                }
            }

            // Tasks List
            if (tasks.isEmpty()) {
                item(key = "empty_tasks_state", contentType = "empty") {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = t("no_habits_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = headingFont),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("no_habits_subtitle"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                // 1. OPEN DUE TASKS (Must be done today!)
                if (openDueTasks.isNotEmpty()) {
                    item(key = "header_open_tasks", contentType = "section_header") {
                        TaskSectionHeader(
                            title = t("section_due_open"),
                            count = openDueTasks.size,
                            isAccent = true,
                        )
                    }

                    items(
                        items = openDueTasks,
                        key = { (t) -> t.id },
                        contentType = { (_) -> "task_row" },
                    ) { item ->
                        TaskRowItem(
                            item = item,
                            onToggle = onToggleTask,
                            onIncrement = onIncrementTask,
                            onEdit = onEditTask,
                            onStartTimer = onStartTimerTask,
                            isReadOnly = !isViewingToday,
                        )
                    }
                } else if (totalCount in (1..completedCount)) {
                    // All due habits completed for today!
                    item(key = "all_done_card", contentType = "all_done") {
                        NothingCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = com.example.ui.components.ShapeCardMedium,
                            backgroundColor = accentColor.copy(alpha = 0.08f),
                            borderColor = accentColor.copy(alpha = 0.4f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = t("all_done_title"),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = headingFont,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = t("all_done_subtitle"),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. COMPLETED TASKS
                if (completedTasks.isNotEmpty()) {
                    item(key = "header_completed_tasks", contentType = "section_header") {
                        TaskSectionHeader(
                            title = t("section_completed"),
                            count = completedTasks.size,
                            isAccent = false,
                        )
                    }

                    items(
                        items = completedTasks,
                        key = { (t) -> t.id },
                        contentType = { (_) -> "task_row" },
                    ) { item ->
                        TaskRowItem(
                            item = item,
                            onToggle = onToggleTask,
                            onIncrement = onIncrementTask,
                            onEdit = onEditTask,
                            onStartTimer = onStartTimerTask,
                            isReadOnly = !isViewingToday,
                        )
                    }
                }

                // 3. NOT DUE TODAY / REST DAY TASKS
                if (notDueTasks.isNotEmpty()) {
                    item(key = "header_not_due_tasks", contentType = "section_header") {
                        TaskSectionHeader(
                            title = t("section_not_due"),
                            count = notDueTasks.size,
                            isAccent = false,
                        )
                    }

                    items(
                        items = notDueTasks,
                        key = { (t) -> t.id },
                        contentType = { (_) -> "task_row" },
                    ) { item ->
                        TaskRowItem(
                            item = item,
                            onToggle = onToggleTask,
                            onIncrement = onIncrementTask,
                            onEdit = onEditTask,
                            onStartTimer = onStartTimerTask,
                            isReadOnly = !isViewingToday,
                        )
                    }
                }
            }
        }

        // Floating Action Button (+) cleanly positioned above bottom pill navigation (only on Today)
        if (isViewingToday) {
            FloatingActionButton(
                onClick = onAddTaskClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp, end = 20.dp)
                    .testTag("add_task_fab"),
                containerColor = accentColor,
                contentColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = t("add_habit"),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Dot-Matrix Completion Celebration Overlay
        CompletionCelebrationEffect(
            visible = showCelebration,
            streakDays = currentStreak,
            accentColor = accentColor,
            headingFont = headingFont,
            bodyFont = bodyFont,
            language = language,
            onDismiss = { showCelebration = false },
        )

        // Focus Timer Sheet
        activeTimerTask?.let { timerTask ->
            FocusTimerSheet(
                task = timerTask,
                accentColor = accentColor,
                headingFont = headingFont,
                bodyFont = bodyFont,
                language = language,
                onDismiss = { activeTimerTask = null },
            ) {
                val tId = timerTask.id
                activeTimerTask = null
                onToggleTask(tId)
            }
        }

        // Streak Freeze / Vacation Mode Dialog
        if (showFreezeDialog) {
            StreakFreezeDialog(
                accentColor = accentColor,
                headingFont = headingFont,
                bodyFont = bodyFont,
                language = language,
                onDismiss = { showFreezeDialog = false },
            ) { start, end, type, note ->
                viewModel.activateStreakFreeze(start, end, type, note)
                Toast.makeText(context, t("freeze_activated_toast"), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private val STEP_OPTIONS_LARGE = listOf(1, 10, 25)
private val STEP_OPTIONS_MEDIUM = listOf(1, 5, 10)
private val STEP_OPTIONS_SMALL = listOf(1, 2, 5)

@Composable
private fun DateStripItem(
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    isStreakMet: Boolean,
    language: AppLanguage,
    accentColor: Color,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayOfWeek = remember(dateKey, language) { DateUtils.getDayOfWeek(dateKey, language) }
    val dayNumber = remember(dateKey) { DateUtils.getDayNumber(dateKey) }
    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val borderClr = if (isSelected) accentColor else if (isToday) accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val textColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val numberColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val dotColor = if (isStreakMet) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .clip(com.example.ui.components.ShapeCardSmall)
            .background(bg, com.example.ui.components.ShapeCardSmall)
            .border(borderWidth, borderClr, com.example.ui.components.ShapeCardSmall)
            .clickable { onSelectDate(dateKey) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.titleMedium,
            color = numberColor,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
fun TaskRowItem(
    item: TaskWithCompletion,
    onToggle: (Long) -> Unit,
    onEdit: (DailyTask) -> Unit,
    modifier: Modifier = Modifier,
    onIncrement: (Long, Int) -> Unit = { _, _ -> },
    onStartTimer: ((DailyTask) -> Unit)? = null,
    isReadOnly: Boolean = false,
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val bodyFont = LocalBodyFontFamily.current
    fun t(key: String): String = AppStrings.get(key, language)

    val isDone = item.isCompleted
    val isNotDue = !isDone && when (item.task.frequencyType) {
        "WEEKDAYS" -> !item.isDueToday
        "INTERVAL" -> !item.isDueToday
        "WEEKLY" -> item.isWeeklyGoalMet
        else -> false
    }
    val isWeeklyOptional = !isDone && (item.task.frequencyType == "WEEKLY") && !item.isWeeklyGoalMet && !item.isMandatoryToday
    val isOpenDue = !isDone && !isNotDue && !isWeeklyOptional

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    val cardBg = remember(isNotDue, isDone, surfaceVariant, surface) {
        when {
            isNotDue -> surfaceVariant.copy(alpha = 0.45f)
            isDone -> surface.copy(alpha = 0.9f)
            else -> surface
        }
    }

    val cardBorder = remember(isNotDue, isOpenDue, accentColor, outline) {
        when {
            isNotDue -> outline.copy(alpha = 0.35f)
            isOpenDue -> accentColor.copy(alpha = 0.45f)
            else -> outline
        }
    }

    val notDueToastMsg = remember(item.task.id, item.task.frequencyType, item.task.daysOfWeek, item.task.targetDaysPerWeek, item.task.intervalDays, item.daysUntilDue, item.weeklyCompletedCount, item.isWeeklyGoalMet, language) {
        when (item.task.frequencyType) {
            "WEEKDAYS" -> String.format(t("not_due_toast"), DateUtils.formatDaysOfWeekShort(item.task.daysOfWeek, language))
            "WEEKLY" -> {
                if (language == AppLanguage.EN) "Weekly goal reached (${item.weeklyCompletedCount}/${item.task.targetDaysPerWeek}) • Rest day"
                else "Wochenziel erreicht (${item.weeklyCompletedCount}/${item.task.targetDaysPerWeek}) • Heute Pausentag"
            }
            "INTERVAL" -> {
                if (language == AppLanguage.EN) "Due in ${item.daysUntilDue} days • Rest day"
                else "In ${item.daysUntilDue} Tagen fällig • Heute Pausentag"
            }
            else -> t("not_due_cant_check")
        }
    }

    val cardClickAction: (() -> Unit)? = remember(isReadOnly, isNotDue, item.task, item.isCompleted, notDueToastMsg, onToggle, onStartTimer) {
        when {
            isReadOnly -> null
            isNotDue -> {
                { Toast.makeText(context, notDueToastMsg, Toast.LENGTH_SHORT).show() }
            }
            item.task.isCounter -> null
            (item.task.durationMinutes > 0) && !item.isCompleted -> {
                { onStartTimer?.invoke(item.task) }
            }
            else -> {
                { onToggle(item.task.id) }
            }
        }
    }

    val onCheckboxClick = remember(item.task, item.isCompleted, onToggle, onStartTimer) {
        {
            if ((item.task.durationMinutes > 0) && !item.isCompleted) {
                onStartTimer?.invoke(item.task)
            } else {
                onToggle(item.task.id)
            }
        }
    }

    val onToggleItem = remember(item.task.id, onToggle) {
        { onToggle(item.task.id) }
    }
    val onEditItem = remember(item.task, onEdit) {
        { onEdit(item.task) }
    }

    val categoryText = remember(item.task.category) { item.task.category.uppercase() }

    val frequencyText = remember(item.task.frequencyType, item.task.daysOfWeek, item.task.targetDaysPerWeek, item.task.intervalDays, item.weeklyCompletedCount, item.isWeeklyGoalMet, isWeeklyOptional, item.daysUntilDue, item.isDueToday, item.isCompleted, language) {
        when (item.task.frequencyType) {
            "WEEKLY" -> {
                if (item.isWeeklyGoalMet) {
                    String.format(t("weekly_goal_met_tag"), item.weeklyCompletedCount, item.task.targetDaysPerWeek)
                } else if (isWeeklyOptional) {
                    val remainingDays = DateUtils.getRemainingDaysInWeek(DateUtils.getTodayKey())
                    String.format(t("weekly_optional_hint"), remainingDays)
                } else {
                    String.format(t("weekly_progress_tag"), item.weeklyCompletedCount, item.task.targetDaysPerWeek)
                }
            }
            "WEEKDAYS" -> DateUtils.formatDaysOfWeekShort(item.task.daysOfWeek, language)
            "INTERVAL" -> {
                if (item.isCompleted) {
                    if (language == AppLanguage.EN) "Every ${item.task.intervalDays}d • Done" else "Alle ${item.task.intervalDays} Tage • Erledigt"
                } else if (item.isDueToday) {
                    if (language == AppLanguage.EN) "Every ${item.task.intervalDays}d • Due today" else "Alle ${item.task.intervalDays} Tage • Heute fällig"
                } else {
                    if (language == AppLanguage.EN) "Every ${item.task.intervalDays}d • In ${item.daysUntilDue}d" else "Alle ${item.task.intervalDays} Tage • In ${item.daysUntilDue}d"
                }
            }
            else -> null
        }
    }

    val counterText = remember(item.task.isCounter, item.count, item.task.targetCount, item.task.unit) {
        if (item.task.isCounter) {
            "${item.count} / ${item.task.targetCount} ${item.task.unit}".trim()
        } else null
    }

    val durationText = remember(item.task.durationMinutes) {
        if (item.task.durationMinutes > 0) "${item.task.durationMinutes} min" else null
    }

    val reminderText = remember(item.task.reminderEnabled, item.task.reminderHour, item.task.reminderMinute) {
        if (item.task.reminderEnabled && (item.task.reminderHour != null) && (item.task.reminderMinute != null)) {
            DateUtils.formatTime(item.task.reminderHour, item.task.reminderMinute)
        } else null
    }

    NothingCard(
        modifier = modifier.fillMaxWidth(),
        shape = com.example.ui.components.ShapeCardMedium,
        backgroundColor = cardBg,
        borderColor = cardBorder,
        onClick = cardClickAction,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Checkbox, Counter Progress Circle, or Calm Rest Day Icon (Moon)
                if (isNotDue) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), CircleShape)
                            .clickable {
                                Toast.makeText(context, notDueToastMsg, Toast.LENGTH_SHORT).show()
                            }
                            .testTag("rest_moon_${item.task.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = t("section_not_due"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                } else if (!item.task.isCounter) {
                    NothingCheckbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onCheckboxClick() },
                        enabled = !isReadOnly,
                        size = 28.dp,
                        testTag = "checkbox_${item.task.id}",
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isCompleted) accentColor
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            )
                            .border(
                                width = if (item.isCompleted) 0.dp else 1.5.dp,
                                color = if (item.isCompleted) Color.Transparent else if (isOpenDue) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .then(if (isReadOnly) Modifier else Modifier.clickable(onClick = onToggleItem)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            val pct = if (item.task.targetCount > 0) {
                                ((item.count.toFloat() / item.task.targetCount.toFloat()) * 100).toInt()
                            } else 0
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = accentColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Task Details: Title with habit icon
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = TaskIcons.getIcon(item.task.iconName),
                            contentDescription = null,
                            tint = when {
                                item.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                isNotDue -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                else -> accentColor
                            },
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = item.task.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = bodyFont,
                                fontSize = 16.sp,
                                fontWeight = if (isOpenDue) FontWeight.SemiBold else FontWeight.Normal,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            color = when {
                                item.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                isNotDue -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary pills section (extracted to prevent full row recomposition)
                    TaskPillsSection(
                        categoryText = categoryText,
                        isOpenDue = isOpenDue,
                        frequencyText = frequencyText,
                        isWeeklyGoalMet = item.isWeeklyGoalMet,
                        isCompleted = item.isCompleted,
                        counterText = counterText,
                        reminderText = reminderText,
                        accentColor = accentColor,
                        durationText = durationText,
                    )
                }

                // Focus Timer trigger button
                if ((item.task.durationMinutes > 0) && !isReadOnly && !item.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .clickable { onStartTimer?.invoke(item.task) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Timer",
                                tint = accentColor,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "${item.task.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = accentColor,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Edit button (only visible on today; locked on past days)
                if (!isReadOnly) {
                    IconButton(
                        onClick = onEditItem,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Enhanced counter progress and steppers (extracted to prevent full row recomposition)
            if (item.task.isCounter) {
                CounterProgressSection(
                    taskId = item.task.id,
                    count = item.count,
                    targetCount = item.task.targetCount,
                    isCompleted = item.isCompleted,
                    isReadOnly = isReadOnly,
                    isNotDue = isNotDue,
                    pastDayTag = t("past_day_tag"),
                    accentColor = accentColor,
                    onIncrement = onIncrement,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskPillsSection(
    categoryText: String,
    isOpenDue: Boolean,
    frequencyText: String?,
    isWeeklyGoalMet: Boolean,
    isCompleted: Boolean,
    counterText: String?,
    reminderText: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    durationText: String? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Category pill
        NothingPill(text = categoryText, isAccent = isOpenDue)

        // Frequency pill
        frequencyText?.let {
            NothingPill(
                text = it,
                isAccent = isWeeklyGoalMet || isCompleted || isOpenDue,
            )
        }

        counterText?.let {
            NothingPill(
                text = it,
                isAccent = isCompleted,
            )
        }

        durationText?.let {
            NothingPill(
                text = it,
                isAccent = isOpenDue,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isOpenDue) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                },
            )
        }

        // Reminder time pill at the bottom with other pills
        reminderText?.let {
            NothingPill(
                text = it,
                isAccent = isOpenDue,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (isOpenDue) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                },
            )
        }
    }
}

@Composable
fun CounterProgressSection(
    taskId: Long,
    count: Int,
    targetCount: Int,
    isCompleted: Boolean,
    isReadOnly: Boolean,
    isNotDue: Boolean,
    pastDayTag: String,
    accentColor: Color,
    onIncrement: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        val progress = if (targetCount > 0) {
            (count.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, com.example.ui.components.ShapeBox6),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(5.dp)
                    .background(
                        if (isCompleted) accentColor else accentColor.copy(alpha = 0.8f),
                        com.example.ui.components.ShapeBox6,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clean counter stepper buttons, read-only indicator, or rest-day pill
        if (isReadOnly) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NothingPill(
                    text = pastDayTag,
                    isAccent = false,
                )
            }
        } else if (!isNotDue) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val canDecrement = count > 0
                val isLargeTarget = targetCount >= 50

                // Decrement button: always stays in place so UI doesn't jump and user cannot miss-click
                val primaryDecStep = if (isLargeTarget) -10 else -1
                val decLabel = if (isLargeTarget && (count in (1..9))) "-1" else if (primaryDecStep == -10) "-10" else "-1"
                val decValue = if (isLargeTarget && (count in (1..9))) -1 else primaryDecStep

                Box(
                    modifier = Modifier
                        .background(
                            if (canDecrement) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            com.example.ui.components.ShapeBox8,
                        )
                        .clickable(enabled = canDecrement) {
                            if (canDecrement) {
                                onIncrement(taskId, decValue)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = decLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (canDecrement) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    )
                }

                // For large targets, provide an additional fine-grained -1 button when count >= 10
                if (isLargeTarget && (count >= 10)) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, com.example.ui.components.ShapeBox8)
                            .clickable { onIncrement(taskId, -1) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "-1",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Increments: +1, +5, +10 (or +25 for larger goals) from pre-allocated lists
                val stepOptions = when {
                    targetCount >= 100 -> STEP_OPTIONS_LARGE
                    targetCount >= 30 -> STEP_OPTIONS_MEDIUM
                    else -> STEP_OPTIONS_SMALL
                }

                stepOptions.forEach { step ->
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.12f), com.example.ui.components.ShapeBox8)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), com.example.ui.components.ShapeBox8)
                            .clickable { onIncrement(taskId, step) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "+$step",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = accentColor,
                        )
                    }
                }
            }
        }
    }
}
