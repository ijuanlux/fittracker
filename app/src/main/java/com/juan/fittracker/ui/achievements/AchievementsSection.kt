package com.juan.fittracker.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.Achievement
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.SoundFx
import com.juan.fittracker.ui.effects.ConfettiOverlay
import kotlinx.coroutines.delay

private val Accent: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
private val OnDark: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface

@Composable
fun AchievementsSection() {
    val context = LocalContext.current
    val dao = remember(context) { Db.get(context).achievementDao() }
    val unlocks by dao.observeAll().collectAsState(initial = emptyList())
    val unlockedIds = unlocks.map { it.id }.toSet()
    val total = Achievement.entries.size
    val done = unlockedIds.size
    val totalXp = Levels.totalXp(unlocks)
    val level = Levels.level(totalXp)
    val nextXp = Levels.xpForNextLevel(level)
    val currentLevelXp = Levels.xpForLevel(level)
    val xpProgress = if (nextXp == null) 1f
    else ((totalXp - currentLevelXp).toFloat() / (nextXp - currentLevelXp).toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Level badge card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Accent.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Accent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Nivel $level",
                            color = MaterialTheme.colorScheme.background,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextXp == null) "Máximo alcanzado, ¡jefa!"
                            else "$totalXp / $nextXp XP",
                            color = OnDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (nextXp == null) "$totalXp XP totales"
                            else "Faltan ${nextXp - totalXp} XP pa' subir",
                            color = OnDark.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Accent,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Mis logros", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(
                text = "$done / $total",
                color = OnDark.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Accent,
            trackColor = Color.White.copy(alpha = 0.08f),
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Achievement.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { a ->
                        AchievementCard(
                            achievement = a,
                            unlocked = a.id in unlockedIds,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardColor = if (unlocked) Accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)
    val textColor = if (unlocked) OnDark else OnDark.copy(alpha = 0.45f)
    val emojiAlpha = if (unlocked) 1f else 0.35f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                achievement.emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(top = 4.dp),
                color = Color.White.copy(alpha = emojiAlpha),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                achievement.title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                achievement.description,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (unlocked) "+${achievement.xp} XP" else "🔒",
                color = if (unlocked) Accent else OnDark.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun AchievementUnlockBanner() {
    val context = LocalContext.current
    val dao = remember(context) { Db.get(context).achievementDao() }
    var shown by remember { mutableStateOf<Achievement?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val seen = remember { mutableSetOf<String>() }
    val firstLoad = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        dao.observeAll().collect { unlocks ->
            val current = unlocks.map { it.id }.toSet()
            if (firstLoad.value) {
                seen.addAll(current)
                firstLoad.value = false
                return@collect
            }
            val newOnes = current - seen
            if (newOnes.isNotEmpty()) {
                val ach = Achievement.byId(newOnes.first())
                if (ach != null) {
                    shown = ach
                    showConfetti = true
                    SoundFx.playAchievement()
                    seen.addAll(current)
                    delay(4000)
                    shown = null
                } else {
                    seen.addAll(current)
                }
            }
        }
    }

    ConfettiOverlay(active = showConfetti, onFinish = { showConfetti = false })

    AnimatedVisibility(
        visible = shown != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        shown?.let { ach ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Accent),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ach.emoji, fontSize = 36.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("¡Logro desbloqueado!", color = MaterialTheme.colorScheme.background, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(2.dp))
                        Text(ach.title, color = MaterialTheme.colorScheme.background, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text(ach.description, color = MaterialTheme.colorScheme.background.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("+${ach.xp}", color = Accent, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
