package com.juan.fittracker.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.juan.fittracker.data.CookieMood
import kotlin.math.cos
import kotlin.math.sin

internal val CookieLight = Color(0xFFD9A86C)
internal val CookieDark = Color(0xFFB8814A)
internal val ChipDark = Color(0xFF3D2418)
private val BarMetal = Color(0xFFBDBDBD)
private val PlateDark = Color(0xFF2E2E2E)
private val ColombiaYellow = Color(0xFFFCD116)
private val ColombiaBlue = Color(0xFF003893)
private val ColombiaRed = Color(0xFFCE1126)
private val MouthInterior = Color(0xFF4A1F0E)
private val Blush = Color(0xFFFF8A8A)

enum class CookieAccessory {
    None, MagnifyingGlass
}

@Composable
fun CookieAvatar(
    modifier: Modifier = Modifier,
    mood: CookieMood = CookieMood.Happy,
    animated: Boolean = true,
    isSpeaking: Boolean = false,
    accessory: CookieAccessory = CookieAccessory.None,
    level: Int = 1,
) {
    val resting = mood == CookieMood.Sleepy || mood == CookieMood.Lazy || mood == CookieMood.Stuffed
    val animatesArms = animated && !resting
    val duration = when (mood) {
        CookieMood.Energetic -> 380
        CookieMood.Happy -> 480
        CookieMood.Neutral -> 650
        CookieMood.Tired -> 950
        CookieMood.Sleepy, CookieMood.Lazy, CookieMood.Stuffed -> 1200
    }
    val bounceTarget = when (mood) {
        CookieMood.Energetic -> 1.06f
        CookieMood.Happy -> 1.04f
        CookieMood.Neutral -> 1.025f
        CookieMood.Tired -> 1.015f
        CookieMood.Stuffed -> 1.05f
        CookieMood.Sleepy, CookieMood.Lazy -> 1.01f
    }

    val transition = rememberInfiniteTransition(label = "cookie")
    val curl by transition.animateFloat(
        initialValue = if (animatesArms) 0f else 0f,
        targetValue = if (animatesArms) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "curl",
    )
    val bounce by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) bounceTarget else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )
    val mouthAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSpeaking && animated) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mouth",
    )
    val auraPulse by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura",
    )
    val capeWave by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cape",
    )
    // Blink: most of the time eyes are open (0); periodically blinks closed (1)
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4200
                0f at 0
                0f at 3800
                1f at 3960
                0f at 4120
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "blink",
    )
    // Subtle sway side to side, like breathing weight shift
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )

    val stuffScale = if (mood == CookieMood.Stuffed) 1.07f else 1f

    Canvas(modifier = modifier.scale(bounce * stuffScale)) {
        drawCookieAthlete(curl, mood, resting, mouthAnim, accessory, level, auraPulse, capeWave, blink, sway)
    }
}

