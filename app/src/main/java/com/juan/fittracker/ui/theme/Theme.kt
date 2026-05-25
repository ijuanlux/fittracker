package com.juan.fittracker.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FitTrackerTheme(
    themeMode: AppThemeMode = AppThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val appColors = colorsFor(themeMode)
    val colorScheme = when (themeMode) {
        AppThemeMode.Light -> lightColorScheme(
            background = appColors.background,
            surface = appColors.background,
            primary = appColors.accent,
            secondary = appColors.accent,
            onBackground = appColors.onSurface,
            onSurface = appColors.onSurface,
            onPrimary = Color.White,
            error = appColors.danger,
        )
        else -> darkColorScheme(
            background = appColors.background,
            surface = appColors.background,
            primary = appColors.accent,
            secondary = appColors.accent,
            onBackground = appColors.onSurface,
            onSurface = appColors.onSurface,
            onPrimary = Color(0xFF15100B),
            error = appColors.danger,
        )
    }
    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalThemeMode provides themeMode,
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Box(modifier = Modifier.fillMaxSize().background(appColors.background)) {
                if (appColors.drawChipPattern) {
                    GalletoChipPattern()
                }
                content()
            }
        }
    }
}

@Composable
private fun GalletoChipPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rng = Random(42)
        val chipDark = Color(0xFF1F1108)
        val chipMid = Color(0xFF2E1A0E)
        val crack = Color(0xFF301B0E).copy(alpha = 0.5f)
        val highlight = Color(0xFFE8B47E).copy(alpha = 0.10f)

        // Lighter cookie speckles (small)
        repeat(140) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            val r = 1.5f + rng.nextFloat() * 2.5f
            drawCircle(color = highlight, radius = r, center = Offset(x, y))
        }
        // Bigger chocolate chips
        repeat(50) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            val r = 8f + rng.nextFloat() * 14f
            val color = if (rng.nextBoolean()) chipDark else chipMid
            drawCircle(color = color.copy(alpha = 0.65f), radius = r, center = Offset(x, y))
            // Tiny shine on chip
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = r * 0.30f,
                center = Offset(x - r * 0.30f, y - r * 0.30f),
            )
        }
        // Crack lines (texture)
        repeat(12) {
            val x0 = rng.nextFloat() * size.width
            val y0 = rng.nextFloat() * size.height
            val angle = rng.nextFloat() * Math.PI.toFloat() * 2f
            val len = 40f + rng.nextFloat() * 110f
            val x1 = x0 + cos(angle) * len
            val y1 = y0 + sin(angle) * len
            drawLine(
                color = crack,
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round,
            )
        }
    }
}
