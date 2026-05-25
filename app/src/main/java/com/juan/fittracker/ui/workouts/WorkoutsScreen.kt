package com.juan.fittracker.ui.workouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.ExerciseSet
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.ShareHelper
import com.juan.fittracker.data.SoundFx
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.Workout
import com.juan.fittracker.data.WorkoutClassifier
import com.juan.fittracker.data.WorkoutVibe
import com.juan.fittracker.data.WorkoutWithSets
import com.juan.fittracker.data.formatLongDate
import com.juan.fittracker.data.formatShortDate
import com.juan.fittracker.ui.CookieAvatar
import com.juan.fittracker.ui.effects.ConfettiOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableIntStateOf

private val Accent: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
private val OnDark: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
private val BgDark: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.background
private val Danger: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.error

private sealed class Mode {
    data object List : Mode()
    data object Adding : Mode()
    data object PickRoutine : Mode()
    data object ManageRoutines : Mode()
    data object Stats : Mode()
    data class LiveSession(val routine: com.juan.fittracker.data.RoutineWithExercises) : Mode()
}

@Composable
fun WorkoutsScreen(profile: UserProfile) {
    var mode by remember { mutableStateOf<Mode>(Mode.List) }
    var showConfetti by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        runCatching { com.juan.fittracker.data.DefaultRoutines.seedIfEmpty(context) }
    }
    BackHandler(enabled = mode !is Mode.List) { mode = Mode.List }
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = mode, animationSpec = tween(300), label = "workouts-mode") { current ->
            when (current) {
                Mode.List -> WorkoutsList(
                    profile = profile,
                    onAdd = { mode = Mode.Adding },
                    onStartLive = { mode = Mode.PickRoutine },
                    onOpenStats = { mode = Mode.Stats },
                )
                Mode.Stats -> WorkoutStatsScreen(
                    profile = profile,
                    onBack = { mode = Mode.List },
                )
                Mode.Adding -> AddWorkoutForm(
                    onCancel = { mode = Mode.List },
                    onSaved = {
                        SoundFx.playSuccess()
                        showConfetti = true
                        mode = Mode.List
                    },
                )
                Mode.PickRoutine -> RoutinePicker(
                    onCancel = { mode = Mode.List },
                    onPick = { routine -> mode = Mode.LiveSession(routine) },
                    onCreateNew = { mode = Mode.ManageRoutines },
                    onManage = { mode = Mode.ManageRoutines },
                )
                Mode.ManageRoutines -> RoutinesEditorScreen(
                    onBack = { mode = Mode.PickRoutine },
                )
                is Mode.LiveSession -> LiveSessionScreen(
                    profile = profile,
                    routine = current.routine,
                    onCancel = { mode = Mode.List },
                    onCompleted = {
                        showConfetti = true
                        mode = Mode.List
                    },
                )
            }
        }
        ConfettiOverlay(active = showConfetti, onFinish = { showConfetti = false })
    }
}

@Composable
private fun WorkoutsList(
    profile: UserProfile,
    onAdd: () -> Unit,
    onStartLive: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).workoutDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val workouts by dao.observeAll().collectAsState(initial = emptyList())
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val galletoideLevel = Levels.level(Levels.totalXp(unlocks))
    var pendingDelete by remember { mutableStateOf<WorkoutWithSets?>(null) }

    val todayWorkouts = workouts.filter { isTodayWorkout(it.workout.dateEpochMs) }
    val vibe = WorkoutClassifier.classify(todayWorkouts)
    val mood = when (vibe) {
        WorkoutVibe.Empty -> CookieMood.Lazy
        WorkoutVibe.Lazy -> CookieMood.Tired
        WorkoutVibe.Regular -> CookieMood.Neutral
        WorkoutVibe.Strong -> CookieMood.Happy
        WorkoutVibe.Beast -> CookieMood.Energetic
    }
    var quoteKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            quoteKey++
        }
    }
    val quote = remember(quoteKey, profile.sex, vibe) {
        RolaPhrases.pick(WorkoutClassifier.poolFor(vibe), quoteKey, profile.sex)
    }
    var speaking by remember { mutableStateOf(true) }
    LaunchedEffect(quote) {
        speaking = true
        delay(2200)
        speaking = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (workouts.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = "Mis Entrenos",
                        color = Accent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Aún no hay sesiones registradas",
                        color = OnDark.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    GalletoideWorkoutCard(
                        quote = quote,
                        mood = mood,
                        level = galletoideLevel,
                        speaking = speaking,
                        onTap = { quoteKey++ },
                    )
                    Spacer(Modifier.height(12.dp))
                    StartLiveButton(onClick = onStartLive)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ManualButton(onClick = onAdd, modifier = Modifier.weight(1f))
                        StatsButton(onClick = onOpenStats, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                    EmptyState(onAdd = onAdd)
                }
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = "Mis Entrenos",
                        color = Accent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${workouts.size} sesiones registradas",
                        color = OnDark.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    GalletoideWorkoutCard(
                        quote = quote,
                        mood = mood,
                        level = galletoideLevel,
                        speaking = speaking,
                        onTap = { quoteKey++ },
                    )
                    Spacer(Modifier.height(12.dp))
                    StartLiveButton(onClick = onStartLive)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ManualButton(onClick = onAdd, modifier = Modifier.weight(1f))
                        StatsButton(onClick = onOpenStats, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                }
                items(workouts, key = { it.workout.id }) { item ->
                    WorkoutCard(
                        item = item,
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Borrar entreno?", color = Accent, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se borrará el entreno del ${formatShortDate(toDelete.workout.dateEpochMs)} con todas sus series.",
                    color = OnDark.copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { dao.deleteWorkout(toDelete.workout) }
                }) {
                    Text("Borrar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = OnDark)
                }
            },
            containerColor = Color(0xFF2A1F18),
        )
    }
}

