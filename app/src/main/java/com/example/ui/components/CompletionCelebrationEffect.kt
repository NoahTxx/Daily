package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings

/**
 * Ultra-clean, sleek Nothing-style completion celebration.
 * Pure minimal UI with NO particle effects:
 * Snappy checkmark badge pop-in, clear streak status capsule,
 * and effortless tap-to-dismiss.
 */
@Composable
fun CompletionCelebrationEffect(
    visible: Boolean,
    streakDays: Int,
    accentColor: Color,
    headingFont: FontFamily,
    bodyFont: FontFamily,
    language: AppLanguage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    val progress = animProgress.value

    // Smooth enter and exit timing
    // Entrance: 0f -> 0.16f (snappy pop)
    // Hold: 0.16f -> 0.84f (crisp and clear)
    // Exit: 0.84f -> 1.0f (soft smooth fade)
    val enterT = (progress / 0.16f).coerceIn(0f, 1f)
    val exitT = ((progress - 0.84f) / 0.16f).coerceIn(0f, 1f)

    // Snappy entrance curve with subtle bounce
    val cardScale = when {
        enterT < 0.7f -> 0.85f + (0.20f * (enterT / 0.7f))
        enterT < 1.0f -> 1.05f - (0.05f * ((enterT - 0.7f) / 0.3f))
        else -> 1f - (0.04f * exitT)
    }

    val checkIconScale = when {
        enterT < 0.6f -> (enterT / 0.6f) * 1.15f
        enterT < 1.0f -> 1.15f - (0.15f * ((enterT - 0.6f) / 0.4f))
        else -> 1f
    }

    val cardAlpha = (enterT * (1f - exitT)).coerceIn(0f, 1f)
    val scrimAlpha = (0.50f * (1f - exitT)).coerceIn(0f, 0.50f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Central Nothing-style Badge Card with clean checkmark glyph (NO particle effects)
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141417))
                .border(
                    width = 1.2.dp,
                    color = accentColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Iconic animated checkmark glyph circle
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .scale(checkIconScale)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title: ALLES ERLEDIGT! / ALL DONE!
            Text(
                text = AppStrings.get("celebration_title", language),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = headingFont,
                    fontSize = 22.sp,
                    letterSpacing = 0.08.sp
                ),
                color = Color(0xFFF5F5F7)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Streak Status Capsule: STREAK GESICHERT • X TAGE
            val streakText = String.format(
                AppStrings.get("celebration_streak_secured", language),
                streakDays.coerceAtLeast(1)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = streakText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = bodyFont,
                        fontSize = 12.sp,
                        letterSpacing = 0.04.sp
                    ),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dismiss hint
            Text(
                text = AppStrings.get("celebration_tap_to_dismiss", language),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = bodyFont,
                    fontSize = 9.sp,
                    letterSpacing = 0.04.sp
                ),
                color = Color(0xFF757580)
            )
        }
    }
}
