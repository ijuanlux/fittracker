package com.juan.fittracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val BgTop = Color(0xFF15100B)
private val BgBottom = Color(0xFF2E1F14)
private val TitleColor = Color(0xFFFFC58A)
private val SubtitleColor = Color(0xFFEDE3D6)

@Composable
fun SplashScreen(isReady: Boolean, onFinished: () -> Unit) {
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var minTimeElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        titleVisible = true
        delay(350)
        subtitleVisible = true
        delay(1900)
        minTimeElapsed = true
    }
    LaunchedEffect(minTimeElapsed, isReady) {
        if (minTimeElapsed && isReady) onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CookieAvatar(modifier = Modifier.size(260.dp))

            Spacer(modifier = Modifier.height(36.dp))

            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(500)) +
                    slideInVertically(animationSpec = tween(500)) { it / 3 },
            ) {
                Text(
                    text = "Galleta",
                    color = TitleColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(animationSpec = tween(500)) +
                    slideInVertically(animationSpec = tween(500)) { it / 3 },
            ) {
                Text(
                    text = "F I T  T R A C K E R",
                    color = SubtitleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                )
            }
        }
    }
}
