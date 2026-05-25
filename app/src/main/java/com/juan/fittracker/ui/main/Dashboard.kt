package com.juan.fittracker.ui.main

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.juan.fittracker.data.DailyMetric
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.GalletoideInsight
import com.juan.fittracker.data.HealthConnectManager
import com.juan.fittracker.data.HeartRateStats
import com.juan.fittracker.data.Insights
import com.juan.fittracker.data.SleepStats
import com.juan.fittracker.data.TodayHealthStats
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.WorkoutWithSets
import com.juan.fittracker.data.CaloriesBalance
import com.juan.fittracker.data.NutritionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal val Accent: Color
    @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
internal val OnDark: Color
    @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
private val PositiveGreen = Color(0xFF7BD389)
private val NegativeRed: Color
    @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.error
@Composable
private fun cardSurface(): Color =
    com.juan.fittracker.ui.theme.LocalAppColors.current.surface

sealed class HCState {
    data object Loading : HCState()
    data object Unavailable : HCState()
    data object NeedsPermission : HCState()
    data class Ready(
        val today: TodayHealthStats,
        val sleep: SleepStats,
        val heartRate: HeartRateStats,
        val dailyMetrics: List<DailyMetric>, // last 14 days, oldest first
    ) : HCState() {
        val last7Days: List<DailyMetric> get() = dailyMetrics.takeLast(7)
        val prev7Days: List<DailyMetric> get() = dailyMetrics.dropLast(7).takeLast(7)
    }
}

suspend fun loadHCState(context: Context): HCState {
    val client = HealthConnectManager.clientOrNull(context) ?: return HCState.Unavailable
    if (!HealthConnectManager.hasAllPermissions(client)) return HCState.NeedsPermission
    return HCState.Ready(
        today = HealthConnectManager.readTodayStats(client),
        sleep = HealthConnectManager.readLastNightSleep(client),
        heartRate = HealthConnectManager.readHeartRate(client),
        dailyMetrics = HealthConnectManager.readLastNDays(client, 14),
    )
}

