package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.reminder.ReminderManager
import com.example.ui.components.CompletionCelebrationEffect
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingHeader
import com.example.ui.theme.GeistFontFamily
import com.example.ui.theme.GeistMonoFontFamily
import com.example.ui.theme.NDotFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.util.AccentColor
import com.example.util.AppLanguage
import com.example.util.AppSettings
import com.example.util.AppStrings
import com.example.util.HapticUtils
import com.example.util.BodyFont
import com.example.util.HeadingFont
import com.example.util.LocalAccentColor
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val currentHeadingFont = LocalHeadingFontFamily.current
    val currentBodyFont = LocalBodyFontFamily.current

    val reminderEnabled by appSettings.generalReminderEnabled.collectAsState()
    val reminderHour by appSettings.generalReminderHour.collectAsState()
    val reminderMinute by appSettings.generalReminderMinute.collectAsState()
    val selectedAccent by appSettings.selectedAccent.collectAsState()
    val selectedHeadingFont by appSettings.selectedHeadingFont.collectAsState()
    val selectedBodyFont by appSettings.selectedBodyFont.collectAsState()
    val selectedLanguage by appSettings.selectedLanguage.collectAsState()
    val vibrationEnabled by appSettings.vibrationFeedbackEnabled.collectAsState()
    val animationEnabled by appSettings.completionAnimationEnabled.collectAsState()
    var testCelebrationVisible by remember { mutableStateOf(false) }

    fun t(key: String): String = AppStrings.get(key, selectedLanguage)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            appSettings.setGeneralReminderEnabled(true)
            ReminderManager.scheduleGeneralDailyReminder(
                context,
                reminderHour,
                reminderMinute
            )
            val msg = "${t("reminder_enabled_toast")} %02d:%02d".format(reminderHour, reminderMinute)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, t("perm_desc"), Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header without version badge as requested by user
        item {
            NothingHeader(
                title = t("settings_header"),
                subtitle = t("settings_subtitle"),
                badgeText = null
            )
        }

        // Section: General Incomplete Habits Reminder
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = t("general_reminder_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        appSettings.setGeneralReminderEnabled(true)
                                        ReminderManager.scheduleGeneralDailyReminder(
                                            context,
                                            reminderHour,
                                            reminderMinute
                                        )
                                        val msg = "${t("reminder_enabled_toast")} %02d:%02d".format(reminderHour, reminderMinute)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    appSettings.setGeneralReminderEnabled(false)
                                    ReminderManager.cancelGeneralDailyReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Time Picker row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            appSettings.setGeneralReminderTime(h, m)
                                            ReminderManager.scheduleGeneralDailyReminder(context, h, m)
                                        },
                                        reminderHour,
                                        reminderMinute,
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("time_label"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = currentBodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = currentHeadingFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Notification button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        ReminderManager.triggerTestNotification(context)
                                        Toast.makeText(context, t("test_notification_sent"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("test_notification_btn"),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section: Feedback & Effects (Haptic & Completion Animation)
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
                    // Item 1: Vibrations-Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = t("settings_haptics_title"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t("settings_haptics_desc"),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = currentBodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { isChecked ->
                                appSettings.setVibrationFeedbackEnabled(isChecked)
                                if (isChecked) {
                                    HapticUtils.vibrateTick(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Item 2: Completion Effect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = t("settings_animation_title"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t("settings_animation_desc"),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = currentBodyFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = animationEnabled,
                            onCheckedChange = { isChecked ->
                                appSettings.setCompletionAnimationEnabled(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    if (animationEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Effect Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    testCelebrationVisible = true
                                    if (vibrationEnabled) {
                                        HapticUtils.vibrateSuccess(context)
                                    }
                                }
                                .defaultMinSize(minHeight = 44.dp)
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("settings_test_effect_btn"),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section: Accent Color
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = t("accent_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("accent_desc"),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val isSelected = accent == selectedAccent
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { appSettings.setAccentColor(accent) }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) accent.color else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .background(accent.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (accent == AccentColor.WHITE) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = accent.displayName.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = currentBodyFont,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) accent.color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Typography (fully functional and transforms headings/body everywhere)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = t("typography_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("typography_desc"),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = t("headings_label"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = currentBodyFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Heading Font Options: Vertically stacked clean cards with uniform equal height
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeadingFont.entries.forEach { fontOption ->
                            val isSelected = fontOption == selectedHeadingFont
                            val previewFont = when (fontOption) {
                                HeadingFont.NDOT -> NDotFontFamily
                                HeadingFont.GEIST -> GeistMonoFontFamily
                                HeadingFont.SILKSCREEN -> SilkscreenFontFamily
                                HeadingFont.SANS -> FontFamily.SansSerif
                            }
                            val sample = when (fontOption) {
                                HeadingFont.NDOT -> "DAILY 24"
                                HeadingFont.GEIST -> "DAILY 24"
                                HeadingFont.SILKSCREEN -> "DAILY 24"
                                HeadingFont.SANS -> "DAILY 24"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { appSettings.setHeadingFont(fontOption) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Selection dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else MaterialTheme.colorScheme.outline)
                                    )

                                    Column {
                                        Text(
                                            text = fontOption.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = currentBodyFont,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = fontOption.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = currentBodyFont,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Live font sample preview
                                Text(
                                    text = sample,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = previewFont,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t("body_label"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = currentBodyFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Body Font Options: Vertically stacked clean cards with uniform equal height
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BodyFont.entries.forEach { fontOption ->
                            val isSelected = fontOption == selectedBodyFont
                            val previewFont = when (fontOption) {
                                BodyFont.GEIST -> GeistFontFamily
                                BodyFont.MONO -> GeistMonoFontFamily
                                BodyFont.SANS -> FontFamily.SansSerif
                            }
                            val sample = when (fontOption) {
                                BodyFont.GEIST -> "Geist Sans 123"
                                BodyFont.MONO -> "Geist Mono 123"
                                BodyFont.SANS -> "System Sans 123"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { appSettings.setBodyFont(fontOption) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Selection dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else MaterialTheme.colorScheme.outline)
                                    )

                                    Column {
                                        Text(
                                            text = fontOption.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = currentBodyFont,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = fontOption.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = currentBodyFont,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Live font sample preview
                                Text(
                                    text = sample,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = previewFont,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Language
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = t("language_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("language_desc"),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = currentBodyFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang == selectedLanguage
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { appSettings.setLanguage(lang) }
                                    .defaultMinSize(minHeight = 48.dp)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accentColor)
                                        )
                                    }
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = currentHeadingFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        CompletionCelebrationEffect(
            visible = testCelebrationVisible,
            streakDays = 14,
            accentColor = accentColor,
            headingFont = currentHeadingFont,
            bodyFont = currentBodyFont,
            language = selectedLanguage,
            onDismiss = { testCelebrationVisible = false }
        )
    }
}
