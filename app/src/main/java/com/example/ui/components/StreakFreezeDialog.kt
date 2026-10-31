package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.DateUtils
import com.example.util.HapticUtils
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakFreezeDialog(
    accentColor: Color,
    headingFont: FontFamily,
    bodyFont: FontFamily,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmFreeze: (startDate: String, endDate: String, type: String, note: String) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    fun t(key: String): String = AppStrings.get(key, language)

    val todayKey = remember { DateUtils.getTodayKey() }
    var selectedType by remember { mutableStateOf("SICKNESS") } // "SICKNESS" vs "VACATION"
    var durationPreset by remember { mutableIntStateOf(3) } // 1, 3, 7, 0 (custom)
    var customStartDate by remember { mutableStateOf(todayKey) }
    var customEndDate by remember { mutableStateOf(DateUtils.addDays(todayKey, 6)) }

    val computedEndDate = remember(durationPreset, todayKey, customEndDate) {
        when (durationPreset) {
            1 -> todayKey
            3 -> DateUtils.addDays(todayKey, 2)
            7 -> DateUtils.addDays(todayKey, 6)
            else -> customEndDate
        }
    }

    // 5-second Hold button state
    val holdProgressAnim = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(value = false) }
    var holdSecondsRemaining by remember { mutableIntStateOf(5) }
    var wasCancelledEarly by remember { mutableStateOf(value = false) }

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
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("freeze_dialog_title"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = headingFont,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reason / Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "SICKNESS" to t("freeze_type_sickness"),
                    "VACATION" to t("freeze_type_vacation"),
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = headingFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                            ),
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Duration selector
            Text(
                text = "ZEITRAUM / DAUER",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    1 to t("freeze_duration_1d"),
                    3 to t("freeze_duration_3d"),
                    7 to t("freeze_duration_7d"),
                    0 to t("freeze_duration_custom"),
                ).forEach { (preset, label) ->
                    val isSel = durationPreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) accentColor else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .background(
                                if (isSel) accentColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { durationPreset = preset }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = bodyFont,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                            ),
                            color = if (isSel) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Custom Date Picker range
            if (durationPreset == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val c = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        customStartDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                    },
                                    c[Calendar.YEAR],
                                    c[Calendar.MONTH],
                                    c[Calendar.DAY_OF_MONTH],
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = String.format(t("freeze_date_from"), customStartDate),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val c = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val picked = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                        if (picked >= customStartDate) {
                                            customEndDate = picked
                                        }
                                    },
                                    c[Calendar.YEAR],
                                    c[Calendar.MONTH],
                                    c[Calendar.DAY_OF_MONTH],
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = String.format(t("freeze_date_to"), customEndDate),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // THE PROVOCATIVE / HONEST NOTHING WARNING BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(18.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = t("freeze_warning_header"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = headingFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = accentColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = t("freeze_warning_body"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = bodyFont,
                            lineHeight = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 5-SECOND HOLD TO CONFIRM BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isHolding) accentColor else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(selectedType, computedEndDate) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val pointerId = down.id
                            isHolding = true
                            wasCancelledEarly = false
                            holdSecondsRemaining = 5

                            var isCompleted = false
                            val holdJob = coroutineScope.launch {
                                holdProgressAnim.snapTo(0f)
                                var lastSecondTicked = 5

                                val animJob = launch {
                                    holdProgressAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
                                    )
                                }

                                val tickJob = launch {
                                    while (isActive) {
                                        val currentProg = holdProgressAnim.value
                                        val remSec = ceil((1f - currentProg) * 5f).toInt().coerceAtLeast(0)
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
                                    HapticUtils.vibrateSuccess(context)
                                    val start = if (durationPreset == 0) customStartDate else todayKey
                                    onConfirmFreeze(start, computedEndDate, selectedType, "")
                                    onDismiss()
                                }
                            }

                            // Track pointer continuously; consume moves to prevent parent verticalScroll from stealing touch
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
                                val finalProg = holdProgressAnim.value
                                if (finalProg in (0.05f..0.99f)) {
                                    wasCancelledEarly = true
                                    HapticUtils.vibrateTick(context)
                                }
                                coroutineScope.launch {
                                    holdProgressAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    )
                                    holdSecondsRemaining = 5
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                // Smooth animated progress fill inside button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(holdProgressAnim.value.coerceIn(0f, 1f))
                        .background(accentColor),
                )

                // Button text & live countdown
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isHolding) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (isHolding) {
                                String.format(t("freeze_hold_counter"), holdSecondsRemaining)
                            } else if (wasCancelledEarly) {
                                t("freeze_hold_cancelled")
                            } else {
                                t("freeze_hold_to_confirm")
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = headingFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = if (isHolding) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
