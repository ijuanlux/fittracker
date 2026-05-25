package com.juan.fittracker.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private val PartyColors = listOf(
    Color(0xFFFCD116), // Colombia yellow
    Color(0xFF003893), // Colombia blue
    Color(0xFFCE1126), // Colombia red
    Color(0xFFFFC58A), // Accent
    Color(0xFFFFFFFF), // White
    Color(0xFF7BD389), // Soft green
)

private data class Particle(
    val color: Color,
    val startX: Float,        // fraction of width [0..1]
    val startY: Float,        // fraction of height (negative = above screen)
    val vx: Float,            // horizontal velocity in fractions/s
    val vy: Float,            // initial vertical velocity in fractions/s
    val gravity: Float,       // gravity multiplier
    val sizeDp: Float,
    val initialRotation: Float,
    val rotationSpeed: Float, // deg/s
)

@Composable
fun ConfettiOverlay(active: Boolean, onFinish: () -> Unit) {
    if (!active) return
    val particles = remember {
        List(60) {
            Particle(
                color = PartyColors.random(),
                startX = Random.nextFloat(),
                startY = -0.10f - Random.nextFloat() * 0.20f,
                vx = (Random.nextFloat() - 0.5f) * 0.35f,
                vy = 0.05f + Random.nextFloat() * 0.20f,
                gravity = 0.6f + Random.nextFloat() * 0.5f,
                sizeDp = 6f + Random.nextFloat() * 10f,
                initialRotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
            )
        }
    }
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    val durationMs = 3200f

    LaunchedEffect(Unit) {
        var start = -1L
        while (elapsedMs < durationMs) {
            withFrameMillis { frame ->
                if (start < 0L) start = frame
                elapsedMs = (frame - start).toFloat()
            }
        }
        onFinish()
    }

    val seconds = elapsedMs / 1000f
    val fadeOutStart = 0.7f
    val fadeAlpha = if (elapsedMs / durationMs < fadeOutStart) 1f
    else 1f - ((elapsedMs / durationMs - fadeOutStart) / (1f - fadeOutStart))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pxPerDp = density
        particles.forEach { p ->
            val x = p.startX * w + p.vx * w * seconds
            val y = p.startY * h +
                p.vy * h * seconds +
                0.5f * p.gravity * 1400f * seconds * seconds
            if (y > h + 80f) return@forEach
            val rot = p.initialRotation + p.rotationSpeed * seconds
            val sizePx = p.sizeDp * pxPerDp
            rotate(rot, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = fadeAlpha),
                    topLeft = Offset(x - sizePx / 2f, y - sizePx * 0.3f),
                    size = Size(sizePx, sizePx * 0.6f),
                )
            }
        }
    }
}
