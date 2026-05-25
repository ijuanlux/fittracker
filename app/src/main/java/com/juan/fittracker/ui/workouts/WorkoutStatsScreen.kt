package com.juan.fittracker.ui.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.PdfReporter
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.StatsRangePreset
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.WorkoutStats
import com.juan.fittracker.data.WorkoutStatsComputer
import com.juan.fittracker.data.formatShortDate
import com.juan.fittracker.ui.CookieAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutStatsScreen(profile: UserProfile, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val dao = remember(context) { Db.get(context).workoutDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val cardioDao = remember(context) { Db.get(context).cardioDao() }
    val workouts by dao.observeAll().collectAsState(initial = emptyList())
    val cardioAll by cardioDao.observeAll().collectAsState(initial = emptyList())
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val level = Levels.level(Levels.totalXp(unlocks))

    var preset by remember { mutableStateOf(StatsRangePreset.Last30) }
    var customStart by remember { mutableLongStateOf(System.currentTimeMillis() - 30L * 24 * 3_600_000) }
    var customEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRangePicker by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val (startMs, endMs, rangeLabel) = when (preset) {
        StatsRangePreset.Last7 -> Triple(now - 7L * 24 * 3_600_000, now, "Últimos 7 días")
        StatsRangePreset.Last30 -> Triple(now - 30L * 24 * 3_600_000, now, "Últimos 30 días")
        StatsRangePreset.Last90 -> Triple(now - 90L * 24 * 3_600_000, now, "Últimos 90 días")
        StatsRangePreset.AllTime -> Triple(0L, now, "Todo el tiempo")
        StatsRangePreset.Custom -> Triple(
            customStart,
            customEnd,
            "${formatShortDate(customStart)} – ${formatShortDate(customEnd)}",
        )
    }
    val stats = WorkoutStatsComputer.compute(workouts, startMs, endMs, rangeLabel, cardioAll)

    val bro = when (profile.sex) {
        Sex.Male -> "hermano"
        Sex.Female -> "hermana"
        Sex.Unspecified -> "marica"
    }
    var commentaryKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(stats.totalWorkouts) {
        commentaryKey = (System.currentTimeMillis() / 1000).toInt()
    }
    val commentary = remember(commentaryKey, profile.sex, stats.totalWorkouts) {
        val pool = when {
            stats.totalWorkouts == 0 -> RolaPhrases.statsReportEmpty
            stats.totalWorkouts < 5 -> RolaPhrases.statsReportLow
            stats.totalWorkouts < 15 -> RolaPhrases.statsReportMid
            else -> RolaPhrases.statsReportHigh
        }
        RolaPhrases.pick(pool, commentaryKey, profile.sex)
    }
    val mood = when {
        stats.totalWorkouts == 0 -> CookieMood.Lazy
        stats.totalWorkouts < 5 -> CookieMood.Tired
        stats.totalWorkouts < 15 -> CookieMood.Happy
        else -> CookieMood.Energetic
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val scope = rememberCoroutineScope()
        var exporting by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🍪 Galleto informe",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stats.rangeLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            IconButton(
                enabled = !exporting,
                onClick = {
                    scope.launch {
                        exporting = true
                        runCatching {
                            val uri = PdfReporter.generate(context, stats)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir Galleto informe"))
                        }
                        exporting = false
                    }
                },
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Exportar a PDF", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsRangePreset.entries.forEach { p ->
                FilterChip(
                    selected = preset == p,
                    onClick = {
                        if (p == StatsRangePreset.Custom) {
                            showRangePicker = true
                            preset = p
                        } else {
                            preset = p
                        }
                    },
                    label = { Text(p.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { commentaryKey++ },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CookieAvatar(
                        modifier = Modifier.size(100.dp),
                        mood = mood,
                        level = level,
                        isSpeaking = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    ) {
                        Text(
                            commentary,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item { BigNumberCard("Entrenos", stats.totalWorkouts.toString()) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard("Series", stats.totalSets.toString(), Modifier.weight(1f))
                    SmallStatCard("Reps", stats.totalReps.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard("Volumen", "${stats.totalVolumeKg.toInt()} kg", Modifier.weight(1f))
                    SmallStatCard("Días activos", stats.totalDays.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard("Promedio / semana", "%.1f".format(stats.avgPerWeek), Modifier.weight(1f))
                    SmallStatCard("Racha actual", "${stats.currentStreak} días", Modifier.weight(1f))
                }
            }
            item { MuscleBreakdownCard(stats.setsByMuscleGroup) }
            item { ListCard("Ejercicios top", stats.topExercises) }
            item { ListCard("Rutinas top", stats.topRoutines) }
            if (stats.cardio.totalSessions > 0) {
                item { CardioSectionCard(stats.cardio) }
            }
        }
    }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart,
            initialSelectedEndDateMillis = customEnd,
        )
        AlertDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedStartDateMillis?.let { customStart = it }
                    pickerState.selectedEndDateMillis?.let { customEnd = it + 24L * 3_600_000 - 1 }
                    showRangePicker = false
                }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = { DateRangePicker(state = pickerState) },
            containerColor = Color(0xFF2A1F18),
        )
    }
}

@Composable
private fun BigNumberCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SmallStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CardioSectionCard(c: com.juan.fittracker.data.CardioStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "🏃 Cardio",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CardioCell("${c.totalSessions}", "sesiones")
                CardioCell("${c.totalMinutes}", "min")
                CardioCell(if (c.totalKm > 0f) "%.1f".format(c.totalKm) else "—", "km")
                CardioCell(if (c.totalKcal > 0) "${c.totalKcal}" else "—", "kcal")
            }
            Spacer(Modifier.height(12.dp))
            if (c.sessionsByType.isNotEmpty()) {
                val max = c.sessionsByType.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                c.sessionsByType.take(6).forEach { (label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.width(110.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(5.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(count.toFloat() / max)
                                    .height(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardioCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun MuscleBreakdownCard(items: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Series por zona",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(
                    "Aún no hay sets para clasificar. Añade un entreno y vuelvo a contarte.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
                return@Card
            }
            val max = items.maxOf { it.second }.toFloat().coerceAtLeast(1f)
            items.forEach { (group, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        group,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(96.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                RoundedCornerShape(7.dp),
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .height(14.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(7.dp),
                                ),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$count",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListCard(title: String, items: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(
                    "Sin datos en este rango.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
                return@Card
            }
            items.forEachIndexed { idx, (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${idx + 1}. $name",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "× $count",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
