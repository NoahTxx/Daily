package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyTask
import com.example.ui.DailyViewModel
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingHeader
import com.example.ui.components.NothingPill
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

@Composable
fun RoutinesScreen(
    viewModel: DailyViewModel,
    tasks: List<DailyTask>,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (DailyTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current
    fun t(key: String): String = AppStrings.get(key, language)

    val resetInfoDismissed by viewModel.routinesResetInfoDismissed.collectAsStateWithLifecycle()

    val activeCount = remember(tasks) { tasks.count { !it.isArchived } }
    val totalCount = remember(tasks) { tasks.size }
    val badgeString = remember(activeCount, totalCount, language) {
        if (totalCount == 0) {
            "0 ${t("active_badge")}"
        } else {
            "$activeCount / $totalCount ${t("active_badge")}"
        }
    }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header", contentType = "header") {
                NothingHeader(
                    title = t("routines_header"),
                    subtitle = null,
                    badgeText = badgeString,
                    badgeFontSize = 20.sp
                )
            }

            if (!resetInfoDismissed) {
                item(key = "reset_info", contentType = "reset_info") {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = com.example.ui.components.ShapeCardMedium,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = t("routines_info"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissRoutinesResetInfo() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = t("dismiss_hint"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                item(key = "empty_routines", contentType = "empty") {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = com.example.ui.components.ShapeCardLarge
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t("no_routines_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = headingFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t("no_routines_subtitle"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = tasks,
                    key = { it.id },
                    contentType = { "routine_card" }
                ) { task ->
                    RoutineItemCard(
                        task = task,
                        onEdit = { onEditTaskClick(task) },
                        onToggleActive = { viewModel.toggleTaskActive(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }

        // Add Routine FAB
        FloatingActionButton(
            onClick = onAddTaskClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, end = 20.dp)
                .testTag("add_routine_fab"),
            containerColor = accentColor,
            contentColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = t("new_routine"),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RoutineItemCard(
    task: DailyTask,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val bodyFont = LocalBodyFontFamily.current
    val headingFont = LocalHeadingFontFamily.current
    fun t(key: String): String = AppStrings.get(key, language)

    val isActive = !task.isArchived
    val cardBg = remember(isActive) {
        if (isActive) Color.Unspecified else Color.Unspecified
    }

    val categoryText = remember(task.category) { task.category.uppercase() }
    val freqText = remember(task.frequencyType, task.targetDaysPerWeek, task.daysOfWeek, task.intervalDays, language) {
        when (task.frequencyType) {
            "WEEKLY" -> String.format(t("times_per_week"), task.targetDaysPerWeek)
            "WEEKDAYS" -> DateUtils.formatDaysOfWeekShort(task.daysOfWeek, language)
            "INTERVAL" -> String.format(t("every_x_days"), task.intervalDays)
            else -> null
        }
    }
    val counterText = remember(task.isCounter, task.targetCount, task.unit) {
        if (task.isCounter) "${t("goal_label")}: ${task.targetCount} ${task.unit}".trim() else null
    }
    val reminderText = remember(task.reminderHour, task.reminderMinute) {
        if (task.reminderHour != null && task.reminderMinute != null) {
            DateUtils.formatTime(task.reminderHour, task.reminderMinute)
        } else null
    }

    NothingCard(
        modifier = modifier.fillMaxWidth(),
        shape = com.example.ui.components.ShapeCardMedium,
        backgroundColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title row with status dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) accentColor else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Default,
                            fontSize = 16.sp
                        ),
                        color = if (isActive) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RoutinePillsSection(
                    categoryText = categoryText,
                    isActive = isActive,
                    freqText = freqText,
                    counterText = counterText,
                    reminderText = reminderText,
                    reminderEnabled = task.reminderEnabled,
                    accentColor = accentColor,
                    pausedText = t("paused_badge")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Active/Inactive toggle switch
            Switch(
                checked = isActive,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier.testTag("routine_toggle_${task.id}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = t("edit_label"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinePillsSection(
    categoryText: String,
    isActive: Boolean,
    freqText: String?,
    counterText: String?,
    reminderText: String?,
    reminderEnabled: Boolean,
    accentColor: Color,
    pausedText: String,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        NothingPill(text = categoryText)

        if (!isActive) {
            NothingPill(
                text = pausedText,
                isAccent = false
            )
        }

        if (freqText != null) {
            NothingPill(
                text = freqText,
                isAccent = isActive
            )
        }

        if (counterText != null) {
            NothingPill(
                text = counterText,
                isAccent = isActive
            )
        }

        if (reminderText != null) {
            NothingPill(
                text = reminderText,
                isAccent = isActive && reminderEnabled,
                leadingIcon = {
                    Icon(
                        imageVector = if (reminderEnabled && isActive) Icons.Default.Alarm else Icons.Default.AlarmOff,
                        contentDescription = null,
                        tint = if (reminderEnabled && isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            )
        }
    }
}