@Composable
fun QuoteBubble(quote: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("“", color = Accent, fontSize = 40.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text(
                text = quote,
                color = OnDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun InsightAdviceCard(insight: GalletoideInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Consejo del día", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = insight.advice,
                color = OnDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun TodayCard(
    today: TodayHealthStats,
    gymKcal: Float,
    hcReady: Boolean,
) {
    val totalKcal = today.activeKcal + gymKcal
    val stepGoal = 10_000L
    val stepProgress = (today.steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Hoy", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                StatItem(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    value = "${today.steps}",
                    label = "pasos",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    icon = Icons.Filled.Bolt,
                    value = "${totalKcal.toInt()}",
                    label = "kcal totales",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Activa: ${today.activeKcal.toInt()} kcal · Gym: ${gymKcal.toInt()} kcal",
                color = OnDark.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { stepProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
                color = Accent,
                trackColor = Color.Transparent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(stepProgress * 100).toInt()}% de tu meta diaria (10.000 pasos)",
                color = OnDark.copy(alpha = 0.6f),
                fontSize = 11.sp,
            )
            if (!hcReady) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sin reloj conectado · solo verás kcal del gym",
                    color = OnDark.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
fun SleepCard(sleep: SleepStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Accent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Sueño anoche", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (sleep.hasData) sleep.display else "Sin datos",
                    color = OnDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = when {
                        !sleep.hasData -> "Tu reloj no registró sueño."
                        sleep.hours < 6f -> "Pocas horas, parce."
                        sleep.hours <= 9f -> "Descanso óptimo."
                        else -> "Demasiada cama hoy."
                    },
                    color = OnDark.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun HeartRateCard(hr: HeartRateStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("Pulsaciones", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${hr.latestBpm ?: hr.dayAvg ?: 0}",
                    color = OnDark,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "bpm",
                    color = OnDark.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hoy · min ${hr.dayMin ?: "—"} · máx ${hr.dayMax ?: "—"} · media ${hr.dayAvg ?: "—"}",
                color = OnDark.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun StepsTrendCard(days: List<DailyMetric>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Pasos · últimos 7 días", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            StepsBarChart(days)
        }
    }
}

@Composable
private fun StepsBarChart(days: List<DailyMetric>) {
    val max = (days.maxOfOrNull { it.steps } ?: 0L).coerceAtLeast(1L)
    val locale = Locale("es", "ES")
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }

    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = if (day.steps >= 1000) "${day.steps / 1000}k" else "${day.steps}",
                    fontSize = 10.sp,
                    color = OnDark.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(110.dp * (day.steps.toFloat() / max.toFloat()))
                        .heightIn(min = 3.dp)
                        .background(
                            Accent.copy(alpha = 0.85f),
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = day.date.format(dayFormatter).take(3).lowercase(locale),
                    fontSize = 11.sp,
                    color = OnDark.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
fun CaloriesCard(balance: CaloriesBalance) {
    val (statusColor, statusBg) = when (balance.status) {
        NutritionStatus.Deficit -> Color(0xFF7BD389) to Color(0xFF7BD389).copy(alpha = 0.15f)
        NutritionStatus.Maintenance -> Accent to Accent.copy(alpha = 0.15f)
        NutritionStatus.Surplus -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Calorías hoy", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(balance.status.label, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MetricColumn(
                    label = "Ingeridas",
                    value = "${balance.intake}",
                    suffix = "kcal",
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    label = "Gastadas",
                    value = "${balance.burnedTotal}",
                    suffix = "kcal",
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    label = "Objetivo",
                    value = "${balance.target}",
                    suffix = "kcal",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = balance.status.description,
                color = OnDark.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = OnDark.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(2.dp))
            Text(suffix, color = OnDark.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
fun WeekDeltaCard(thisWeek: WeekTotals, lastWeek: WeekTotals) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Esta semana vs anterior", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DeltaItem(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = "Pasos",
                    value = thisWeek.steps,
                    prev = lastWeek.steps,
                    modifier = Modifier.weight(1f),
                )
                DeltaItem(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Kcal",
                    value = (thisWeek.totalKcal).toLong(),
                    prev = (lastWeek.totalKcal).toLong(),
                    modifier = Modifier.weight(1f),
                )
                DeltaItem(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Entrenos",
                    value = thisWeek.workouts.toLong(),
                    prev = lastWeek.workouts.toLong(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DeltaItem(
    icon: ImageVector,
    label: String,
    value: Long,
    prev: Long,
    modifier: Modifier = Modifier,
) {
    val delta = value - prev
    val arrow: ImageVector? = when {
        delta > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        delta < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> null
    }
    val color = when {
        delta > 0 -> PositiveGreen
        delta < 0 -> NegativeRed
        else -> OnDark.copy(alpha = 0.6f)
    }
    Column(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (value >= 1000) "${value / 1000}k" else "$value",
            color = OnDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(label, color = OnDark.copy(alpha = 0.6f), fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (arrow != null) Icon(arrow, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                text = if (delta == 0L) "igual" else (if (delta > 0) "+$delta" else "$delta"),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

data class WeekTotals(
    val steps: Long,
    val totalKcal: Float,
    val workouts: Int,
)

@Composable
fun ConnectHealthCard(onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Conectar reloj", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Conecta Health Connect para que la galletoide lea pasos, sueño, pulsaciones y kcal de Samsung Health / Google Fit.",
                color = OnDark.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text("Conectar con Salud", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HCUnavailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Salud no disponible", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Health Connect no está disponible en este dispositivo. En tu S25 Ultra real vendrá de fábrica con Android 15.",
                color = OnDark.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
internal fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Icon(icon, contentDescription = null, tint = Accent)
        Spacer(Modifier.height(4.dp))
        Text(value, color = OnDark, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(label, color = OnDark.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

fun isToday(epochMs: Long): Boolean {
    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return date == LocalDate.now()
}

fun List<WorkoutWithSets>.gymKcal(bodyWeightKg: Int): Float =
    sumOf { it.totalSets } * bodyWeightKg * 0.125f

fun List<WorkoutWithSets>.totals(bodyWeightKg: Int): WeekTotals = WeekTotals(
    steps = 0L, // gym side; no steps from workouts
    totalKcal = gymKcal(bodyWeightKg),
    workouts = size,
)
