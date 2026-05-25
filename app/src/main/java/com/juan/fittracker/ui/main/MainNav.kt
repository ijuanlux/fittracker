package com.juan.fittracker.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.juan.fittracker.data.AchievementsEngine
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.HealthConnectManager
import com.juan.fittracker.data.Insights
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.Notifier
import com.juan.fittracker.data.Nutrition
import com.juan.fittracker.data.ReminderScheduler
import com.juan.fittracker.data.ReminderSettings
import com.juan.fittracker.data.UserPrefs
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.TodayHealthStats
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.todayOnly
import com.juan.fittracker.data.totalKcal
import com.juan.fittracker.ui.CookieAvatar
import com.juan.fittracker.ui.achievements.AchievementUnlockBanner
import com.juan.fittracker.ui.achievements.AchievementsSection
import com.juan.fittracker.ui.food.FoodScreen
import com.juan.fittracker.ui.nearby.NearbyScreen
import com.juan.fittracker.ui.workouts.WorkoutsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Workouts("Entrenos", Icons.AutoMirrored.Filled.DirectionsRun),
    Food("Comida", Icons.Filled.Restaurant),
    Nearby("Gym cerca", Icons.Filled.Place),
    Profile("Perfil", Icons.Filled.Person),
}

@Composable
fun MainNav(profile: UserProfile, onResetProfile: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1310),
                contentColor = Color(0xFFEDE3D6),
            ) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF15100B),
                            selectedTextColor = Color(0xFFFFC58A),
                            indicatorColor = Color(0xFFFFC58A),
                            unselectedIconColor = Color(0xFFEDE3D6).copy(alpha = 0.7f),
                            unselectedTextColor = Color(0xFFEDE3D6).copy(alpha = 0.7f),
                        ),
                    )
                }
            }
        },
        containerColor = Color(0xFF15100B),
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.Home -> HomeScreen(profile)
                Tab.Workouts -> WorkoutsScreen()
                Tab.Food -> FoodScreen(profile)
                Tab.Nearby -> NearbyScreen(profile)
                Tab.Profile -> ProfileScreen(profile, onResetProfile)
            }
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                AchievementUnlockBanner()
            }
        }
    }
}

