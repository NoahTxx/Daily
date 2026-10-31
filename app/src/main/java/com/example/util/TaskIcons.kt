package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PillCapsuleIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PillCapsule",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Upper-right half of capsule (original shape, centered at 12, 12)
        path(fill = SolidColor(Color.White)) {
            moveTo(9.8f, 8.4f)
            lineTo(12.8f, 5.4f)
            curveTo(14.4f, 3.8f, 17.0f, 3.8f, 18.6f, 5.4f)
            curveTo(20.2f, 7.0f, 20.2f, 9.6f, 18.6f, 11.2f)
            lineTo(15.6f, 14.2f)
            close()
        }
        // Lower-left half of capsule (original shape, centered at 12, 12)
        path(fill = SolidColor(Color.White)) {
            moveTo(14.2f, 15.6f)
            lineTo(11.2f, 18.6f)
            curveTo(9.6f, 20.2f, 7.0f, 20.2f, 5.4f, 18.6f)
            curveTo(3.8f, 17.0f, 3.8f, 14.4f, 5.4f, 12.8f)
            lineTo(8.4f, 9.8f)
            close()
        }
    }.build()
}

data class TaskIconItem(
    val key: String,
    val icon: ImageVector,
    val labelDe: String,
    val labelEn: String,
)

object TaskIcons {
    val items: List<TaskIconItem> = listOf(
        TaskIconItem("pill", PillCapsuleIcon, "Pille / Supplements", "Supplements"),
        TaskIconItem("water", Icons.Default.WaterDrop, "Wasser / Trinken", "Water / Hydration"),
        TaskIconItem("fitness", Icons.Default.FitnessCenter, "Fitness / Gym", "Fitness / Workout"),
        TaskIconItem("running", Icons.AutoMirrored.Filled.DirectionsRun, "Laufen / Cardio", "Running / Cardio"),
        TaskIconItem("book", Icons.AutoMirrored.Filled.MenuBook, "Lesen / Lernen", "Reading / Learning"),
        TaskIconItem("brain", Icons.Default.Psychology, "Fokus / Geist", "Focus / Mind"),
        TaskIconItem("meditation", Icons.Default.SelfImprovement, "Meditation / Ruhe", "Meditation"),
        TaskIconItem("sleep", Icons.Default.Bedtime, "Schlaf / Erholung", "Sleep / Rest"),
        TaskIconItem("timer", Icons.Default.Timer, "Zeit / Timer", "Focus Timer"),
        TaskIconItem("heart", Icons.Default.Favorite, "Gesundheit / Herz", "Health / Vitals"),
        TaskIconItem("nutrition", Icons.Default.Restaurant, "Ernährung / Essen", "Diet / Food"),
        TaskIconItem("code", Icons.Default.Code, "Arbeit / Code", "Work / Code"),
        TaskIconItem("walk", Icons.AutoMirrored.Filled.DirectionsWalk, "Schritte / Spazieren", "Walking / Steps"),
        TaskIconItem("clean", Icons.Default.CleaningServices, "Ordnung / Haushalt", "Cleaning / Chores"),
        TaskIconItem("sparkles", Icons.Default.AutoAwesome, "Gewohnheit / Daily", "Daily Habit"),
    )

    fun getIcon(key: String): ImageVector {
        return items.firstOrNull { (k) -> k.equals(key, ignoreCase = true) }?.icon
            ?: PillCapsuleIcon
    }
}
