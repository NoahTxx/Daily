package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DailyViewModel
import com.example.ui.HabitStats
import com.example.ui.StreakStats
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingHeader
import com.example.ui.components.NothingPill
import com.example.ui.components.ShapeBox6
import com.example.ui.components.ShapeBox8
import com.example.ui.components.ShapeCardLarge
import com.example.ui.components.ShapeCardMedium
import com.example.ui.components.ShapeCardSmall
import com.example.ui.components.ShapePill
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

    // Synchronize or default to first habit using derivedStateOf
    val selectedHabitStats = remember(habitStatsList, selectedHabitIdFromVm, localSelectedHabitId) {
        derivedStateOf {
            val currentSelectedHabitId = selectedHabitIdFromVm
                ?: localSelectedHabitId
                ?: habitStatsList.firstOrNull()?.task?.id

            habitStatsList.firstOrNull { it.task.id == currentSelectedHabitId }
                ?: habitStatsList.firstOrNull()
        }
    }.value

    val habitCompletedDateSet = remember(selectedHabitStats?.task?.id, selectedHabitStats?.completedDateSet) {
        selectedHabitStats?.completedDateSet ?: emptySet()
    }

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
        item(key = "stats_header") {
            NothingHeader(
                title = t("stats_header"),
                subtitle = null
            )
        }

        // Segmented Control: [ ÜBERSICHT ] | [ PRO GEWOHNHEIT ]
        item(key = "segmented_control") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapePill)
                    .background(MaterialTheme.colorScheme.surfaceVariant, ShapePill)
                    .border(1.dp, MaterialTheme.colorScheme.outline, ShapePill)
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
                        .clip(ShapePill)
                        .background(byHabitBg, ShapePill)
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
                        .clip(ShapePill)
                        .background(overallBg, ShapePill)
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
                item(key = "empty_habits_card") {
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
                // Horizontal Habit Carousel Selector (Using LazyRow for virtualized, buttery smooth scrolling)
                item(key = "habit_carousel_selector") {
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

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = habitStatsList,
                                key = { "carousel_chip_${it.task.id}" }
                            ) { hStats ->
                                val isSelected = hStats.task.id == selectedHabitStats.task.id
                                val cardBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                                val borderClr = if (isSelected) accentColor
                                else MaterialTheme.colorScheme.outline

                                Row(
                                    modifier = Modifier
                                        .background(cardBg, ShapeCardMedium)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = borderClr,
                                            shape = ShapeCardMedium
                                        )
                                        .clip(ShapeCardMedium)
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
                                                .background(
                                                    if (isSelected) accentColor.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    ShapeBox8
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
                item(key = "selected_habit_card_${selectedHabitStats.task.id}") {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCardLarge,
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
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
                                        fontFamily = FontFamily.Default,
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
                item(key = "habit_metrics_grid_${selectedHabitStats.task.id}") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Row 1: Current Streak & Record Streak
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current Streak Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = ShapeCardMedium,
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
                                    val streakSubtext = when (selectedHabitStats.task.frequencyType) {
                                        "WEEKLY" -> if (selectedHabitStats.currentStreak == 1) (if (language == AppLanguage.EN) "Week in a row" else "Woche in Folge") else (if (language == AppLanguage.EN) "Weeks in a row" else "Wochen in Folge")
                                        "INTERVAL" -> if (selectedHabitStats.currentStreak == 1) (if (language == AppLanguage.EN) "Interval in a row" else "Intervall in Folge") else (if (language == AppLanguage.EN) "Intervals in a row" else "Intervalle in Folge")
                                        else -> if (selectedHabitStats.currentStreak == 1) t("habit_day_single") else t("habit_days_plural")
                                    }
                                    Text(
                                        text = streakSubtext,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Best Streak Card
                            NothingCard(
                                modifier = Modifier.weight(1f),
                                shape = ShapeCardMedium,
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
                                    // FIXED: Using localized longest_streak_label
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
                                shape = ShapeCardMedium,
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
                                shape = ShapeCardMedium,
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
                item(key = "habit_recent_7_days_${selectedHabitStats.task.id}") {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCardLarge,
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
                                            .background(
                                                if (day.isToday) MaterialTheme.colorScheme.surfaceVariant
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                ShapeBox8
                                            )
                                            .border(
                                                width = if (day.isToday) 1.5.dp else 1.dp,
                                                color = if (day.isToday) accentColor else MaterialTheme.colorScheme.outline,
                                                shape = ShapeBox8
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
                item(key = "habit_month_matrix_${selectedHabitStats.task.id}") {
                    MonthMatrixCard(
                        title = t("habit_month_matrix"),
                        subtitle = String.format(
                            t("month_overview_sub"),
                            currentMonthName,
                            selectedHabitStats.monthlyCompletedDays,
                            selectedHabitStats.monthlyDueDays
                        ),
                        percentage = "${selectedHabitStats.monthlyAdherencePercent}%",
                        calendarWeeks = calendarWeeks,
                        weekdayLabels = weekdayLabels,
                        completedDateSet = habitCompletedDateSet,
                        accentColor = accentColor,
                        bodyFont = bodyFont
                    )
                }

                // Milestones for this Habit
                item(key = "habit_milestones_${selectedHabitStats.task.id}") {
                    MilestonesCard(
                        title = t("habit_milestones_title"),
                        milestones = milestones,
                        longestStreak = selectedHabitStats.longestStreak,
                        currentStreak = selectedHabitStats.currentStreak,
                        accentColor = accentColor,
                        bodyFont = bodyFont,
                        language = language
                    )
                }

                // All Habits Comparison Section Header
                item(key = "habit_comparison_header") {
                    Text(
                        text = t("habit_all_comparison"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = bodyFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Individual Comparison Items: Virtualized so Compose recycles them smoothly!
                items(
                    items = habitStatsList,
                    key = { "habit_comp_${it.task.id}" },
                    contentType = { "habit_comparison" }
                ) { hStats ->
                    HabitComparisonCard(
                        hStats = hStats,
                        isSelected = hStats.task.id == selectedHabitStats.task.id,
                        accentColor = accentColor,
                        bodyFont = bodyFont,
                        language = language,
                        onClick = {
                            localSelectedHabitId = hStats.task.id
                            viewModel.selectHabitForStats(hStats.task.id)
                        }
                    )
                }
            }
        }

        // =========================================================================
        // VIEW MODE: OVERALL (GESAMTÜBERSICHT)
        // =========================================================================
        if (viewMode == StatsViewMode.OVERALL) {
            // Streak & Longest Streak Metrics Row
            item(key = "overall_streak_metrics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Streak Card
                    NothingCard(
                        modifier = Modifier.weight(1f),
                        shape = ShapeCardMedium,
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
                        shape = ShapeCardMedium,
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
                            // FIXED: Using localized longest_streak_label (Längste Streak)
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
            item(key = "overall_adherence_metrics") {
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCardMedium,
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
                            // FIXED: Total days string
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
            item(key = "overall_milestones") {
                MilestonesCard(
                    title = t("milestones_header"),
                    milestones = milestones,
                    longestStreak = streakStats.longestStreak,
                    currentStreak = streakStats.currentStreak,
                    accentColor = accentColor,
                    bodyFont = bodyFont,
                    language = language
                )
            }

            // Current Month Dot Matrix Heatmap
            item(key = "overall_month_matrix") {
                MonthMatrixCard(
                    title = t("month_overview"),
                    subtitle = String.format(t("month_overview_sub"), currentMonthName, daysWithCompletions, daysInMonth),
                    percentage = t("stat_streak_label"),
                    calendarWeeks = calendarWeeks,
                    weekdayLabels = weekdayLabels,
                    completedDateSet = completedDateSet,
                    accentColor = accentColor,
                    bodyFont = bodyFont
                )
            }

            // Habits Summary Card at bottom of Overall view with direct link to individual habit view
            if (habitStatsList.isNotEmpty()) {
                item(key = "overall_habits_preview") {
                    NothingCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCardLarge,
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
                                            .background(MaterialTheme.colorScheme.surfaceVariant, ShapeCardSmall)
                                            .clip(ShapeCardSmall)
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
                                    .background(accentColor.copy(alpha = 0.12f), ShapeCardSmall)
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), ShapeCardSmall)
                                    .clip(ShapeCardSmall)
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

/**
 * Individual Habit Comparison Card (Skippable & Virtualized for smooth 60fps scrolling)
 */
@Composable
private fun HabitComparisonCard(
    hStats: HabitStats,
    isSelected: Boolean,
    accentColor: Color,
    bodyFont: FontFamily,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun t(key: String): String = AppStrings.get(key, language)

    val totalText = remember(hStats.task.isCounter, hStats.totalCountSum, hStats.totalCompletions, hStats.task.unit, language) {
        if (hStats.task.isCounter) {
            String.format(t("habit_total_count_format"), hStats.totalCountSum, hStats.task.unit)
        } else {
            String.format(t("habit_times_completed_format"), hStats.totalCompletions)
        }
    }
    val recordText = remember(hStats.longestStreak, language) {
        String.format(t("habit_record_format"), hStats.longestStreak)
    }

    NothingCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_comparison_${hStats.task.id}"),
        shape = ShapeCardMedium,
        backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
        onClick = onClick
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
                                fontFamily = FontFamily.Default,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = hStats.task.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val streakUnit = if (hStats.task.frequencyType == "WEEKLY") "w" else "d"
                    NothingPill(
                        text = "${hStats.currentStreak}$streakUnit",
                        isAccent = hStats.currentStreak > 0
                    )
                    Text(
                        text = "${hStats.monthlyAdherencePercent}%",
                        style = MaterialTheme.typography.labelMedium.copy(
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
                    .clip(ShapeBox6),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = recordText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable Milestones Card
 */
@Composable
private fun MilestonesCard(
    title: String,
    milestones: List<StreakMilestone>,
    longestStreak: Int,
    currentStreak: Int,
    accentColor: Color,
    bodyFont: FontFamily,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    fun t(key: String): String = AppStrings.get(key, language)

    NothingCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCardLarge,
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
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
                    val isReached = longestStreak >= milestone.days

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isReached) accentColor.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                ShapeCardSmall
                            )
                            .border(
                                1.dp,
                                if (isReached) accentColor.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline,
                                ShapeCardSmall
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
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isReached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = t(milestone.descKey),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isReached) {
                            NothingPill(text = t("milestone_reached"), isAccent = true)
                        } else {
                            val left = (milestone.days - currentStreak).coerceAtLeast(0)
                            val leftText = if (left == 1) t("milestone_left_single") else String.format(t("milestone_left"), left)
                            Text(
                                text = leftText,
                                style = MaterialTheme.typography.labelSmall.copy(
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

/**
 * Fast & Pre-allocated Month Matrix Grid
 */
@Composable
private fun MonthMatrixCard(
    title: String,
    subtitle: String,
    percentage: String,
    calendarWeeks: List<List<String?>>,
    weekdayLabels: List<String>,
    completedDateSet: Set<String>,
    accentColor: Color,
    bodyFont: FontFamily,
    modifier: Modifier = Modifier
) {
    val todayKey = remember { DateUtils.getTodayKey() }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    NothingCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCardLarge,
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
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
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
                        text = percentage,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7-Column month grid with pre-allocated ShapeBox8
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = onSurfaceVariant.copy(alpha = 0.6f),
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
                            val isCompleted = dateKey != null && completedDateSet.contains(dateKey)
                            val isToday = dateKey == todayKey

                            CalendarDayCell(
                                dateKey = dateKey,
                                isCompleted = isCompleted,
                                isToday = isToday,
                                accentColor = accentColor,
                                surfaceVariant = surfaceVariant,
                                outline = outline,
                                onSurfaceVariant = onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
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

/**
 * Isolated Day Cell composable to avoid whole-calendar recomposition on state changes
 */
@Composable
private fun CalendarDayCell(
    dateKey: String?,
    isCompleted: Boolean,
    isToday: Boolean,
    accentColor: Color,
    surfaceVariant: Color,
    outline: Color,
    onSurfaceVariant: Color,
    modifier: Modifier = Modifier
) {
    if (dateKey == null) {
        Spacer(modifier = modifier)
    } else {
        val cellBg = if (isCompleted) accentColor else surfaceVariant
        val cellBorder = if (isToday) accentColor else outline
        val borderWidth = if (isToday) 1.5.dp else 1.dp
        val dayNum = remember(dateKey) { DateUtils.getDayNumber(dateKey) }
        val textColor = if (isCompleted) {
            if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White
        } else onSurfaceVariant

        Box(
            modifier = modifier
                .height(36.dp)
                .background(cellBg, ShapeBox8)
                .border(borderWidth, cellBorder, ShapeBox8),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNum,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
