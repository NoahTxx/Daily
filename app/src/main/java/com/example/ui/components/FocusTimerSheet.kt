package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyTask
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.HapticUtils
import com.example.util.TaskIcons
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerSheet(
    task: DailyTask,
    accentColor: Color,
    headingFont: FontFamily,
    bodyFont: FontFamily,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun t(key: String): String = AppStrings.get(key, language)

    val targetSeconds = remember(task.durationMinutes) {
        (task.durationMinutes.coerceAtLeast(1)) * 60
    }
    var remainingSeconds by rememberSaveable(task.id) { mutableIntStateOf(targetSeconds) }
    var isRunning by rememberSaveable(task.id) { mutableStateOf(value = false) }
    var isFinished by rememberSaveable(task.id) { mutableStateOf(value = false) }

    // Hold-to-complete state (3 seconds hold)
    val holdProgressAnim = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(value = false) }
    var holdSecondsRemaining by remember { mutableIntStateOf(3) }

    // Reset double-tap confirmation state
    var resetConfirmPending by remember { mutableStateOf(value = false) }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && (remainingSeconds > 0)) {
            delay(1.seconds)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                isRunning = false
                isFinished = true
                HapticUtils.vibrateSuccess(context)
                onCompleted()
            }
        }
    }

    val progress = if (targetSeconds > 0) {
        1f - (remainingSeconds.toFloat() / targetSeconds.toFloat())
    } else 0f

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TaskIcons.getIcon(task.iconName),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = t("timer_title"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = headingFont,
                                letterSpacing = 1.sp,
                            ),
                            color = accentColor,
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = bodyFont,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = t("timer_close"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Circular Timer Display
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(240.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 10.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                )
                // Active Progress
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(240.dp),
                    color = accentColor,
                    strokeWidth = 10.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = headingFont,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFinished) t("timer_completed")
                        else if (isRunning) "${(progress * 100).toInt()}%"
                        else "${targetSeconds / 60} MIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = bodyFont,
                            letterSpacing = 1.sp,
                        ),
                        color = if (isFinished) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Controls (Reset & Start/Pause)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Reset button with double-tap / confirmation protection (clean custom Box without M3 circle ripple silhouette)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (resetConfirmPending) 2.dp else 1.dp,
                            color = if (resetConfirmPending) accentColor else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(
                            if (resetConfirmPending) accentColor.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!resetConfirmPending) {
                                resetConfirmPending = true
                                HapticUtils.vibrateTick(context)
                                resetJob?.cancel()
                                resetJob = coroutineScope.launch {
                                    delay(2.seconds)
                                    resetConfirmPending = false
                                }
                            } else {
                                resetJob?.cancel()
                                resetConfirmPending = false
                                isRunning = false
                                isFinished = false
                                remainingSeconds = targetSeconds
                                HapticUtils.vibrateSuccess(context)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (resetConfirmPending) t("timer_reset_confirm") else t("timer_reset"),
                        tint = if (resetConfirmPending) accentColor else MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Start / Pause button
                Button(
                    onClick = {
                        isRunning = !isRunning
                        HapticUtils.vibrateTick(context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = if (isRunning) t("timer_pause") else t("timer_start"),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = headingFont,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Discreet Fallback: Hold to complete ("Schon erledigt")
            val holdProg = holdProgressAnim.value.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = if (isHolding) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .background(
                        if (isHolding) accentColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .drawWithContent {
                        if (holdProg > 0f) {
                            drawRect(
                                color = accentColor.copy(alpha = 0.28f),
                                size = Size(
                                    width = size.width * holdProg,
                                    height = size.height,
                                ),
                            )
                        }
                        drawContent()
                    }
                    .pointerInput(task.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val pointerId = down.id
                            isHolding = true
                            holdSecondsRemaining = 3

                            var isCompleted = false
                            val holdJob = coroutineScope.launch {
                                holdProgressAnim.snapTo(0f)
                                var lastSecondTicked = 3

                                val animJob = launch {
                                    holdProgressAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
                                    )
                                }

                                val tickJob = launch {
                                    while (isActive) {
                                        val currentProg = holdProgressAnim.value
                                        val remSec = ceil((1f - currentProg) * 3f).toInt().coerceAtLeast(0)
                                        holdSecondsRemaining = remSec
                                        if (remSec in (1 until lastSecondTicked)) {
                                            lastSecondTicked = remSec
                                            HapticUtils.vibrateHoldTick(context)
                                        }
                                        if (currentProg >= 1f) {
                                            break
                                        }
                                        delay(16.milliseconds)
                                    }
                                }

                                animJob.join()
                                tickJob.cancel()

                                if (holdProgressAnim.value >= 1f) {
                                    isCompleted = true
                                    isHolding = false
                                    isRunning = false
                                    isFinished = true
                                    HapticUtils.vibrateSuccess(context)
                                    onCompleted()
                                    onDismiss()
                                }
                            }

                            // Track pointer continuously; consume moves to prevent bottom sheet from stealing touch
                            var isUp = false
                            while (!isUp && !isCompleted) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if ((change == null) || !change.pressed) {
                                    isUp = true
                                } else {
                                    change.consume()
                                }
                            }

                            if (!isCompleted) {
                                isHolding = false
                                holdJob.cancel()
                                coroutineScope.launch {
                                    holdProgressAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    )
                                    holdSecondsRemaining = 3
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isHolding) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (isHolding) {
                            String.format(t("timer_holding"), holdSecondsRemaining)
                        } else {
                            t("timer_hold_to_complete")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = bodyFont,
                            fontSize = 11.sp,
                            fontWeight = if (isHolding) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 0.4.sp,
                        ),
                        color = if (isHolding) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
