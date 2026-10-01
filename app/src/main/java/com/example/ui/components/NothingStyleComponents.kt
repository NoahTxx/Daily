package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalAccentColor
import com.example.util.LocalAppLanguage
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(backgroundColor)
            .then(clickableModifier)
    ) {
        content()
    }
}

@Composable
fun NothingCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    enabled: Boolean = true,
    testTag: String = "habit_checkbox"
) {
    val accentColor = LocalAccentColor.current

    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else MaterialTheme.colorScheme.outline,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
        label = "checkboxBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
        label = "checkboxBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "checkboxDotScale"
    )

    val clickModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onCheckedChange
        )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .background(bgColor)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(size * 0.45f)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun NothingPill(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val accentColor = LocalAccentColor.current
    val bodyFont = LocalBodyFontFamily.current
    val bg = if (isAccent) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isAccent) accentColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
    val textColor = if (isAccent) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, border, RoundedCornerShape(100.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = bodyFont,
                lineHeight = 14.sp
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun NothingHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeFontSize: TextUnit = 22.sp,
    badgeFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = if (badgeText != null) {
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = headingFont,
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.8.sp
                    ),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = bodyFont,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = badgeFontFamily ?: headingFont,
                        fontSize = badgeFontSize,
                        lineHeight = badgeFontSize * 1.25f,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DotMatrixProgress(
    total: Int,
    completed: Int,
    modifier: Modifier = Modifier
) {
    if (total == 0) return
    val accentColor = LocalAccentColor.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total.coerceAtMost(16)) { index ->
            val isDone = index < completed
            val dotColor = if (isDone) accentColor else MaterialTheme.colorScheme.outline
            val dotSize = if (isDone) 7.dp else 5.dp

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun NotificationPermissionCard(
    onGrantClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val language = LocalAppLanguage.current
    val headingFont = LocalHeadingFontFamily.current
    val bodyFont = LocalBodyFontFamily.current

    val title = AppStrings.get("perm_title", language)
    val description = AppStrings.get("perm_desc", language)
    val buttonText = AppStrings.get("perm_allow", language)

    NothingCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        borderColor = accentColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = headingFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor)
                    .clickable(onClick = onGrantClick)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = bodyFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
