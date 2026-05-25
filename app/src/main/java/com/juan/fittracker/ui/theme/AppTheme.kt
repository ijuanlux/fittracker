package com.juan.fittracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
    Dark("Oscuro"),
    Light("Día"),
    Galleto("Galleto"),
    ;
}

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val accent: Color,
    val danger: Color,
    val navBar: Color,
    val drawChipPattern: Boolean,
)

private val DarkColors = AppColors(
    background = Color(0xFF15100B),
    surface = Color.White.copy(alpha = 0.05f),
    onSurface = Color(0xFFEDE3D6),
    accent = Color(0xFFFFC58A),
    danger = Color(0xFFFF8A80),
    navBar = Color(0xFF1A1310),
    drawChipPattern = false,
)

private val LightColors = AppColors(
    background = Color(0xFFFFF6E2),
    surface = Color(0xFF2A1F18).copy(alpha = 0.07f),
    onSurface = Color(0xFF2A1F18),
    accent = Color(0xFF8B5A2B),
    danger = Color(0xFFC62828),
    navBar = Color(0xFFEAD6B0),
    drawChipPattern = false,
)

private val GalletoColors = AppColors(
    background = Color(0xFF4A2E1B),
    surface = Color(0xFF6B4423).copy(alpha = 0.45f),
    onSurface = Color(0xFFFFE9C7),
    accent = Color(0xFFFFC58A),
    danger = Color(0xFFFF8A80),
    navBar = Color(0xFF36200F),
    drawChipPattern = true,
)

fun colorsFor(mode: AppThemeMode): AppColors = when (mode) {
    AppThemeMode.Dark -> DarkColors
    AppThemeMode.Light -> LightColors
    AppThemeMode.Galleto -> GalletoColors
}

val LocalAppColors = compositionLocalOf { DarkColors }
val LocalThemeMode = compositionLocalOf { AppThemeMode.Dark }
