package com.juan.fittracker.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.ui.CookieAvatar

private val BgTop = Color(0xFF15100B)
private val BgBottom = Color(0xFF2E1F14)
private val Accent = Color(0xFFFFC58A)
private val OnDark = Color(0xFFEDE3D6)

private data class TourStep(
    val icon: ImageVector,
    val title: String,
    val description: (String) -> String, // takes "bro" and returns text
    val mood: CookieMood,
)

private val steps = listOf(
    TourStep(
        icon = Icons.Filled.Home,
        title = "Home",
        description = { bro -> "Acá te muestro cómo vas hoy, $bro. Pasos, kcal, sueño, todo. Frase rota que cambia, y yo te juzgo (con cariño)." },
        mood = CookieMood.Happy,
    ),
    TourStep(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        title = "Entrenos",
        description = { bro -> "Anote cada sesión con sus series y peso, $bro. Galletoide le dice si fue bestial, regular o si se rajó." },
        mood = CookieMood.Energetic,
    ),
    TourStep(
        icon = Icons.Filled.Restaurant,
        title = "Comida",
        description = { bro -> "Anote lo que tragó, $bro. Hay atajos rolos (tamal, ajiaco) y mediterráneos. Si come fit le aplaudo, si cae en Pan Yiyo le jodo un toque." },
        mood = CookieMood.Stuffed,
    ),
    TourStep(
        icon = Icons.Filled.Place,
        title = "Gym cerca",
        description = { bro -> "Si busca dónde sudar cerca, acá le saco los gyms del barrio. OpenStreetMap, gratis, sin trampa." },
        mood = CookieMood.Happy,
    ),
    TourStep(
        icon = Icons.Filled.Person,
        title = "Perfil",
        description = { bro -> "Tus datos, logros, nivel, tema (Oscuro / Día / Galleto) y el recordatorio diario. Acá manda usted, $bro." },
        mood = CookieMood.Neutral,
    ),
)

@Composable
fun TourScreen(profile: UserProfile, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val bro = when (profile.sex) {
        Sex.Male -> "hermano"
        Sex.Female -> "hermana"
        Sex.Unspecified -> "marica"
    }
    val current = steps[step]
    val isLast = step == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / steps.size },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = Accent,
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
                Spacer(Modifier.width(16.dp))
                TextButton(onClick = onFinish) {
                    Text("Saltar", color = OnDark.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${step + 1} de ${steps.size}",
                color = OnDark.copy(alpha = 0.55f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(tween(350)) { it / 2 } + fadeIn(tween(350)) togetherWith
                            slideOutHorizontally(tween(350)) { -it / 2 } + fadeOut(tween(250))
                    } else {
                        slideInHorizontally(tween(350)) { -it / 2 } + fadeIn(tween(350)) togetherWith
                            slideOutHorizontally(tween(350)) { it / 2 } + fadeOut(tween(250))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "tour",
            ) { idx ->
                val s = steps[idx]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CookieAvatar(modifier = Modifier.size(180.dp), mood = s.mood)
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .background(Accent.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(s.icon, contentDescription = null, tint = Accent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.title,
                                color = Accent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = s.description(bro),
                        color = OnDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = { step-- },
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text("Atrás", color = OnDark, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = {
                        if (isLast) onFinish() else step++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF15100B),
                    ),
                ) {
                    Text(
                        if (isLast) "¡Vamos, $bro!" else "Siguiente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
