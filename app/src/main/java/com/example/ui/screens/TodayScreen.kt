package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.DailyTask
import com.example.data.TaskWithCompletion
import com.example.ui.DailyViewModel
import com.example.ui.components.CompletionCelebrationEffect
import com.example.ui.components.DotMatrixProgress
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingCheckbox
import com.example.ui.components.NothingHeader
import com.example.ui.components.NothingPill
import com.example.ui.components.NotificationPermissionCard
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.util.HapticUtils
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

@Composable
fun TodayScreen(
    viewModel: DailyViewModel,
    selectedDateKey: String,
    tasks: List<TaskWithCompletion>,
    selectedCategory: String?,
    currentStreak: Int,
    completionDates: List<String>,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (DailyTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPermissionCard by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            } else false
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showPermissionCard = !isGranted
    }

    val dueTasks = remember(tasks) { tasks.filter { it.isDueToday || it.isCompleted || it.isWeeklyGoalMet } }
    val completedCount = remember(dueTasks) { dueTasks.count { it.isCompleted || (it.task.frequencyType == "WEEKLY" && it.isWeeklyGoalMet) } }
    val totalCount = dueTasks.size
    val progressPct = remember(completedCount, totalCount) { if (totalCount > 0) (completedCount * 100) / totalCount else 0 }
    val recentDays = remember { DateUtils.getPastDays(7) }
    val isViewingToday = DateUtils.isToday(selectedDateKey)
    val completionDateSet = remember(completionDates) { completionDates.toSet() }

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

    val appSettings = remember { AppSettings.getInstance(context) }
    val vibrationEnabled by appSettings.vibrationFeedbackEnabled.collectAsState()
    val animationEnabled by appSettings.completionAnimationEnabled.collectAsState()

    var showCelebration by remember { mutableStateOf(false) }
    var previousCompletedCount by remember(selectedDateKey) { mutableIntStateOf(completedCount) }

    LaunchedEffect(completedCount, totalCount) {
        if (totalCount > 0 && completedCount == totalCount && previousCompletedCount < totalCount && DateUtils.isToday(selectedDateKey)) {
            if (animationEnabled) {
                showCelebration = true
            }
            if (vibrationEnabled) {
                HapticUtils.vibrateSuccess(context)
            }
        }
        previousCompletedCount = completedCount
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with noticeably enlarged "0/5" badge on the top right
            item {
                NothingHeader(
                    title = "DAILY",
                    subtitle = DateUtils.formatDisplayDate(selectedDateKey, language),
                    badgeText = "$completedCount / $totalCount",
                    badgeFontSize = 26.sp
                )
            }

            // Notification Permission Card (if permission not granted)
            if (showPermissionCard) {
                item {
                    NotificationPermissionCard(
                        onGrantClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDismissClick = { showPermissionCard = false }
                    )
                }
            }

            // Horizontal Date Strip (Past 6 days + Today)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentDays.forEach { dateKey ->
                        val isSelected = dateKey == selectedDateKey
                        val isToday = DateUtils.isToday(dateKey)
                        val hasCompletions = completionDateSet.contains(dateKey)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { viewModel.selectDate(dateKey) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DateUtils.getDayOfWeek(dateKey, language),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = bodyFont,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = DateUtils.getDayNumber(dateKey),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = headingFont,
                                    fontSize = 15.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            // Dot indicator
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            hasCompletions -> accentColor
                                            isToday -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Summary Card (Progress + Streak)
            item {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val summaryTitle = if (totalCount == 0) t("no_habits_title")
                                else if (completedCount == totalCount) t("all_done_title")
                                else String.format(t("progress_completed_of"), completedCount, totalCount)

                                Text(
                                    text = summaryTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(t("progress_percent"), progressPct),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = headingFont),
                                    color = accentColor
                                )
                            }

                            // Streak Counter Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(t("streak_badge_days"), currentStreak),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dot Matrix Progress
                        DotMatrixProgress(
                            total = totalCount,
                            completed = completedCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = when (cat) {
                            "ALL", "ALLE" -> selectedCategory == null
                            else -> selectedCategory != null && DailyViewModel.isCategoryEquivalent(selectedCategory, cat)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(100.dp)
                                )
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    val catToSelect = when (cat) {
                                        "ALL", "ALLE" -> null
                                        else -> cat
                                    }
                                    viewModel.selectCategory(catToSelect)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Past Day Read-Only Indicator
            if (!isViewingToday) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingPill(
                            text = "• " + t("past_day_tag") + " •",
                            isAccent = false
                        )
                    }
                }
            }

            // Tasks List
            if (tasks.isEmpty()) {
                item {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = t("no_habits_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = headingFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("no_habits_subtitle"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.task.id }) { item ->
                    TaskRowItem(
                        item = item,
                        onToggle = {
                            if (!isViewingToday) {
                                Toast.makeText(context, t("past_day_readonly_toast"), Toast.LENGTH_SHORT).show()
                                return@TaskRowItem
                            }
                            if (vibrationEnabled) HapticUtils.vibrateTick(context)
                            viewModel.toggleTask(item.task.id)
                        },
                        onIncrement = { delta ->
                            if (!isViewingToday) {
                                Toast.makeText(context, t("past_day_readonly_toast"), Toast.LENGTH_SHORT).show()
                                return@TaskRowItem
                            }
                            if (vibrationEnabled) HapticUtils.vibrateTick(context)
                            viewModel.incrementCounter(item.task.id, delta)
                        },
                        onEdit = { onEditTaskClick(item.task) },
                        isReadOnly = !isViewingToday
                    )
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
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = t("add_habit"),
                    modifier = Modifier.size(24.dp)
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
            onDismiss = { showCelebration = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRowItem(
    item: TaskWithCompletion,
    onToggle: () -> Unit,
    onIncrement: (Int) -> Unit = {},
    onEdit: () -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current
    fun t(key: String): String = AppStrings.get(key, language)

    val cardAlpha = if (item.isDueToday || item.isCompleted || item.isWeeklyGoalMet) 1f else 0.72f

    NothingCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (cardAlpha < 1f) Modifier.graphicsLayer { alpha = cardAlpha } else Modifier),
        shape = RoundedCornerShape(16.dp),
        onClick = if (isReadOnly || item.task.isCounter) null else onToggle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox or Counter Progress Circle
                if (!item.task.isCounter) {
                    NothingCheckbox(
                        checked = item.isCompleted,
                        onCheckedChange = onToggle,
                        enabled = !isReadOnly,
                        size = 28.dp,
                        testTag = "checkbox_${item.task.id}"
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (item.isCompleted) 0.dp else 1.5.dp,
                                color = if (item.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .background(
                                if (item.isCompleted) accentColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(if (isReadOnly) Modifier else Modifier.clickable { onToggle() }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            val pct = if (item.task.targetCount > 0) {
                                ((item.count.toFloat() / item.task.targetCount.toFloat()) * 100).toInt()
                            } else 0
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = bodyFont,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Task Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.task.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = bodyFont,
                            fontSize = 16.sp,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Category pill
                        NothingPill(text = item.task.category.uppercase())

                        // Frequency pill
                        when (item.task.frequencyType) {
                            "WEEKLY" -> {
                                val text = if (item.isWeeklyGoalMet) {
                                    String.format(t("weekly_goal_met_tag"), item.weeklyCompletedCount, item.task.targetDaysPerWeek)
                                } else {
                                    String.format(t("weekly_progress_tag"), item.weeklyCompletedCount, item.task.targetDaysPerWeek)
                                }
                                NothingPill(
                                    text = text,
                                    isAccent = item.isWeeklyGoalMet || item.isCompleted
                                )
                            }
                            "WEEKDAYS" -> {
                                val daysText = DateUtils.formatDaysOfWeekShort(item.task.daysOfWeek, language)
                                if (!item.isDueToday) {
                                    NothingPill(
                                        text = "$daysText • ${t("not_due_today_tag")}",
                                        isAccent = false
                                    )
                                } else {
                                    NothingPill(
                                        text = daysText,
                                        isAccent = item.isCompleted
                                    )
                                }
                            }
                            else -> {}
                        }

                        if (item.task.isCounter) {
                            NothingPill(
                                text = "${item.count} / ${item.task.targetCount} ${item.task.unit}".trim(),
                                isAccent = item.isCompleted
                            )
                        }

                        // Reminder time pill (if set)
                        if (item.task.reminderEnabled && item.task.reminderHour != null && item.task.reminderMinute != null) {
                            NothingPill(
                                text = DateUtils.formatTime(item.task.reminderHour, item.task.reminderMinute),
                                isAccent = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Edit button (only visible on today; locked on past days)
                if (!isReadOnly) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Enhanced counter progress and steppers
            if (item.task.isCounter) {
                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                val progress = if (item.task.targetCount > 0) {
                    (item.count.toFloat() / item.task.targetCount.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (item.isCompleted) accentColor else accentColor.copy(alpha = 0.8f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clean counter stepper buttons or read-only indicator
                if (isReadOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingPill(
                            text = t("past_day_tag"),
                            isAccent = false
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canDecrement = item.count > 0
                        val isLargeTarget = item.task.targetCount >= 50

                        // Decrement button: always stays in place so UI doesn't jump and user cannot miss-click
                        val primaryDecStep = if (isLargeTarget) -10 else -1
                        val decLabel = if (isLargeTarget && item.count in 1..9) "-1" else if (primaryDecStep == -10) "-10" else "-1"
                        val decValue = if (isLargeTarget && item.count in 1..9) -1 else primaryDecStep

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (canDecrement) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .clickable(enabled = canDecrement) {
                                    if (canDecrement) {
                                        onIncrement(decValue)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = decLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = bodyFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (canDecrement) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }

                        // For large targets, provide an additional fine-grained -1 button when count >= 10
                        if (isLargeTarget && item.count >= 10) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onIncrement(-1) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "-1",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = bodyFont,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Increments: +1, +5, +10 (or +25 for larger goals)
                        val stepOptions = if (item.task.targetCount >= 100) listOf(1, 10, 25)
                        else if (item.task.targetCount >= 30) listOf(1, 5, 10)
                        else listOf(1, 2, 5)

                        stepOptions.forEach { step ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .clickable { onIncrement(step) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+$step",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = bodyFont,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
