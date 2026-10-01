package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.AppStrings
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private data class DotParticle(
    val angle: Double,
    val startRadius: Float,
    val maxDistance: Float,
    val radius: Float,
    val colorType: Int, // 0 = accent, 1 = white, 2 = muted gray
    val isSquare: Boolean
)

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
                animationSpec = tween(durationMillis = 2300, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    // 72 omnidirectional dot particles radiating outward evenly in 360° (up, down, left, right, diagonals)
    val particles = remember {
        List(72) { index ->
            // Distribute base angle uniformly around 360 degrees so all quadrants are equally populated
            val baseAngle = (index.toDouble() / 72.0) * (Math.PI * 2)
            val jitter = Random.nextDouble(-0.08, 0.08)
            val angle = baseAngle + jitter
            val startDist = Random.nextFloat() * 70f + 60f
            val maxTravel = Random.nextFloat() * 320f + 160f
            val size = Random.nextFloat() * 2.2f + 2.2f
            val colorType = when {
                index % 3 == 0 -> 0 // Accent color
                index % 3 == 1 -> 1 // Crisp white
                else -> 2           // Neutral light slate
            }
            DotParticle(
                angle = angle,
                startRadius = startDist,
                maxDistance = maxTravel,
                radius = size,
                colorType = colorType,
                isSquare = (index % 2 == 0)
            )
        }
    }

    val progress = animProgress.value

    // Instant, snappy entrance: text is visible right away without lag or delay
    // Hold: 0f -> 0.85f (stay crisp and visible)
    // Exit: 0.85f -> 1.0f (last ~340ms): smooth fade out
    val exitT = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)

    // Snappy, clean scale curve
    val enterScale = (progress / 0.08f).coerceIn(0f, 1f)
    val cardScale = (0.95f + (0.05f * enterScale)) * (1f - 0.05f * exitT)
    val cardAlpha = (1f - exitT)
    val scrimAlpha = (0.75f * (1f - exitT)).coerceIn(0f, 0.75f)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
        // Full-screen Omnidirectional Dot-Matrix Particles (radiating outward strictly behind and outside the card)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // Half-dimensions of the central card plus generous margin so particles NEVER render on/inside the text card
            val cardHalfW = (165.dp).toPx()
            val cardHalfH = (115.dp).toPx()

            // Smooth explosive deceleration: expands fast then softly glides
            val particleProgress = (1f - (1f - progress).pow(2.2f))
            // Fade out smoothly towards the end of the explosion
            val particleAlpha = (1f - (progress * 1.15f)).coerceIn(0f, 1f)

            if (particleAlpha > 0.01f) {
                drawContext.canvas.save()
                drawContext.canvas.clipRect(
                    centerX - cardHalfW,
                    centerY - cardHalfH,
                    centerX + cardHalfW,
                    centerY + cardHalfH,
                    androidx.compose.ui.graphics.ClipOp.Difference
                )

                particles.forEach { p ->
                    val cosA = cos(p.angle).toFloat()
                    val sinA = sin(p.angle).toFloat()

                    // Minimum clearance distance at angle p.angle to start outside the card perimeter
                    val rX = if (kotlin.math.abs(cosA) > 0.001f) kotlin.math.abs(cardHalfW / cosA) else Float.MAX_VALUE
                    val rY = if (kotlin.math.abs(sinA) > 0.001f) kotlin.math.abs(cardHalfH / sinA) else Float.MAX_VALUE
                    val cardClearance = kotlin.math.min(rX, rY) + 20f

                    val distance = cardClearance + (p.maxDistance * particleProgress)
                    val x = centerX + (cosA * distance)
                    val y = centerY + (sinA * distance)

                    val dotColor = when (p.colorType) {
                        0 -> accentColor.copy(alpha = particleAlpha)
                        1 -> Color(0xFFF5F5F7).copy(alpha = particleAlpha * 0.95f)
                        else -> Color(0xFF888894).copy(alpha = particleAlpha * 0.85f)
                    }

                    if (p.isSquare) {
                        drawRect(
                            color = dotColor,
                            topLeft = Offset(x - p.radius, y - p.radius),
                            size = androidx.compose.ui.geometry.Size(p.radius * 2, p.radius * 2)
                        )
                    } else {
                        drawCircle(
                            color = dotColor,
                            radius = p.radius,
                            center = Offset(x, y)
                        )
                    }
                }

                drawContext.canvas.restore()
            }
        }

        // Central Nothing-style Badge Card strictly in front with instant visibility
        Column(
            modifier = Modifier
                .zIndex(10f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161619))
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = borderPulse),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Dot Matrix indicator row: ● ● ● 100% ● ● ●
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = headingFont,
                        fontSize = 11.sp,
                        letterSpacing = 0.1.sp
                    ),
                    color = accentColor
                )

                repeat(3) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title: ALLES ERLEDIGT! / ALL DONE!
            Text(
                text = AppStrings.get("celebration_title", language),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = headingFont,
                    fontSize = 24.sp,
                    letterSpacing = 0.08.sp
                ),
                color = Color(0xFFF5F5F7)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtext: STREAK GESICHERT • X TAGE
            val streakText = String.format(
                AppStrings.get("celebration_streak_secured", language),
                streakDays.coerceAtLeast(1)
            )
            Text(
                text = streakText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    letterSpacing = 0.06.sp
                ),
                color = accentColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Subtle dismiss pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF232328))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = AppStrings.get("celebration_tap_to_dismiss", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = bodyFont,
                        fontSize = 9.sp,
                        letterSpacing = 0.05.sp
                    ),
                    color = Color(0xFF888894)
                )
            }
        }
    }
}