@Composable
private fun HomeScreen(profile: UserProfile) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workoutDao = remember(context) { Db.get(context).workoutDao() }
    val mealDao = remember(context) { Db.get(context).mealDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val workouts by workoutDao.observeAll().collectAsState(initial = emptyList())
    val meals by mealDao.observeAll().collectAsState(initial = emptyList())
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val totalXp = Levels.totalXp(unlocks)
    val galletoideLevel = Levels.level(totalXp)

    var hcState by remember { mutableStateOf<HCState>(HCState.Loading) }
    val contract = remember { PermissionController.createRequestPermissionResultContract() }
    val launcher = rememberLauncherForActivityResult(contract) {
        scope.launch { hcState = loadHCState(context) }
    }
    LaunchedEffect(Unit) { hcState = loadHCState(context) }

    val ready = hcState as? HCState.Ready
    val today = ready?.today ?: TodayHealthStats(0L, 0.0)
    val sleep = ready?.sleep
    val heartRate = ready?.heartRate
    val last7Days = ready?.last7Days ?: emptyList()
    val prev7Days = ready?.prev7Days ?: emptyList()
    val hasData = ready != null

    val todayWorkouts = workouts.filter { isToday(it.workout.dateEpochMs) }
    val gymKcalToday = todayWorkouts.gymKcal(profile.weightKg)
    val intakeToday = meals.todayOnly().totalKcal()
    val balance = Nutrition.balance(profile, intakeToday, today.activeKcal, gymKcalToday)

    // Week comparison
    val now = System.currentTimeMillis()
    val day = 24L * 3_600_000L
    val sevenDaysAgo = now - 7 * day
    val fourteenDaysAgo = now - 14 * day
    val thisWeekWorkouts = workouts.filter { it.workout.dateEpochMs >= sevenDaysAgo }
    val prevWeekWorkouts = workouts.filter {
        it.workout.dateEpochMs in fourteenDaysAgo..<sevenDaysAgo
    }
    val thisWeekTotals = WeekTotals(
        steps = last7Days.sumOf { it.steps },
        totalKcal = thisWeekWorkouts.gymKcal(profile.weightKg) +
            last7Days.sumOf { it.activeKcal }.toFloat(),
        workouts = thisWeekWorkouts.size,
    )
    val prevWeekTotals = WeekTotals(
        steps = prev7Days.sumOf { it.steps },
        totalKcal = prevWeekWorkouts.gymKcal(profile.weightKg) +
            prev7Days.sumOf { it.activeKcal }.toFloat(),
        workouts = prevWeekWorkouts.size,
    )

    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            refreshKey++
        }
    }

    LaunchedEffect(workouts.size, meals.size, today.steps, intakeToday) {
        runCatching {
            val newOnes = AchievementsEngine.evaluateAndPersist(
                context = context,
                workouts = workouts,
                meals = meals,
                todaySteps = today.steps,
                intakeToday = intakeToday,
                targetKcal = balance.target,
            )
            newOnes.forEach { Notifier.showAchievement(context, it) }
        }
    }

    val insight = remember(today.steps, sleep?.totalMinutes, hasData, heartRate?.dayAvg, profile.sex, intakeToday, refreshKey) {
        Insights.compute(
            sleep = sleep,
            steps = today.steps,
            activeKcal = today.activeKcal,
            hr = heartRate,
            hasAnyData = hasData,
            sex = profile.sex,
            intakeKcal = intakeToday,
            targetKcal = balance.target,
            refreshKey = refreshKey,
        )
    }

    var speaking by remember { mutableStateOf(true) }
    LaunchedEffect(insight.quote) {
        speaking = true
        delay(2800)
        speaking = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = when (profile.sex) {
                Sex.Male -> "¡Hola, campeón!"
                Sex.Female -> "¡Hola, campeona!"
                Sex.Unspecified -> "¡Hola, crack!"
            },
            color = Color(0xFFFFC58A),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Tu galletoide te lee según el día",
            color = Color(0xFFEDE3D6).copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { refreshKey++ },
            contentAlignment = Alignment.Center,
        ) {
            CookieAvatar(
                modifier = Modifier.size(200.dp),
                mood = insight.mood,
                isSpeaking = speaking,
                level = galletoideLevel,
            )
        }
        Spacer(Modifier.height(12.dp))

        QuoteBubble(insight.quote)
        Spacer(Modifier.height(16.dp))

        when (val s = hcState) {
            HCState.Loading -> {
                TodayCard(today, gymKcalToday, hcReady = false)
            }
            HCState.Unavailable -> {
                HCUnavailableCard()
                Spacer(Modifier.height(16.dp))
                TodayCard(today, gymKcalToday, hcReady = false)
            }
            HCState.NeedsPermission -> {
                ConnectHealthCard(onConnect = { launcher.launch(HealthConnectManager.Permissions) })
                Spacer(Modifier.height(16.dp))
                TodayCard(today, gymKcalToday, hcReady = false)
            }
            is HCState.Ready -> {
                InsightAdviceCard(insight)
                Spacer(Modifier.height(16.dp))
                TodayCard(today, gymKcalToday, hcReady = true)
                Spacer(Modifier.height(16.dp))
                SleepCard(s.sleep)
                if (s.heartRate.hasData) {
                    Spacer(Modifier.height(16.dp))
                    HeartRateCard(s.heartRate)
                }
                Spacer(Modifier.height(16.dp))
                StepsTrendCard(s.last7Days)
                Spacer(Modifier.height(16.dp))
                WeekDeltaCard(thisWeek = thisWeekTotals, lastWeek = prevWeekTotals)
            }
        }

        Spacer(Modifier.height(16.dp))
        CaloriesCard(balance)
        Spacer(Modifier.height(16.dp))
        SummaryCard(
            title = "Tu plan",
            line1 = "${profile.frequency.label} · ${profile.frequency.days}",
            line2 = "${profile.heightCm} cm · ${profile.weightKg} kg",
        )
        Spacer(Modifier.height(16.dp))
        SummaryCard(
            title = "Próximamente",
            line1 = "Logros, badges y evolución de galletoide",
            line2 = "Comparativas semanales y récords personales",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile, onResetProfile: () -> Unit) {
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val totalXp = Levels.totalXp(unlocks)
    val currentLevel = Levels.level(totalXp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CookieAvatar(modifier = Modifier.size(160.dp), animated = false, level = currentLevel)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tu perfil",
            color = Color(0xFFFFC58A),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(24.dp))
        ProfileRow(label = "Edad", value = "${profile.age} años")
        ProfileRow(label = "Sexo", value = profile.sex.label)
        ProfileRow(label = "Frecuencia", value = "${profile.frequency.label} · ${profile.frequency.days}")
        ProfileRow(label = "Altura", value = "${profile.heightCm} cm")
        ProfileRow(label = "Peso", value = "${profile.weightKg} kg")
        Spacer(Modifier.height(32.dp))
        AchievementsSection()
        Spacer(Modifier.height(32.dp))
        ReminderCard()
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(
                "Resetear perfil",
                color = Color(0xFFFF8A80),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    "¿Resetear perfil?",
                    color = Color(0xFFFFC58A),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "Se borrarán todos tus datos y volverás al onboarding desde el inicio. Esta acción no se puede deshacer.",
                    color = Color(0xFFEDE3D6).copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetProfile()
                }) {
                    Text("Sí, resetear", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = Color(0xFFEDE3D6))
                }
            },
            containerColor = Color(0xFF2A1F18),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by UserPrefs.observeReminder(context).collectAsState(initial = ReminderSettings.Default)
    var showTimePicker by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch {
                UserPrefs.saveReminder(context, settings.copy(enabled = true))
                ReminderScheduler.enable(context, settings.hour, settings.minute)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.hasPermission(context)) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        scope.launch {
            UserPrefs.saveReminder(context, settings.copy(enabled = enabled))
            if (enabled) ReminderScheduler.enable(context, settings.hour, settings.minute)
            else ReminderScheduler.disable(context)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Recordatorio diario",
                        color = Color(0xFFFFC58A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (settings.enabled)
                            "Galletoide te avisará a las ${"%02d".format(settings.hour)}:${"%02d".format(settings.minute)}"
                        else
                            "Activa pa' que la galletoide te recuerde entrenar",
                        color = Color(0xFFEDE3D6).copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF15100B),
                        checkedTrackColor = Color(0xFFFFC58A),
                        uncheckedThumbColor = Color(0xFFEDE3D6).copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    ),
                )
            }
            if (settings.enabled) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                ) {
                    Text(
                        "Cambiar hora · ${"%02d".format(settings.hour)}:${"%02d".format(settings.minute)}",
                        color = Color(0xFFFFC58A),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newSettings = settings.copy(hour = pickerState.hour, minute = pickerState.minute)
                    showTimePicker = false
                    scope.launch {
                        UserPrefs.saveReminder(context, newSettings)
                        if (newSettings.enabled) {
                            ReminderScheduler.enable(context, newSettings.hour, newSettings.minute)
                        }
                    }
                }) { Text("OK", color = Color(0xFFFFC58A)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar", color = Color(0xFFEDE3D6))
                }
            },
            title = { Text("Hora del recordatorio", color = Color(0xFFFFC58A)) },
            text = { TimePicker(state = pickerState) },
            containerColor = Color(0xFF2A1F18),
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFFEDE3D6).copy(alpha = 0.65f), fontSize = 15.sp)
        Text(value, color = Color(0xFFEDE3D6), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryCard(title: String, line1: String, line2: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = Color(0xFFFFC58A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(line1, color = Color(0xFFEDE3D6), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(line2, color = Color(0xFFEDE3D6).copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}