private fun isTodayWorkout(epochMs: Long): Boolean {
    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return date == LocalDate.now()
}

@Composable
private fun StartLiveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = BgDark,
        ),
    ) {
        Text("▶  Iniciar entreno guiado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text("🍪 Informe", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ManualButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text("✏ Manual", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun GalletoideWorkoutCard(
    quote: String,
    mood: CookieMood,
    level: Int,
    speaking: Boolean,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CookieAvatar(
                modifier = Modifier.size(96.dp),
                mood = mood,
                isSpeaking = speaking,
                level = level,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = quote,
                color = OnDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = Accent.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Aún no hay entrenos",
            color = OnDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Toca el + para registrar tu primera sesión.",
            color = OnDark.copy(alpha = 0.65f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun WorkoutCard(item: WorkoutWithSets, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        onClick = { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier
                    .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = formatShortDate(item.workout.dateEpochMs),
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.exerciseNames.firstOrNull().orEmpty()
                            .let { name ->
                                val extra = item.exerciseCount - 1
                                if (extra > 0) "$name · +$extra más" else name
                            }
                            .ifBlank { "Entreno" },
                        color = OnDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${item.totalSets} series · ${item.totalVolumeKg.toInt()} kg volumen",
                        color = OnDark.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                    )
                }
                val ctx = LocalContext.current
                IconButton(onClick = { ShareHelper.shareWorkout(ctx, item) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = Accent)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = Danger.copy(alpha = 0.85f))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    item.setsByExercise().forEach { (name, sets) ->
                        Text(
                            text = name,
                            color = Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                        sets.forEach { s ->
                            Text(
                                text = " · ${s.reps} reps × ${formatWeight(s.weightKg)}",
                                color = OnDark.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatWeight(kg: Float): String =
    if (kg <= 0f) "peso corporal" else "${if (kg % 1f == 0f) kg.toInt().toString() else kg.toString()} kg"

private class SetForm {
    var reps by mutableStateOf("")
    var weight by mutableStateOf("")
}

private enum class ExerciseMode(val label: String) {
    Simple("Simple"),
    Custom("Personalizada"),
}

private class ExerciseForm(initialName: String = "") {
    var name by mutableStateOf(initialName)
    var mode by mutableStateOf(ExerciseMode.Simple)
    var simpleSets by mutableStateOf("3")
    var simpleReps by mutableStateOf("10")
    var simpleWeight by mutableStateOf("")
    val sets: SnapshotStateList<SetForm> = mutableStateListOf(SetForm())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkoutForm(onCancel: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).workoutDao() }

    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val exercises = remember { mutableStateListOf(ExerciseForm()) }
    var saving by remember { mutableStateOf(false) }

    val canSave = exercises.any { ex ->
        ex.name.isNotBlank() && when (ex.mode) {
            ExerciseMode.Simple ->
                (ex.simpleSets.toIntOrNull() ?: 0) > 0 && (ex.simpleReps.toIntOrNull() ?: 0) > 0
            ExerciseMode.Custom ->
                ex.sets.any { (it.reps.toIntOrNull() ?: 0) > 0 }
        }
    } && !saving

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = OnDark)
            }
            Text(
                text = "Nuevo entreno",
                color = Accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                onClick = { showDatePicker = true },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Accent)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Fecha", color = OnDark.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text(
                            text = formatLongDate(dateMs),
                            color = OnDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            exercises.forEachIndexed { idx, exercise ->
                ExerciseCard(
                    index = idx,
                    exercise = exercise,
                    canRemove = exercises.size > 1,
                    onRemove = { exercises.removeAt(idx) },
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = { exercises.add(ExerciseForm()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Accent)
                Spacer(Modifier.size(8.dp))
                Text("Añadir ejercicio", color = Accent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    val workout = Workout(dateEpochMs = dateMs)
                    val sets = exercises.flatMapIndexed { exIdx, ex ->
                        if (ex.name.isBlank()) return@flatMapIndexed emptyList<ExerciseSet>()
                        when (ex.mode) {
                            ExerciseMode.Simple -> {
                                val n = ex.simpleSets.toIntOrNull() ?: 0
                                val reps = ex.simpleReps.toIntOrNull() ?: 0
                                val weight = ex.simpleWeight.replace(',', '.').toFloatOrNull() ?: 0f
                                if (n <= 0 || reps <= 0) emptyList()
                                else (0 until n).map { setIdx ->
                                    ExerciseSet(
                                        workoutId = 0L,
                                        exerciseName = ex.name.trim(),
                                        exerciseIndex = exIdx,
                                        setIndex = setIdx,
                                        reps = reps,
                                        weightKg = weight,
                                    )
                                }
                            }
                            ExerciseMode.Custom -> ex.sets.mapIndexedNotNull { setIdx, s ->
                                val reps = s.reps.toIntOrNull() ?: return@mapIndexedNotNull null
                                if (reps <= 0) return@mapIndexedNotNull null
                                val weight = s.weight.replace(',', '.').toFloatOrNull() ?: 0f
                                ExerciseSet(
                                    workoutId = 0L,
                                    exerciseName = ex.name.trim(),
                                    exerciseIndex = exIdx,
                                    setIndex = setIdx,
                                    reps = reps,
                                    weightKg = weight,
                                )
                            }
                        }
                    }
                    dao.saveWorkout(workout, sets)
                    onSaved()
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = BgDark,
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = OnDark.copy(alpha = 0.4f),
            ),
        ) {
            Text("Guardar entreno", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMs = it }
                    showDatePicker = false
                }) { Text("OK", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = OnDark)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ExerciseCard(
    index: Int,
    exercise: ExerciseForm,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ejercicio ${index + 1}",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar ejercicio", tint = Danger.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exercise.name,
                onValueChange = { exercise.name = it },
                label = { Text("Nombre", color = OnDark.copy(alpha = 0.6f)) },
                placeholder = { Text("Press banca, sentadilla...", color = OnDark.copy(alpha = 0.35f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            ModeToggle(exercise.mode) { exercise.mode = it }
            Spacer(Modifier.height(12.dp))
            Crossfade(targetState = exercise.mode, label = "ex-mode") { mode ->
                when (mode) {
                    ExerciseMode.Simple -> SimpleInputs(exercise)
                    ExerciseMode.Custom -> CustomInputs(exercise)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(mode: ExerciseMode, onModeChange: (ExerciseMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ExerciseMode.entries.forEachIndexed { idx, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onModeChange(m) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = ExerciseMode.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Accent.copy(alpha = 0.25f),
                    activeContentColor = Accent,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = OnDark.copy(alpha = 0.7f),
                    activeBorderColor = Accent,
                    inactiveBorderColor = OnDark.copy(alpha = 0.2f),
                ),
            ) {
                Text(m.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SimpleInputs(exercise: ExerciseForm) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = exercise.simpleSets,
            onValueChange = { v -> exercise.simpleSets = v.filter { it.isDigit() }.take(2) },
            label = { Text("Series", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = darkTextFieldColors(),
        )
        Text("×", color = OnDark.copy(alpha = 0.5f), fontSize = 22.sp, fontWeight = FontWeight.Light)
        OutlinedTextField(
            value = exercise.simpleReps,
            onValueChange = { v -> exercise.simpleReps = v.filter { it.isDigit() }.take(3) },
            label = { Text("Reps", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = darkTextFieldColors(),
        )
        Text("@", color = OnDark.copy(alpha = 0.5f), fontSize = 22.sp, fontWeight = FontWeight.Light)
        OutlinedTextField(
            value = exercise.simpleWeight,
            onValueChange = { v -> exercise.simpleWeight = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
            label = { Text("Kg", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            colors = darkTextFieldColors(),
        )
    }
}

@Composable
private fun CustomInputs(exercise: ExerciseForm) {
    Column {
        exercise.sets.forEachIndexed { setIdx, set ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${setIdx + 1}",
                    color = Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.size(28.dp).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = set.reps,
                    onValueChange = { v -> set.reps = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Reps", color = OnDark.copy(alpha = 0.6f), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = darkTextFieldColors(),
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = set.weight,
                    onValueChange = { v -> set.weight = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                    label = { Text("Kg", color = OnDark.copy(alpha = 0.6f), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = darkTextFieldColors(),
                )
                if (exercise.sets.size > 1) {
                    IconButton(onClick = { exercise.sets.removeAt(setIdx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar serie", tint = Danger.copy(alpha = 0.7f))
                    }
                }
            }
        }
        TextButton(
            onClick = { exercise.sets.add(SetForm()) },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Accent)
            Spacer(Modifier.size(4.dp))
            Text("Añadir serie", color = Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnDark,
    unfocusedTextColor = OnDark,
    focusedBorderColor = Accent,
    unfocusedBorderColor = OnDark.copy(alpha = 0.25f),
    cursorColor = Accent,
    focusedLabelColor = Accent,
    unfocusedLabelColor = OnDark.copy(alpha = 0.6f),
)
