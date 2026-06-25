package com.sudugu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sudugu.app.model.ThemeMode

object AppColors {
    val lightPrimary = Color(0xFF4A90D9)
    val lightBackground = Color(0xFFF5F5F5)
    val lightSurface = Color(0xFFFFFFFF)
    val lightOnSurface = Color(0xFF333333)
    val lightOnSurfaceVariant = Color(0xFF666666)
    val lightOutline = Color(0xFFE0E0E0)
    val lightReaderBg = Color(0xFFF5F0E8)
    val lightReaderText = Color(0xFF333333)
    val lightError = Color(0xFFE74C3C)
    val lightSuccess = Color(0xFF27AE60)

    val darkPrimary = Color(0xFF5A9FE8)
    val darkBackground = Color(0xFF1A1A1A)
    val darkSurface = Color(0xFF2A2A2A)
    val darkOnSurface = Color(0xFFE0E0E0)
    val darkOnSurfaceVariant = Color(0xFF999999)
    val darkOutline = Color(0xFF404040)
    val darkReaderBg = Color(0xFF1A1A1A)
    val darkReaderText = Color(0xFFC0C0C0)
    val darkError = Color(0xFFE74C3C)
    val darkSuccess = Color(0xFF27AE60)
}

private val LightColors = lightColorScheme(
    primary = AppColors.lightPrimary,
    background = AppColors.lightBackground,
    surface = AppColors.lightSurface,
    onSurface = AppColors.lightOnSurface,
    onSurfaceVariant = AppColors.lightOnSurfaceVariant,
    outline = AppColors.lightOutline,
    error = AppColors.lightError,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.darkPrimary,
    background = AppColors.darkBackground,
    surface = AppColors.darkSurface,
    onSurface = AppColors.darkOnSurface,
    onSurfaceVariant = AppColors.darkOnSurfaceVariant,
    outline = AppColors.darkOutline,
    error = AppColors.darkError,
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

@Composable
fun SuduguTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = mode == ThemeMode.DARK || (mode == ThemeMode.LIGHT && systemDark && false)
    // Note: we don't auto-switch on system theme; user toggle controls it.
    val colors = if (mode == ThemeMode.DARK) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
