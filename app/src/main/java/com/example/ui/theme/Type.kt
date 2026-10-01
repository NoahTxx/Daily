package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val GeistFontFamily = FontFamily(
    Font(R.font.geist, FontWeight.Normal),
    Font(R.font.geist, FontWeight.Medium),
    Font(R.font.geist, FontWeight.SemiBold),
    Font(R.font.geist, FontWeight.Bold)
)

val GeistMonoFontFamily = FontFamily(
    Font(R.font.geist_mono, FontWeight.Normal),
    Font(R.font.geist_mono, FontWeight.Medium),
    Font(R.font.geist_mono, FontWeight.SemiBold),
    Font(R.font.geist_mono, FontWeight.Bold)
)

val NDotFontFamily = FontFamily(
    Font(R.font.doto, FontWeight.Normal),
    Font(R.font.doto, FontWeight.Bold)
)

val SilkscreenFontFamily = FontFamily(
    Font(R.font.silkscreen, FontWeight.Normal),
    Font(R.font.silkscreen, FontWeight.Bold)
)

val DotGothicFontFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal)
)

// Alias for compatibility
val DotMatrixFontFamily = NDotFontFamily

fun createAppTypography(
    headingFont: FontFamily = NDotFontFamily,
    bodyFont: FontFamily = GeistFontFamily
): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            letterSpacing = 1.sp
        ),
        displayMedium = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = 1.5.sp
        ),
        displaySmall = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = 1.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 2.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            letterSpacing = 2.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 1.sp
        ),
        titleLarge = TextStyle(
            fontFamily = headingFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.5.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.3.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.8.sp
        )
    )
}

val Typography = createAppTypography()
