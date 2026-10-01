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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
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

    val activeCount = tasks.count { !it.isArchived }
    val totalCount = tasks.size
    val badgeString = if (totalCount == 0) {
        "0 ${t("active_badge")}"
    } else {
        "$activeCount / $totalCount ${t("active_badge")}"
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NothingHeader(
                    title = t("routines_header"),
                    subtitle = t("routines_subtitle"),
                    badgeText = badgeString,
                    badgeFontSize = 20.sp
                )
            }

            if (!resetInfoDismissed) {
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                item {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
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
                items(tasks, key = { it.id }) { task ->
                    RoutineItemCard(
                        task = task,
                        onEdit = { onEditTaskClick(task) },
                        onToggleActive = {
                            viewModel.toggleTaskActive(task)
                        },
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

@OptIn(ExperimentalLayoutApi::class)
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
    val cardAlpha = if (isActive) 1f else 0.55f

    NothingCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (cardAlpha < 1f) Modifier.graphicsLayer { alpha = cardAlpha } else Modifier),
        shape = RoundedCornerShape(16.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) accentColor else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = bodyFont,
                            fontSize = 16.sp
                        ),
                        color = if (isActive) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NothingPill(text = task.category.uppercase())

                    if (!isActive) {
                        NothingPill(
                            text = t("paused_badge"),
                            isAccent = false
                        )
                    }

                    when (task.frequencyType) {
                        "WEEKLY" -> {
                            NothingPill(
                                text = String.format(t("times_per_week"), task.targetDaysPerWeek),
                                isAccent = isActive
                            )
                        }
                        "WEEKDAYS" -> {
                            NothingPill(
                                text = DateUtils.formatDaysOfWeekShort(task.daysOfWeek, language),
                                isAccent = isActive
                            )
                        }
                        else -> {
                            // "DAILY"
                        }
                    }

                    if (task.isCounter) {
                        NothingPill(
                            text = "${t("goal_label")}: ${task.targetCount} ${task.unit}".trim(),
                            isAccent = isActive
                        )
                    }

                    if (task.reminderHour != null && task.reminderMinute != null) {
                        NothingPill(
                            text = DateUtils.formatTime(task.reminderHour, task.reminderMinute),
                            isAccent = isActive && task.reminderEnabled,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (task.reminderEnabled && isActive) Icons.Default.Alarm else Icons.Default.AlarmOff,
                                    contentDescription = null,
                                    tint = if (task.reminderEnabled && isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                }
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
