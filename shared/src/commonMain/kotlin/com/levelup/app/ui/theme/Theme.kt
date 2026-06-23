package com.levelup.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Dark, low-saturation cyan/violet palette - "system / hunter UI" without using
 * any copyrighted iconography. Pure Compose, so the same theme renders on iOS.
 */

val Void = Color(0xFF070912)
val Surface = Color(0xFF0E1322)
val SurfaceHigh = Color(0xFF161D33)
val AccentCyan = Color(0xFF5BE9FF)
val AccentViolet = Color(0xFFA47BFF)
val AccentGold = Color(0xFFFFD66B)
val TextPrimary = Color(0xFFE6ECFF)
val TextMuted = Color(0xFF9AA3BF)
val HpRed = Color(0xFFFF6A6A)

private val HunterDark = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Void,
    secondary = AccentViolet,
    onSecondary = Void,
    tertiary = AccentGold,
    onTertiary = Void,
    background = Void,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextMuted,
    error = HpRed,
    onError = Void
)

private val LevelUpTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
)

@Composable
fun LevelUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HunterDark,
        typography = LevelUpTypography,
        content = content
    )
}
