package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun t(key: String): String = AppStrings.get(key, language)

    val targetSeconds = remember(task.durationMinutes) {
        (task.durationMinutes.coerceAtLeast(1)) * 60
    }
    val totalSeconds = targetSeconds
    var remainingSeconds by remember { mutableIntStateOf(targetSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    // Hold-to-complete state (3 seconds hold)
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdSecondsRemaining by remember { mutableIntStateOf(3) }
    val holdDurationMs = 3000L

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                isRunning = false
                isFinished = true
                HapticUtils.vibrateSuccess(context)
                onCompleted()
            }
        }
    }

    val progress = if (totalSeconds > 0) {
        1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
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
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TaskIcons.getIcon(task.iconName),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = t("timer_title"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = headingFont,
                                letterSpacing = 1.sp
                            ),
                            color = accentColor
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = bodyFont,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = t("timer_close"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Circular Timer Display
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(240.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 10.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )
                // Active Progress
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(240.dp),
                    color = accentColor,
                    strokeWidth = 10.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = headingFont,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFinished) t("timer_completed")
                        else if (isRunning) "${(progress * 100).toInt()}%"
                        else "${totalSeconds / 60} MIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = bodyFont,
                            letterSpacing = 1.sp
                        ),
                        color = if (isFinished) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Controls (Reset & Start/Pause)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button
                IconButton(
                    onClick = {
                        isRunning = false
                        isFinished = false
                        remainingSeconds = totalSeconds
                        HapticUtils.vibrateTick(context)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = t("timer_reset"),
                        tint = MaterialTheme.colorScheme.onSurface
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
                        contentColor = if (accentColor == Color(0xFFF5F5F7)) Color.Black else Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isRunning) t("timer_pause") else t("timer_start"),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = headingFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-SECOND HOLD TO COMPLETE BUTTON (Prevents Accidental or Shortcut Check-Off)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (isHolding) accentColor else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(task.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isHolding = true
                            holdProgress = 0f
                            holdSecondsRemaining = 3

                            val holdJob = coroutineScope.launch {
                                val startTime = System.currentTimeMillis()
                                var lastSecondTicked = 3

                                while (isHolding) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val prog = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                                    holdProgress = prog
                                    val remSec = (3 - (elapsed / 1000L).toInt()).coerceAtLeast(0)
                                    holdSecondsRemaining = remSec

                                    if (remSec < lastSecondTicked && remSec > 0) {
                                        lastSecondTicked = remSec
                                        HapticUtils.vibrateHoldTick(context)
                                    }

                                    if (elapsed >= holdDurationMs) {
                                        isHolding = false
                                        holdProgress = 1f
                                        isRunning = false
                                        isFinished = true
                                        HapticUtils.vibrateSuccess(context)
                                        onCompleted()
                                        onDismiss()
                                        break
                                    }
                                    delay(40L)
                                }
                            }

                            val up = waitForUpOrCancellation()
                            if (up == null || holdProgress < 1f) {
                                isHolding = false
                                holdJob.cancel()
                                holdProgress = 0f
                                holdSecondsRemaining = 3
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background fill progress indicator
                if (holdProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(holdProgress)
                            .height(52.dp)
                            .align(Alignment.CenterStart)
                            .background(accentColor.copy(alpha = 0.25f))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isHolding) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isHolding) {
                            String.format(t("timer_holding"), holdSecondsRemaining)
                        } else {
                            t("timer_hold_to_complete")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = headingFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isHolding) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
