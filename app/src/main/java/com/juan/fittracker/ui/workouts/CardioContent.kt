package com.juan.fittracker.ui.workouts

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.CardioIntensity
import com.juan.fittracker.data.CardioKcal
import com.juan.fittracker.data.CardioSession
import com.juan.fittracker.data.CardioSource
import com.juan.fittracker.data.CardioSync
import com.juan.fittracker.data.CardioType
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.HealthConnectManager
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.SoundFx
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.formatShortDate
import com.juan.fittracker.ui.CookieAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private sealed class CardioMode {
    data object List : CardioMode()
    data object Adding : CardioMode()
}

@Composable
fun CardioContent(profile: UserProfile) {
    var mode by remember { mutableStateOf<CardioMode>(CardioMode.List) }
    Crossfade(targetState = mode, animationSpec = tween(220), label = "cardio-mode") { current ->
        when (current) {
            CardioMode.List -> CardioList(
                profile = profile,
                onAdd = { mode = CardioMode.Adding },
            )
            CardioMode.Adding -> CardioForm(
                profile = profile,
                onCancel = { mode = CardioMode.List },
                onSaved = {
                    SoundFx.playSuccess()
                    mode = CardioMode.List
                },
            )
        }
    }
}

@Composable
private fun CardioList(profile: UserProfile, onAdd: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).cardioDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val sessions by dao.observeAll().collectAsState(initial = emptyList())
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val level = Levels.level(Levels.totalXp(unlocks))
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<CardioSession?>(null) }

    val hcAvailable = remember(context) { HealthConnectManager.isAvailable(context) }
    val weeklyMin = sessions.filter { withinLastDays(it.dateEpochMs, 7) }.sumOf { it.durationMin }
    val weeklyKcal = sessions.filter { withinLastDays(it.dateEpochMs, 7) }.sumOf { it.kcal ?: 0 }
    val weeklyKm = sessions.filter { withinLastDays(it.dateEpochMs, 7) }.sumOf { (it.distanceKm ?: 0f).toDouble() }
    val weeklyCount = sessions.count { withinLastDays(it.dateEpochMs, 7) }

    var quoteKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            quoteKey++
        }
    }
    val pool = when {
        sessions.isEmpty() -> RolaPhrases.cardioEmpty
        weeklyCount == 0 -> RolaPhrases.cardioEmpty
        weeklyCount >= 4 -> RolaPhrases.cardioBeast
        weeklyCount >= 2 -> RolaPhrases.cardioRegular
        else -> RolaPhrases.cardioLight
    }
    val quote = remember(quoteKey, profile.sex, pool) {
        RolaPhrases.pick(pool, quoteKey, profile.sex)
    }
    val mood = when {
        weeklyCount >= 4 -> CookieMood.Energetic
        weeklyCount >= 2 -> CookieMood.Happy
        weeklyCount == 0 -> CookieMood.Lazy
        else -> CookieMood.Neutral
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxWidth()
                    .clickable { quoteKey++ }) {
                    CookieAvatar(
                        modifier = Modifier.size(110.dp),
                        mood = mood,
                        level = level,
                        isSpeaking = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        ),
                    ) {
                        Text(
                            quote,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item {
                WeeklySummaryCard(
                    minutes = weeklyMin,
                    km = weeklyKm.toFloat(),
                    kcal = weeklyKcal,
                    sessions = weeklyCount,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.background,
                        ),
                    ) {
                        Text("➕ Añadir cardio", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (hcAvailable) {
                        OutlinedButton(
                            onClick = {
                                if (!syncing) {
                                    syncing = true
                                    scope.launch {
                                        val added = runCatching {
                                            CardioSync.syncRecent(
                                                context = context,
                                                days = 14,
                                                defaultWeightKg = profile.weightKg.toFloat(),
                                            )
                                        }.getOrDefault(0)
                                        syncMessage = if (added > 0) "✓ $added importados" else "Sin nuevos"
                                        syncing = false
                                        delay(2500)
                                        syncMessage = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (syncing) "Sync…" else (syncMessage ?: "Sync reloj"),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Aún no hay sesiones de cardio",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Añade una manual o sincroniza desde tu reloj para arrancar.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            } else {
                items(sessions, key = { it.id }) { s ->
                    SessionCard(session = s, onDelete = { pendingDelete = s })
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text("¿Borrar cardio?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Se borrará ${toDelete.type.label} del ${formatShortDate(toDelete.dateEpochMs)}.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { dao.deleteById(toDelete.id) }
                }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = Color(0xFF2A1F18),
        )
    }
}

@Composable
private fun WeeklySummaryCard(minutes: Int, km: Float, kcal: Int, sessions: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Esta semana",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatCell(value = "$minutes", label = "minutos")
                StatCell(value = if (km > 0f) "%.1f".format(km) else "—", label = "km")
                StatCell(value = if (kcal > 0) "$kcal" else "—", label = "kcal")
                StatCell(value = "$sessions", label = "sesiones")
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun SessionCard(session: CardioSession, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(session.type.emoji, fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        session.type.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (session.source == CardioSource.HealthConnect) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "⌚",
                            fontSize = 12.sp,
                        )
                    }
                }
                Text(
                    formatShortDate(session.dateEpochMs) + " · " + session.intensity.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("⏱ ${session.durationMin} min")
                        session.distanceKm?.let { if (it > 0f) append("  ·  📍 %.1f km".format(it)) }
                        session.kcal?.let { if (it > 0) append("  ·  🔥 $it kcal") }
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardioForm(
    profile: UserProfile,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).cardioDao() }

    var type by remember { mutableStateOf(CardioType.Correr) }
    var intensity by remember { mutableStateOf(CardioIntensity.Media) }
    var duration by remember { mutableStateOf("30") }
    var distance by remember { mutableStateOf("") }
    var kcalOverride by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val autoKcal = remember(type, intensity, duration, profile.weightKg) {
        val mins = duration.toIntOrNull() ?: 0
        CardioKcal.estimate(type, intensity, mins, profile.weightKg.toFloat())
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Nuevo cardio",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Tipo", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            CardioTypeGrid(selected = type, onSelect = { type = it })

            Spacer(Modifier.height(20.dp))
            Text("Intensidad", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            IntensityRow(selected = intensity, onSelect = { intensity = it })

            Spacer(Modifier.height(20.dp))
            Row {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { v -> duration = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Duración (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = tfColors(),
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = distance,
                    onValueChange = { v ->
                        distance = v.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.').take(6)
                    },
                    label = { Text("Distancia (km)") },
                    placeholder = { Text("opcional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = tfColors(),
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = kcalOverride,
                onValueChange = { v -> kcalOverride = v.filter { it.isDigit() }.take(5) },
                label = { Text("Kcal (deja vacío para auto)") },
                placeholder = { Text("Auto: $autoKcal kcal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(80) },
                label = { Text("Notas (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors(),
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val mins = duration.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val finalKcal = kcalOverride.toIntOrNull() ?: autoKcal
                        val dist = distance.toFloatOrNull()?.takeIf { it > 0f }
                        dao.insert(
                            CardioSession(
                                dateEpochMs = System.currentTimeMillis(),
                                typeKey = type.name,
                                durationMin = mins,
                                distanceKm = dist,
                                intensityKey = intensity.name,
                                kcal = finalKcal,
                                sourceKey = CardioSource.Manual.name,
                                externalId = null,
                                notes = notes.ifBlank { null },
                            ),
                        )
                        onSaved()
                    }
                },
                enabled = !saving && (duration.toIntOrNull() ?: 0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                ),
            ) {
                Text("Guardar cardio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CardioTypeGrid(selected: CardioType, onSelect: (CardioType) -> Unit) {
    val types = CardioType.values().toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    val isSel = t == selected
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(t) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.05f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(t.emoji, fontSize = 24.sp)
                            Text(
                                t.label,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IntensityRow(selected: CardioIntensity, onSelect: (CardioIntensity) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CardioIntensity.values().forEach { i ->
            val isSel = i == selected
            Card(
                modifier = Modifier.weight(1f).clickable { onSelect(i) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.05f),
                ),
            ) {
                Text(
                    i.label,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
)

private fun withinLastDays(epochMs: Long, days: Int): Boolean {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val cutoff = LocalDate.now().minusDays((days - 1).toLong())
    return !date.isBefore(cutoff)
}