private fun DrawScope.drawCookieAthlete(
    curl: Float,
    mood: CookieMood,
    resting: Boolean,
    mouthAnim: Float,
    accessory: CookieAccessory,
    level: Int,
    auraPulse: Float,
    capeWave: Float,
    blink: Float = 0f,
    sway: Float = 0f,
) {
    val swayOffset = sway * size.minDimension * 0.005f
    val center = Offset(size.width / 2f + swayOffset, size.height / 2f)
    val r = size.minDimension * 0.28f

    // Aura (lv 9+) - behind everything
    if (level >= 9) drawAura(center, r, auraPulse)
    // Cape (lv 7+) - behind body
    if (level >= 7) drawCape(center, r, capeWave)

    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = r * 1.06f,
        center = center + Offset(0f, r * 0.08f),
    )
    drawCircle(color = CookieLight, radius = r, center = center)
    drawCircle(
        color = CookieDark,
        radius = r * 0.93f,
        center = center,
        style = Stroke(width = r * 0.04f),
    )

    val cookiePath = Path().apply {
        addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    clipPath(cookiePath) {
        val shirtTop = center.y + r * 0.05f
        val shirtBottom = center.y + r
        val shirtH = shirtBottom - shirtTop
        val yellowH = shirtH * 0.50f
        val blueH = shirtH * 0.25f
        val redH = shirtH * 0.25f
        val left = center.x - r
        val width = 2f * r
        drawRect(color = ColombiaYellow, topLeft = Offset(left, shirtTop), size = Size(width, yellowH))
        drawRect(color = ColombiaBlue, topLeft = Offset(left, shirtTop + yellowH), size = Size(width, blueH))
        drawRect(color = ColombiaRed, topLeft = Offset(left, shirtTop + yellowH + blueH), size = Size(width, redH))
    }
    drawArc(
        color = CookieDark,
        startAngle = 190f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(center.x - r * 0.95f, center.y - r * 0.55f),
        size = Size(r * 1.9f, r * 1.2f),
        style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
    )

    val chips = listOf(
        Triple(-0.55f, -0.45f, 0.075f),
        Triple(0.50f, -0.50f, 0.07f),
        Triple(-0.30f, -0.62f, 0.06f),
        Triple(0.25f, -0.65f, 0.055f),
        Triple(-0.10f, -0.70f, 0.05f),
        Triple(0.10f, -0.55f, 0.045f),
        Triple(-0.45f, -0.62f, 0.045f),
        Triple(0.45f, -0.40f, 0.055f),
        Triple(-0.60f, -0.30f, 0.045f),
        Triple(0.60f, -0.32f, 0.05f),
        Triple(0.0f, -0.48f, 0.045f),
        Triple(-0.20f, -0.55f, 0.045f),
        Triple(-0.35f, -0.40f, 0.04f),
        Triple(0.35f, -0.55f, 0.04f),
    )
    val chipHighlight = Color(0xFF6B3D26).copy(alpha = 0.55f)
    chips.forEach { (x, y, rad) ->
        val cx = center.x + x * r
        val cy = center.y + y * r
        val chipR = r * rad
        drawCircle(color = ChipDark, radius = chipR, center = Offset(cx, cy))
        // Subtle highlight for chocolate shine
        drawCircle(
            color = chipHighlight,
            radius = chipR * 0.45f,
            center = Offset(cx - chipR * 0.35f, cy - chipR * 0.35f),
        )
    }

    if (mood == CookieMood.Stuffed) {
        // Chubby cheek puffs
        val cheekY = r * 0.02f
        val cheekX = r * 0.88f
        val cheekR = r * 0.20f
        drawCircle(color = CookieLight, radius = cheekR, center = center + Offset(-cheekX, cheekY))
        drawCircle(color = CookieLight, radius = cheekR, center = center + Offset(cheekX, cheekY))
        drawCircle(color = CookieDark, radius = cheekR, center = center + Offset(-cheekX, cheekY), style = Stroke(width = r * 0.03f))
        drawCircle(color = CookieDark, radius = cheekR, center = center + Offset(cheekX, cheekY), style = Stroke(width = r * 0.03f))
        // Pink blush
        drawCircle(color = Blush.copy(alpha = 0.55f), radius = cheekR * 0.55f, center = center + Offset(-cheekX, cheekY))
        drawCircle(color = Blush.copy(alpha = 0.55f), radius = cheekR * 0.55f, center = center + Offset(cheekX, cheekY))
    }

    drawFace(mood, center, r, mouthAnim, level, blink)
    if (level >= 3) drawSunglasses(center, r)
    if (level >= 5) drawChain(center, r)
    when {
        level >= 8 -> drawCrown(center, r)
        level >= 4 -> drawCap(center, r)
        level >= 2 -> drawHeadband(center, r)
    }

    val shoulderY = r * 0.22f
    val leftShoulder = center + Offset(-r * 0.92f, shoulderY)
    val rightShoulder = center + Offset(r * 0.92f, shoulderY)

    if (resting) {
        val armLength = r * 0.7f
        drawLine(
            color = CookieLight,
            start = leftShoulder,
            end = leftShoulder + Offset(-r * 0.10f, armLength),
            strokeWidth = r * 0.14f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = CookieLight,
            start = rightShoulder,
            end = rightShoulder + Offset(r * 0.10f, armLength),
            strokeWidth = r * 0.14f,
            cap = StrokeCap.Round,
        )
        if (mood == CookieMood.Sleepy) drawSleepyZ(center, r)
    } else {
        val rightAngleDeg = 35f - 95f * curl
        val leftAngleDeg = 180f - rightAngleDeg
        val armLength = r * 0.85f
        val rightHand = polarOffset(rightShoulder, rightAngleDeg, armLength)
        val leftHand = polarOffset(leftShoulder, leftAngleDeg, armLength)
        val armStroke = r * 0.14f
        drawLine(color = CookieLight, start = leftShoulder, end = leftHand, strokeWidth = armStroke, cap = StrokeCap.Round)
        drawLine(color = CookieLight, start = rightShoulder, end = rightHand, strokeWidth = armStroke, cap = StrokeCap.Round)
        val dbAngle = rightAngleDeg + 90f
        drawDumbbell(leftHand, r * 0.20f, -dbAngle)
        drawDumbbell(rightHand, r * 0.20f, dbAngle)
    }

    if (accessory == CookieAccessory.MagnifyingGlass) {
        drawMagnifyingGlass(center, r)
    }
}

