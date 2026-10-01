package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DailyViewModel
import com.example.ui.HabitDayStatus
import com.example.ui.HabitStats
import com.example.ui.StreakStats
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingHeader
import com.example.ui.components.NothingPill
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StreakMilestone(
    val days: Int,
    val titleKey: String,
    val descKey: String
)

enum class StatsViewMode {
    OVERALL,
    BY_HABIT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: DailyViewModel,
    streakStats: StreakStats,
    completionDates: List<String>,
    totalTasksCount: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current
    val isEn = language == AppLanguage.EN
    fun t(key: String): String = AppStrings.get(key, language)

    val habitStatsList by viewModel.habitStatsList.collectAsStateWithLifecycle()
    val selectedHabitIdFromVm by viewModel.selectedHabitIdForStats.collectAsStateWithLifecycle()

    var viewMode by rememberSaveable { mutableStateOf(StatsViewMode.BY_HABIT) }
    var localSelectedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Synchronize or default to first habit
    val currentSelectedHabitId = selectedHabitIdFromVm
        ?: localSelectedHabitId
        ?: habitStatsList.firstOrNull()?.task?.id

    val selectedHabitStats = habitStatsList.firstOrNull { it.task.id == currentSelectedHabitId }
        ?: habitStatsList.firstOrNull()

