package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyTask
import com.example.ui.DailyViewModel
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TaskEditDialog
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.GeistFontFamily
import com.example.ui.theme.GeistMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NDotFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.util.AppSettings
import com.example.util.BodyFont
import com.example.util.DateUtils
import com.example.util.HeadingFont
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

enum class DailyNavTab {
    TODAY,
    ROUTINES,
    STATS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: DailyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val appSettings = remember { AppSettings.getInstance(context) }
            val selectedAccent by appSettings.selectedAccent.collectAsStateWithLifecycle()
            val selectedHeadingFont by appSettings.selectedHeadingFont.collectAsStateWithLifecycle()
            val selectedBodyFont by appSettings.selectedBodyFont.collectAsStateWithLifecycle()
            val selectedLanguage by appSettings.selectedLanguage.collectAsStateWithLifecycle()

            val headingFontFamily = remember(selectedHeadingFont) {
                when (selectedHeadingFont) {
                    HeadingFont.NDOT -> NDotFontFamily
                    HeadingFont.GEIST -> GeistMonoFontFamily
                    HeadingFont.SILKSCREEN -> SilkscreenFontFamily
                    HeadingFont.SANS -> FontFamily.SansSerif
                }
            }
            val bodyFontFamily = remember(selectedBodyFont) {
                when (selectedBodyFont) {
                    BodyFont.GEIST -> GeistFontFamily
                    BodyFont.MONO -> GeistMonoFontFamily
                    BodyFont.SANS -> FontFamily.SansSerif
                }
            }

            CompositionLocalProvider(
                LocalAccentColor provides selectedAccent.color,
                LocalAppLanguage provides selectedLanguage,
                LocalHeadingFontFamily provides headingFontFamily,
                LocalBodyFontFamily provides bodyFontFamily
            ) {
                MyApplicationTheme(
                    accentColor = selectedAccent.color,
                    headingFont = headingFontFamily,
                    bodyFont = bodyFontFamily
                ) {
                    MainContent(viewModel = viewModel, appSettings = appSettings)
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: DailyViewModel, appSettings: AppSettings) {
    var currentTab by rememberSaveable { mutableStateOf(DailyNavTab.TODAY) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var taskToEdit by rememberSaveable { mutableStateOf<DailyTask?>(null) }

    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val tasksWithCompletions by viewModel.tasksWithCompletions.collectAsStateWithLifecycle()
    val allActiveTasks by viewModel.allActiveTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val streakStats by viewModel.streakStats.collectAsStateWithLifecycle()
    val allCompletionDates by viewModel.allCompletionDates.collectAsStateWithLifecycle()

    val openTaskEditor: (DailyTask?) -> Unit = remember {
        { task ->
            taskToEdit = task
            showEditSheet = true
        }
    }
    val dismissTaskEditor = remember {
        {
            showEditSheet = false
            taskToEdit = null
        }
    }
    val onAddTaskClick = remember {
        {
            taskToEdit = null
            showEditSheet = true
        }
    }

    BackHandler(enabled = currentTab != DailyNavTab.TODAY) {
        currentTab = DailyNavTab.TODAY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentTab) {
                DailyNavTab.TODAY -> {
                    TodayScreen(
                        viewModel = viewModel,
                        selectedDateKey = selectedDateKey,
                        tasks = tasksWithCompletions,
                        selectedCategory = selectedCategory,
                        currentStreak = currentStreak,
                        completionDates = allCompletionDates,
                        onAddTaskClick = onAddTaskClick,
                        onEditTaskClick = { task ->
                            if (DateUtils.isToday(selectedDateKey)) {
                                openTaskEditor(task)
                            }
                        }
                    )
                }

                DailyNavTab.ROUTINES -> {
                    RoutinesScreen(
                        viewModel = viewModel,
                        tasks = allTasks,
                        onAddTaskClick = onAddTaskClick,
                        onEditTaskClick = { task ->
                            openTaskEditor(task)
                        }
                    )
                }

                DailyNavTab.STATS -> {
                    StatsScreen(
                        viewModel = viewModel,
                        streakStats = streakStats,
                        completionDates = allCompletionDates,
                        totalTasksCount = allActiveTasks.size
                    )
                }

                DailyNavTab.SETTINGS -> {
                    SettingsScreen(
                        appSettings = appSettings
                    )
                }
            }

            NothingBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showEditSheet) {
            TaskEditDialog(
                task = taskToEdit,
                onDismiss = dismissTaskEditor,
                onSave = { updatedTask ->
                    viewModel.saveTask(updatedTask)
                    dismissTaskEditor()
                },
                onDelete = { taskToDelete ->
                    viewModel.deleteTask(taskToDelete)
                    dismissTaskEditor()
                }
            )
        }
    }
}

@Composable
fun NothingBottomNavigation(
    currentTab: DailyNavTab,
    onTabSelected: (DailyNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val currentLang = LocalAppLanguage.current
    val bodyFont = LocalBodyFontFamily.current

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF101014))
                .border(
                    width = 1.dp,
                    color = Color(0xFF26262E),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DailyNavTab.entries.forEach { tab ->
                NothingBottomNavItem(
                    tab = tab,
                    isSelected = tab == currentTab,
                    currentLang = currentLang,
                    accentColor = accentColor,
                    bodyFont = bodyFont,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}

@Composable
private fun NothingBottomNavItem(
    tab: DailyNavTab,
    isSelected: Boolean,
    currentLang: com.example.util.AppLanguage,
    accentColor: Color,
    bodyFont: FontFamily,
    onTabSelected: (DailyNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) Color(0xFFF5F5F7) else Color(0xFF6E6E78)
    val bgColor = if (isSelected) Color(0xFF1C1C22) else Color.Transparent

    val tabTitle = remember(tab, currentLang) {
        when (tab) {
            DailyNavTab.TODAY -> if (currentLang == com.example.util.AppLanguage.EN) "TODAY" else "HEUTE"
            DailyNavTab.ROUTINES -> if (currentLang == com.example.util.AppLanguage.EN) "ROUTINES" else "ROUTINEN"
            DailyNavTab.STATS -> "STATS"
            DailyNavTab.SETTINGS -> if (currentLang == com.example.util.AppLanguage.EN) "SETTINGS" else "EINSTELLUNGEN"
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onTabSelected(tab) }
            )
            .defaultMinSize(minHeight = 44.dp)
            .padding(
                horizontal = if (tab == DailyNavTab.SETTINGS) 12.dp else 14.dp,
                vertical = 8.dp
            )
            .testTag("nav_tab_${tab.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (tab == DailyNavTab.SETTINGS) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = tabTitle,
                tint = if (isSelected) accentColor else contentColor,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = tabTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = bodyFont,
                    letterSpacing = 1.sp
                ),
                color = contentColor
            )
        }
    }
}