private fun DrawScope.drawFace(mood: CookieMood, center: Offset, r: Float, mouthAnim: Float, level: Int = 1, blink: Float = 0f) {
    val eyeY = -r * 0.25f
    val isBlinking = blink > 0.5f && mood != CookieMood.Sleepy
    if (level >= 3) {
        // Eyes covered by sunglasses; skip drawing
    } else if (isBlinking) {
        // Drawn as closed eye lines for the brief blink frame
        drawLine(Color.Black, center + Offset(-r * 0.28f, eyeY), center + Offset(-r * 0.14f, eyeY),
            strokeWidth = r * 0.05f, cap = StrokeCap.Round)
        drawLine(Color.Black, center + Offset(r * 0.14f, eyeY), center + Offset(r * 0.28f, eyeY),
            strokeWidth = r * 0.05f, cap = StrokeCap.Round)
    } else when (mood) {
        CookieMood.Sleepy -> {
            drawLine(Color.Black, center + Offset(-r * 0.32f, eyeY), center + Offset(-r * 0.12f, eyeY),
                strokeWidth = r * 0.05f, cap = StrokeCap.Round)
            drawLine(Color.Black, center + Offset(r * 0.12f, eyeY), center + Offset(r * 0.32f, eyeY),
                strokeWidth = r * 0.05f, cap = StrokeCap.Round)
        }
        CookieMood.Tired, CookieMood.Stuffed -> {
            // Heavy droopy lids: horizontal line slightly curved DOWN at the ends
            drawArc(
                color = Color.Black,
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(center.x - r * 0.32f, eyeY - r * 0.04f),
                size = Size(r * 0.20f, r * 0.10f),
                style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.Black,
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(center.x + r * 0.12f, eyeY - r * 0.04f),
                size = Size(r * 0.20f, r * 0.10f),
                style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
            )
            // Tiny droplet/sweat for Tired (no for Stuffed - cheeks tell the story)
            if (mood == CookieMood.Tired) {
                drawCircle(
                    color = Color(0xFF8AB4F8).copy(alpha = 0.7f),
                    radius = r * 0.04f,
                    center = center + Offset(r * 0.40f, eyeY + r * 0.10f),
                )
            }
        }
        CookieMood.Lazy -> {
            drawCircle(Color.Black, radius = r * 0.05f, center = center + Offset(-r * 0.18f, eyeY))
            drawCircle(Color.Black, radius = r * 0.05f, center = center + Offset(r * 0.26f, eyeY))
        }
        else -> {
            drawCircle(Color.Black, radius = r * 0.06f, center = center + Offset(-r * 0.22f, eyeY))
            drawCircle(Color.Black, radius = r * 0.06f, center = center + Offset(r * 0.22f, eyeY))
            drawCircle(Color.White, radius = r * 0.02f, center = center + Offset(-r * 0.20f, eyeY - r * 0.02f))
            drawCircle(Color.White, radius = r * 0.02f, center = center + Offset(r * 0.24f, eyeY - r * 0.02f))
        }
    }

    val mouthY = -r * 0.05f
    if (mouthAnim > 0.05f) {
        // Animated speaking mouth: opens and closes
        val openH = r * (0.05f + 0.16f * mouthAnim)
        val openW = r * (0.18f + 0.06f * mouthAnim)
        drawOval(
            color = MouthInterior,
            topLeft = Offset(center.x - openW / 2f, mouthY + r * 0.06f - openH / 2f),
            size = Size(openW, openH),
        )
    } else {
        when (mood) {
            CookieMood.Energetic, CookieMood.Happy -> {
                drawLine(Color.Black,
                    center + Offset(-r * 0.14f, mouthY),
                    center + Offset(0f, mouthY + r * 0.10f),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round)
                drawLine(Color.Black,
                    center + Offset(0f, mouthY + r * 0.10f),
                    center + Offset(r * 0.14f, mouthY),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round)
            }
            CookieMood.Stuffed -> {
                // Small contented smile
                drawArc(
                    color = Color.Black,
                    startAngle = 0f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(center.x - r * 0.10f, mouthY + r * 0.02f),
                    size = Size(r * 0.20f, r * 0.10f),
                    style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
                )
            }
            CookieMood.Neutral -> {
                drawLine(Color.Black,
                    center + Offset(-r * 0.10f, mouthY + r * 0.05f),
                    center + Offset(r * 0.10f, mouthY + r * 0.05f),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round)
            }
            CookieMood.Tired, CookieMood.Sleepy, CookieMood.Lazy -> {
                drawLine(Color.Black,
                    center + Offset(-r * 0.12f, mouthY + r * 0.12f),
                    center + Offset(0f, mouthY + r * 0.05f),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round)
                drawLine(Color.Black,
                    center + Offset(0f, mouthY + r * 0.05f),
                    center + Offset(r * 0.12f, mouthY + r * 0.12f),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawSleepyZ(center: Offset, r: Float) {
    val origin = center + Offset(r * 0.6f, -r * 0.85f)
    val s = r * 0.18f
    drawLine(Color(0xFFEDE3D6), origin, origin + Offset(s, 0f),
        strokeWidth = r * 0.05f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEDE3D6), origin + Offset(s, 0f), origin + Offset(0f, s),
        strokeWidth = r * 0.05f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEDE3D6), origin + Offset(0f, s), origin + Offset(s, s),
        strokeWidth = r * 0.05f, cap = StrokeCap.Round)
}

private fun DrawScope.drawMagnifyingGlass(center: Offset, r: Float) {
    val lensCenter = center + Offset(r * 0.55f, r * 0.10f)
    val lensR = r * 0.42f
    val frameColor = Color(0xFF8B5A2B)
    val glassColor = Color(0xFFB3D1FF).copy(alpha = 0.25f)
    drawCircle(color = glassColor, radius = lensR, center = lensCenter)
    drawCircle(color = frameColor, radius = lensR, center = lensCenter, style = Stroke(width = r * 0.08f))
    // shine
    val shine = lensCenter + Offset(-lensR * 0.35f, -lensR * 0.35f)
    drawArc(
        color = Color.White.copy(alpha = 0.7f),
        startAngle = 200f, sweepAngle = 50f, useCenter = false,
        topLeft = Offset(shine.x - lensR * 0.5f, shine.y - lensR * 0.5f),
        size = Size(lensR, lensR),
        style = Stroke(width = r * 0.04f, cap = StrokeCap.Round),
    )
    // Handle
    val handleStart = lensCenter + Offset(lensR * 0.71f, lensR * 0.71f)
    val handleEnd = handleStart + Offset(r * 0.50f, r * 0.50f)
    drawLine(color = frameColor, start = handleStart, end = handleEnd, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
}

private fun polarOffset(origin: Offset, angleDeg: Float, length: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        origin.x + (cos(rad) * length).toFloat(),
        origin.y + (sin(rad) * length).toFloat(),
    )
}

private fun DrawScope.drawAura(center: Offset, r: Float, pulse: Float) {
    val auraColor = Color(0xFFFFD700)
    val outerR = r * (1.40f + 0.20f * pulse)
    val innerR = r * 1.06f
    for (i in 6 downTo 1) {
        val frac = i / 6f
        drawCircle(
            color = auraColor.copy(alpha = 0.07f * (1f - frac) + 0.04f),
            radius = innerR + (outerR - innerR) * frac,
            center = center,
        )
    }
}

private fun DrawScope.drawCape(center: Offset, r: Float, wave: Float) {
    val capeColor = Color(0xFFCE1126)
    val edgeColor = Color(0xFF8B0000)
    val topY = center.y - r * 0.3f
    val bottomY = center.y + r * 1.35f
    val leftTop = center.x - r * 0.75f
    val rightTop = center.x + r * 0.75f
    val sway = wave * r * 0.08f
    val leftBottom = center.x - r * 1.15f + sway
    val rightBottom = center.x + r * 1.15f + sway
    val path = Path().apply {
        moveTo(leftTop, topY)
        lineTo(rightTop, topY)
        lineTo(rightBottom, bottomY - wave * r * 0.10f)
        quadraticBezierTo(center.x + r * 0.45f, bottomY + r * 0.10f * wave, center.x, bottomY)
        quadraticBezierTo(center.x - r * 0.45f, bottomY - r * 0.10f * wave, leftBottom, bottomY - wave * r * 0.10f)
        close()
    }
    drawPath(path, color = capeColor)
    drawPath(path, color = edgeColor, style = Stroke(width = r * 0.035f))
}

private fun DrawScope.drawHeadband(center: Offset, r: Float) {
    val bandColor = Color(0xFFCE1126)
    val bandShadow = Color(0xFF7A0A14)
    val bandHighlight = Color(0xFFE93140)
    val stripeColor = Color.White.copy(alpha = 0.85f)

    val bandTop = center.y - r * 0.50f
    val bandBottom = center.y - r * 0.33f
    val bandH = bandBottom - bandTop
    val width = r * 2f
    val left = center.x - r

    val cookiePath = Path().apply {
        addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    clipPath(cookiePath) {
        // Base band
        drawRect(color = bandColor, topLeft = Offset(left, bandTop), size = Size(width, bandH))
        // Top highlight
        drawRect(
            color = bandHighlight,
            topLeft = Offset(left, bandTop),
            size = Size(width, bandH * 0.18f),
        )
        // Bottom shadow
        drawRect(
            color = bandShadow,
            topLeft = Offset(left, bandBottom - bandH * 0.16f),
            size = Size(width, bandH * 0.16f),
        )
        // Subtle white center stripe baseline (thin)
        drawRect(
            color = stripeColor.copy(alpha = 0.20f),
            topLeft = Offset(left, bandTop + bandH * 0.47f),
            size = Size(width, bandH * 0.06f),
        )

        // NIKE branding text + swoosh
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = Color.White.toArgb()
                textSize = bandH * 0.68f
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.08f
            }
            val cx = center.x + r * 0.10f  // shifted right a bit to balance knot on the left
            val cy = bandTop + bandH * 0.72f
            canvas.nativeCanvas.drawText("NIKE", cx, cy, paint)
        }

        // Swoosh-style curve next to the text
        val swooshStart = Offset(center.x - r * 0.10f, bandTop + bandH * 0.40f)
        val swooshMid = Offset(center.x - r * 0.05f, bandTop + bandH * 0.95f)
        val swooshEnd = Offset(center.x + r * 0.05f, bandTop + bandH * 0.55f)
        val swooshPath = Path().apply {
            moveTo(swooshStart.x, swooshStart.y)
            quadraticBezierTo(swooshMid.x, swooshMid.y, swooshEnd.x, swooshEnd.y)
        }
        drawPath(
            path = swooshPath,
            color = Color.White,
            style = Stroke(width = bandH * 0.14f, cap = StrokeCap.Round),
        )
    }

    // Knot on the left side, sticking out from the cookie
    val knotMidY = (bandTop + bandBottom) / 2f
    val knotCenter = Offset(center.x - r * 0.94f, knotMidY)
    drawCircle(color = bandColor, radius = r * 0.11f, center = knotCenter)
    drawCircle(color = bandShadow, radius = r * 0.11f, center = knotCenter, style = Stroke(width = r * 0.022f))
    // Small darker crease in the knot
    drawLine(
        color = bandShadow,
        start = knotCenter + Offset(-r * 0.05f, -r * 0.05f),
        end = knotCenter + Offset(r * 0.05f, r * 0.05f),
        strokeWidth = r * 0.025f,
        cap = StrokeCap.Round,
    )
    // Two ribbon tails hanging diagonally from the knot
    val tailStartA = knotCenter + Offset(-r * 0.06f, -r * 0.04f)
    val tailEndA = knotCenter + Offset(-r * 0.26f, -r * 0.32f)
    drawLine(
        color = bandColor,
        start = tailStartA,
        end = tailEndA,
        strokeWidth = r * 0.09f,
        cap = StrokeCap.Round,
    )
    // Tail A fold
    drawLine(
        color = bandShadow,
        start = tailEndA,
        end = tailEndA + Offset(-r * 0.04f, r * 0.05f),
        strokeWidth = r * 0.05f,
        cap = StrokeCap.Round,
    )
    val tailStartB = knotCenter + Offset(-r * 0.06f, r * 0.04f)
    val tailEndB = knotCenter + Offset(-r * 0.22f, r * 0.34f)
    drawLine(
        color = bandColor,
        start = tailStartB,
        end = tailEndB,
        strokeWidth = r * 0.09f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = bandShadow,
        start = tailEndB,
        end = tailEndB + Offset(-r * 0.05f, -r * 0.04f),
        strokeWidth = r * 0.05f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCap(center: Offset, r: Float) {
    val capColor = Color(0xFF15100B)
    val capAccent = Color(0xFFFFC58A)
    val capCenter = Offset(center.x - r * 0.10f, center.y - r * 0.78f)
    drawArc(
        color = capColor,
        startAngle = 180f, sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(capCenter.x - r * 0.65f, capCenter.y - r * 0.36f),
        size = Size(r * 1.30f, r * 0.72f),
    )
    drawArc(
        color = capColor,
        startAngle = 180f, sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(capCenter.x + r * 0.30f, capCenter.y - r * 0.04f),
        size = Size(r * 0.60f, r * 0.20f),
    )
    drawCircle(color = capAccent, radius = r * 0.06f, center = Offset(capCenter.x, capCenter.y - r * 0.22f))
    drawLine(
        color = capAccent,
        start = Offset(capCenter.x - r * 0.65f, capCenter.y),
        end = Offset(capCenter.x + r * 0.65f, capCenter.y),
        strokeWidth = r * 0.03f,
    )
}

private fun DrawScope.drawCrown(center: Offset, r: Float) {
    val gold = Color(0xFFFFD700)
    val goldDark = Color(0xFFB8860B)
    val baseY = center.y - r * 0.88f
    val left = center.x - r * 0.55f
    val right = center.x + r * 0.55f
    val baseH = r * 0.12f
    drawRect(color = gold, topLeft = Offset(left, baseY), size = Size(right - left, baseH))
    drawRect(color = goldDark, topLeft = Offset(left, baseY), size = Size(right - left, baseH), style = Stroke(width = r * 0.02f))

    val spikeH = r * 0.36f
    val path = Path().apply {
        moveTo(left, baseY)
        lineTo(left + (right - left) / 5f, baseY - spikeH * 0.70f)
        lineTo(left + (right - left) * 2f / 5f, baseY)
        lineTo(center.x, baseY - spikeH)
        lineTo(left + (right - left) * 3f / 5f, baseY)
        lineTo(left + (right - left) * 4f / 5f, baseY - spikeH * 0.70f)
        lineTo(right, baseY)
        close()
    }
    drawPath(path, color = gold)
    drawPath(path, color = goldDark, style = Stroke(width = r * 0.025f))
    drawCircle(color = Color(0xFFCE1126), radius = r * 0.05f, center = Offset(center.x, baseY - spikeH * 0.55f))
    drawCircle(color = Color(0xFF003893), radius = r * 0.035f, center = Offset(left + (right - left) / 5f, baseY - spikeH * 0.35f))
    drawCircle(color = Color(0xFF003893), radius = r * 0.035f, center = Offset(left + (right - left) * 4f / 5f, baseY - spikeH * 0.35f))
}

private fun DrawScope.drawSunglasses(center: Offset, r: Float) {
    val frame = Color(0xFF080808)
    val lens = Color(0xFF1A1A1A)
    val shine = Color.White.copy(alpha = 0.45f)
    val eyeY = center.y - r * 0.25f
    val lensR = r * 0.14f
    val leftLens = Offset(center.x - r * 0.22f, eyeY)
    val rightLens = Offset(center.x + r * 0.22f, eyeY)
    drawCircle(color = lens, radius = lensR, center = leftLens)
    drawCircle(color = lens, radius = lensR, center = rightLens)
    drawCircle(color = frame, radius = lensR, center = leftLens, style = Stroke(width = r * 0.025f))
    drawCircle(color = frame, radius = lensR, center = rightLens, style = Stroke(width = r * 0.025f))
    drawLine(
        color = frame,
        start = Offset(leftLens.x + lensR * 0.9f, eyeY),
        end = Offset(rightLens.x - lensR * 0.9f, eyeY),
        strokeWidth = r * 0.03f,
    )
    drawCircle(color = shine, radius = r * 0.035f, center = Offset(leftLens.x - lensR * 0.35f, eyeY - lensR * 0.35f))
    drawCircle(color = shine, radius = r * 0.035f, center = Offset(rightLens.x - lensR * 0.35f, eyeY - lensR * 0.35f))
}

private fun DrawScope.drawChain(center: Offset, r: Float) {
    val gold = Color(0xFFFFD700)
    val goldDark = Color(0xFFB8860B)
    drawArc(
        color = gold,
        startAngle = 15f, sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(center.x - r * 0.60f, center.y - r * 0.05f),
        size = Size(r * 1.20f, r * 0.50f),
        style = Stroke(width = r * 0.045f, cap = StrokeCap.Round),
    )
    val pendant = Offset(center.x, center.y + r * 0.35f)
    drawCircle(color = gold, radius = r * 0.075f, center = pendant)
    drawCircle(color = goldDark, radius = r * 0.075f, center = pendant, style = Stroke(width = r * 0.015f))
    drawCircle(color = Color(0xFFCE1126), radius = r * 0.03f, center = pendant)
}

private fun DrawScope.drawDumbbell(center: Offset, s: Float, angleDeg: Float) {
    val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
    val ux = cos(rad)
    val uy = sin(rad)

    val barHalf = s * 1.3f
    val barStart = Offset(center.x - ux * barHalf, center.y - uy * barHalf)
    val barEnd = Offset(center.x + ux * barHalf, center.y + uy * barHalf)
    drawLine(
        color = BarMetal,
        start = barStart,
        end = barEnd,
        strokeWidth = s * 0.36f,
        cap = StrokeCap.Round,
    )

    val plateOffset = s * 1.1f
    val plateW = s * 0.5f
    val plateH = s * 1.5f
    listOf(-1f, 1f).forEach { sign ->
        val cx = center.x + ux * plateOffset * sign
        val cy = center.y + uy * plateOffset * sign
        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
            drawRoundRect(
                color = PlateDark,
                topLeft = Offset(cx - plateW / 2f, cy - plateH / 2f),
                size = Size(plateW, plateH),
                cornerRadius = CornerRadius(plateW * 0.2f, plateW * 0.2f),
            )
        }
    }
}