    val calendar = remember { Calendar.getInstance() }
    val locale = if (isEn) Locale.US else Locale.GERMAN
    val currentMonthName = remember(language) {
        SimpleDateFormat("MMMM yyyy", locale).format(calendar.time).uppercase()
    }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val monthKeyPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time) }
    val daysOfMonth = remember(daysInMonth, monthKeyPrefix) {
        (1..daysInMonth).map { String.format(Locale.US, "%s-%02d", monthKeyPrefix, it) }
    }
    val calendarSlots: List<String?> = remember(daysOfMonth) {
        val firstIso = daysOfMonth.firstOrNull()?.let { DateUtils.getIsoDayOfWeek(it) } ?: 1
        val leadingNulls = List<String?>(firstIso - 1) { null }
        leadingNulls + daysOfMonth
    }
    val calendarWeeks = remember(calendarSlots) { calendarSlots.chunked(7) }
    val weekdayLabels = remember(isEn) {
        if (isEn) listOf("M", "T", "W", "T", "F", "S", "S")
        else listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    }
    val completedDateSet = remember(completionDates) { completionDates.toSet() }

    val daysWithCompletions = remember(daysOfMonth, completedDateSet) { daysOfMonth.count { completedDateSet.contains(it) } }
    val monthlyAdherence = remember(daysWithCompletions, daysInMonth) { if (daysInMonth > 0) (daysWithCompletions * 100) / daysInMonth else 0 }

    val milestones = remember {
        listOf(
            StreakMilestone(3, "ms_3_title", "ms_3_desc"),
            StreakMilestone(7, "ms_7_title", "ms_7_desc"),
            StreakMilestone(14, "ms_14_title", "ms_14_desc"),
            StreakMilestone(30, "ms_30_title", "ms_30_desc"),
            StreakMilestone(100, "ms_100_title", "ms_100_desc")
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            NothingHeader(
                title = t("stats_header"),
                subtitle = t("stats_subtitle")
            )
        }

        // Segmented Control: [ ÜBERSICHT ] | [ PRO GEWOHNHEIT ]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // By Habit Tab
                val isByHabitSelected = viewMode == StatsViewMode.BY_HABIT
                val byHabitBg by animateColorAsState(
                    targetValue = if (isByHabitSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = spring(),
                    label = "byHabitBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(byHabitBg)
                        .clickable { viewMode = StatsViewMode.BY_HABIT }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isByHabitSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Text(
                            text = t("stats_tab_habits"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = bodyFont,
                                fontWeight = if (isByHabitSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isByHabitSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Overall Tab
                val isOverallSelected = viewMode == StatsViewMode.OVERALL
                val overallBg by animateColorAsState(
                    targetValue = if (isOverallSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = spring(),
                    label = "overallBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(overallBg)
                        .clickable { viewMode = StatsViewMode.OVERALL }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOverallSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Text(
                            text = t("stats_tab_overall"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = bodyFont,
                                fontWeight = if (isOverallSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isOverallSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // =========================================================================
        // VIEW MODE: BY HABIT (INDIVIDUAL HABIT STATISTICS)
        // =========================================================================
        if (viewMode == StatsViewMode.BY_HABIT) {
            if (habitStatsList.isEmpty()) {
                item {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
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
                                text = t("habit_no_habits"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = headingFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("habit_no_habits_desc"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (selectedHabitStats != null) {
                // Horizontal Habit Carousel Selector
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = t("select_habit_hint"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = bodyFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            habitStatsList.forEach { hStats ->
                                val isSelected = hStats.task.id == selectedHabitStats.task.id
                                val cardBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                                val borderClr = if (isSelected) accentColor
                                else MaterialTheme.colorScheme.outline

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = borderClr,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .background(cardBg)
                                        .clickable {
                                            localSelectedHabitId = hStats.task.id
                                            viewModel.selectHabitForStats(hStats.task.id)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Status Dot indicator
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    hStats.isTodayCompleted -> accentColor
                                                    hStats.isTodayDue -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )

                                    Text(
                                        text = hStats.task.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (hStats.currentStreak > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) accentColor.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${hStats.currentStreak}d",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = bodyFont,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isSelected) accentColor
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Habit Main Card
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Top Row: Category & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    NothingPill(
                                        text = selectedHabitStats.task.category.uppercase(),
                                        isAccent = true
                                    )

                                    when (selectedHabitStats.task.frequencyType) {
                                        "WEEKLY" -> {
                                            NothingPill(
                                                text = String.format(t("times_per_week"), selectedHabitStats.task.targetDaysPerWeek),
                                                isAccent = false
                                            )
                                        }
                                        "WEEKDAYS" -> {
                                            NothingPill(
                                                text = DateUtils.formatDaysOfWeekShort(selectedHabitStats.task.daysOfWeek, language),
                                                isAccent = false
                                            )
                                        }
                                        else -> {
                                            NothingPill(
                                                text = t("freq_daily"),
                                                isAccent = false
                                            )
                                        }
                                    }

                                    if (selectedHabitStats.task.isCounter) {
                                        NothingPill(
                                            text = "${t("goal_label")}: ${selectedHabitStats.task.targetCount} ${selectedHabitStats.task.unit}".trim(),
                                            isAccent = false
                                        )
                                    }
                                }

                                // Today status
                                val (statusText, statusAccent) = when {
                                    selectedHabitStats.isTodayCompleted -> Pair(t("habit_status_secured"), true)
                                    !selectedHabitStats.isTodayDue -> Pair(t("habit_status_rest"), false)
                                    else -> Pair(t("habit_status_open"), false)
                                }
                                NothingPill(text = statusText, isAccent = statusAccent)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Habit Title
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedHabitStats.task.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = bodyFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2x2 Metric Cards Grid for the selected habit
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Row 1: Current Streak & Record Streak
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current Streak Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = t("habit_streak_current"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "${selectedHabitStats.currentStreak}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp,
                                            lineHeight = 42.sp
                                        ),
                                        color = accentColor
                                    )
                                    Text(
                                        text = if (selectedHabitStats.currentStreak == 1) t("habit_day_single") else t("habit_days_plural"),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Best Streak Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = t("habit_streak_best"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "${selectedHabitStats.longestStreak}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp,
                                            lineHeight = 42.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = t("longest_streak_label"),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Row 2: Monthly Adherence & Total Accomplishment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Monthly Adherence Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = t("habit_completion_rate"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${selectedHabitStats.monthlyAdherencePercent}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(
                                            t("active_days_subtext"),
                                            selectedHabitStats.monthlyCompletedDays,
                                            selectedHabitStats.monthlyDueDays
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Total Completions / Total Volume Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (selectedHabitStats.task.isCounter) t("habit_total_volume") else t("habit_total_done"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (selectedHabitStats.task.isCounter) {
                                        Text(
                                            text = "${selectedHabitStats.totalCountSum}",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = bodyFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 28.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${selectedHabitStats.task.unit} (${selectedHabitStats.totalCompletions}×)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "${selectedHabitStats.totalCompletions}×",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = bodyFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 28.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = t("total_days_subtext"),
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent 7 Days Mini Activity Tracker
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
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
                                Text(
                                    text = t("habit_last_7_days"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedHabitStats.recent7Days.forEach { day ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (day.isToday) 1.5.dp else 1.dp,
                                                color = if (day.isToday) accentColor else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                if (day.isToday) MaterialTheme.colorScheme.surfaceVariant
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = day.dayLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = bodyFont,
                                                fontSize = 10.sp,
                                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (day.isToday) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = day.dayNumber,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = bodyFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Status symbol
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        day.isCompleted -> accentColor
                                                        day.isToday -> accentColor.copy(alpha = 0.15f)
                                                        day.isDue -> MaterialTheme.colorScheme.surface
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = when {
                                                        day.isCompleted -> accentColor
                                                        day.isToday -> accentColor
                                                        day.isDue -> MaterialTheme.colorScheme.outline
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (day.isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Done",
                                                    tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            } else if (!day.isDue) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.outline)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Month Matrix Heatmap for this Habit
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = t("habit_month_matrix"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(
                                            t("month_overview_sub"),
                                            currentMonthName,
                                            selectedHabitStats.monthlyCompletedDays,
                                            selectedHabitStats.monthlyDueDays
                                        ),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Text(
                                        text = "${selectedHabitStats.monthlyAdherencePercent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // 7-Column month grid for this habit
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Weekday headers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    weekdayLabels.forEach { label ->
                                        Text(
                                            text = label,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = bodyFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                calendarWeeks.forEach { week ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        week.forEach { dateKey ->
                                            if (dateKey == null) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            } else {
                                                val isCompleted = selectedHabitStats.completedDateSet.contains(dateKey)
                                                val isToday = DateUtils.isToday(dateKey)

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            width = if (isToday) 1.5.dp else 1.dp,
                                                            color = if (isToday) accentColor else MaterialTheme.colorScheme.outline,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .background(
                                                            when {
                                                                isCompleted -> accentColor
                                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = DateUtils.getDayNumber(dateKey),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = bodyFont,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        ),
                                                        color = if (isCompleted) {
                                                            if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White
                                                        } else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        val remaining = 7 - week.size
                                        repeat(remaining) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Milestones for this Habit
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
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
                                Text(
                                    text = t("habit_milestones_title"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                milestones.forEach { milestone ->
                                    val isReached = selectedHabitStats.longestStreak >= milestone.days

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                if (isReached) accentColor.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                if (isReached) accentColor.copy(alpha = 0.08f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isReached) accentColor
                                                    else MaterialTheme.colorScheme.outline
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isReached) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Reached",
                                                    tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surface)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = t(milestone.titleKey),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = bodyFont,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isReached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = t(milestone.descKey),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isReached) {
                                            NothingPill(text = t("milestone_reached"), isAccent = true)
                                        } else {
                                            val left = (milestone.days - selectedHabitStats.currentStreak).coerceAtLeast(0)
                                            Text(
                                                text = String.format(t("milestone_left"), left),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = bodyFont,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // All Habits Comparison Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = t("habit_all_comparison"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = bodyFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        habitStatsList.forEach { hStats ->
                            val isCurrent = hStats.task.id == selectedHabitStats.task.id
                            NothingCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("habit_comparison_${hStats.task.id}"),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                borderColor = if (isCurrent) accentColor else MaterialTheme.colorScheme.outline,
                                onClick = {
                                    localSelectedHabitId = hStats.task.id
                                    viewModel.selectHabitForStats(hStats.task.id)
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (hStats.isTodayCompleted) accentColor
                                                        else MaterialTheme.colorScheme.outline
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = hStats.task.title,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontFamily = bodyFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = hStats.task.category.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = bodyFont,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            NothingPill(
                                                text = "${hStats.currentStreak}d",
                                                isAccent = hStats.currentStreak > 0
                                            )
                                            Text(
                                                text = "${hStats.monthlyAdherencePercent}%",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = bodyFont,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Adherence Progress Bar
                                    LinearProgressIndicator(
                                        progress = { hStats.monthlyAdherencePercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = accentColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (hStats.task.isCounter) {
                                                "${hStats.totalCountSum} ${hStats.task.unit} gesamt"
                                            } else {
                                                String.format(t("habit_times_completed_format"), hStats.totalCompletions)
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = bodyFont,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Rekord: ${hStats.longestStreak}d",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = bodyFont,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // VIEW MODE: OVERALL (GESAMTÜBERSICHT)
        // =========================================================================
        if (viewMode == StatsViewMode.OVERALL) {
            // Streak & Longest Streak Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Streak Card
                    NothingCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("current_streak"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Today status badge
                                Text(
                                    text = if (streakStats.isTodaySecured) t("streak_secured") else t("streak_open"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = bodyFont,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (streakStats.isTodaySecured) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${streakStats.currentStreak}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = bodyFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp,
                                    lineHeight = 44.sp
                                ),
                                color = accentColor
                            )
                            Text(
                                text = if (streakStats.currentStreak == 1) t("day_single") else t("days_plural"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Best Streak Card
                    NothingCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t("best_streak"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${streakStats.longestStreak}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = bodyFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp,
                                    lineHeight = 44.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("longest_streak_label"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Adherence & Total Active Days Row
            item {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${t("rate_label")} ($currentMonthName)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$monthlyAdherence%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = bodyFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    lineHeight = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(t("active_days_subtext"), daysWithCompletions, daysInMonth),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )

                        Column {
                            Text(
                                text = t("total_days"),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${streakStats.totalDays}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = bodyFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    lineHeight = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("total_days_subtext"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Streak Milestones
            item {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
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
                            Text(
                                text = t("milestones_header"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            milestones.forEach { milestone ->
                                val isReached = streakStats.longestStreak >= milestone.days

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isReached) accentColor.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .background(
                                            if (isReached) accentColor.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isReached) accentColor
                                                else MaterialTheme.colorScheme.outline
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isReached) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Reached",
                                                tint = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surface)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t(milestone.titleKey),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = bodyFont,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isReached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = t(milestone.descKey),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isReached) {
                                        NothingPill(text = t("milestone_reached"), isAccent = true)
                                    } else {
                                        val left = (milestone.days - streakStats.currentStreak).coerceAtLeast(0)
                                        Text(
                                            text = String.format(t("milestone_left"), left),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = bodyFont,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Current Month Dot Matrix Heatmap
            item {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = t("month_overview"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(t("month_overview_sub"), currentMonthName, daysWithCompletions, daysInMonth),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = "Streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Month days in 7-column rows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Weekday headers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                weekdayLabels.forEach { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = bodyFont,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            calendarWeeks.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    week.forEach { dateKey ->
                                        if (dateKey == null) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        } else {
                                            val isCompleted = completedDateSet.contains(dateKey)
                                            val isToday = DateUtils.isToday(dateKey)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (isToday) 1.5.dp else 1.dp,
                                                        color = if (isToday) accentColor else MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .background(
                                                        when {
                                                            isCompleted -> accentColor
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = DateUtils.getDayNumber(dateKey),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = bodyFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (isCompleted) {
                                                        if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White
                                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    val remaining = 7 - week.size
                                    repeat(remaining) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Habits Summary Card at bottom of Overall view with direct link to individual habit view
            if (habitStatsList.isNotEmpty()) {
                item {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
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
                                    Text(
                                        text = t("habit_overview_card_title"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = t("habit_overview_card_sub"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mini habit list
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                habitStatsList.take(4).forEach { hStats ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                localSelectedHabitId = hStats.task.id
                                                viewModel.selectHabitForStats(hStats.task.id)
                                                viewMode = StatsViewMode.BY_HABIT
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (hStats.isTodayCompleted) accentColor
                                                        else MaterialTheme.colorScheme.outline
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = hStats.task.title,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = bodyFont,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            NothingPill(
                                                text = "${hStats.currentStreak}d",
                                                isAccent = hStats.currentStreak > 0
                                            )
                                            Text(
                                                text = "${hStats.monthlyAdherencePercent}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = bodyFont,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Button to switch to individual habits
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { viewMode = StatsViewMode.BY_HABIT }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t("habit_view_details_btn"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = bodyFont,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
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
