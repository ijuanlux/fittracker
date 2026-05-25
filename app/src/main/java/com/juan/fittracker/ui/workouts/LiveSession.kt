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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.AchievementUnlock
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.ExerciseSet
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.RoutineWithExercises
import com.juan.fittracker.data.SoundFx
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.Workout
import com.juan.fittracker.ui.CookieAvatar
import com.juan.fittracker.ui.effects.ConfettiOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoutinePicker(
    onCancel: () -> Unit,
    onPick: (RoutineWithExercises) -> Unit,
    onCreateNew: () -> Unit,
    onManage: () -> Unit,
) {
    BackHandler { onCancel() }
    val context = LocalContext.current
    val dao = remember(context) { Db.get(context).routineDao() }
    val routines by dao.observeAll().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "¿Qué entrenamos hoy?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onManage) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar rutinas", tint = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(routines, key = { it.routine.id }) { rwx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(rwx) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(rwx.routine.emoji, fontSize = 36.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rwx.routine.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                rwx.sortedExercises.joinToString(", ") { it.exerciseName }
                                    .let { if (it.length > 60) it.take(57) + "…" else it },
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${rwx.exercises.size} ejercicios",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateNew() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Crear nueva rutina",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveSessionScreen(
    profile: UserProfile,
    routine: RoutineWithExercises,
    onCancel: () -> Unit,
    onCompleted: () -> Unit,
) {
    BackHandler { onCancel() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).workoutDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val unlocks by produceState(initialValue = emptyList<AchievementUnlock>()) {
        achievementDao.observeAll().collect { value = it }
    }
    val level = Levels.level(Levels.totalXp(unlocks))
    val exerciseList = routine.sortedExercises

    val completed = remember { mutableStateListOf<Int>() }
    var quoteKey by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8_000)
            quoteKey++
        }
    }
    val hype = remember(quoteKey, profile.sex) {
        RolaPhrases.pick(RolaPhrases.liveWorkoutHype, quoteKey, profile.sex)
    }
    val completeQuote = remember(profile.sex) {
        RolaPhrases.pick(RolaPhrases.liveWorkoutComplete, 0, profile.sex)
    }
    val allDone = completed.size == exerciseList.size && exerciseList.isNotEmpty()

    fun finishAndSave() {
        if (saved) return
        if (completed.isEmpty()) return
        saved = true
        scope.launch {
            val incomplete = completed.size < exerciseList.size
            val workout = Workout(
                dateEpochMs = System.currentTimeMillis(),
                notes = if (incomplete) "Rutina ${routine.routine.name} (incompleta)"
                else "Rutina ${routine.routine.name}",
            )
            val sets: List<ExerciseSet> = exerciseList
                .mapIndexedNotNull { exIdx, ex ->
                    if (!completed.contains(exIdx)) return@mapIndexedNotNull null
                    exIdx to ex
                }
                .flatMap { (exIdx, ex) ->
                    val reps = parseReps(ex.repsText)
                    (0 until ex.sets).map { setIdx ->
                        ExerciseSet(
                            workoutId = 0L,
                            exerciseName = ex.exerciseName,
                            exerciseIndex = exIdx,
                            setIndex = setIdx,
                            reps = reps,
                            weightKg = 0f,
                        )
                    }
                }
            dao.saveWorkout(workout, sets)
            SoundFx.playAchievement()
            delay(400)
            onCompleted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Salir", tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${routine.routine.emoji} ${routine.routine.name}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "${completed.size} / ${exerciseList.size} ejercicios",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { if (exerciseList.isEmpty()) 0f else completed.size.toFloat() / exerciseList.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.10f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clickable { quoteKey++ },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CookieAvatar(
                    modifier = Modifier.size(120.dp),
                    mood = if (allDone) CookieMood.Happy else CookieMood.Energetic,
                    level = level,
                    isSpeaking = true,
                )
                Spacer(Modifier.width(8.dp))
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                ) {
                    Text(
                        text = if (allDone) completeQuote else hype,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(exerciseList.size) { idx ->
                    val ex = exerciseList[idx]
                    val done = completed.contains(idx)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (done) completed.remove(idx)
                                else {
                                    completed.add(idx)
                                    SoundFx.playSuccess()
                                    quoteKey++
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.05f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ex.exerciseName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                )
                                Text(
                                    buildString {
                                        append("${ex.sets} series × ${ex.repsText}")
                                        if (ex.restSeconds > 0) append("  ·  💤 ${formatRest(ex.restSeconds)}")
                                    },
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }

            val partial = completed.isNotEmpty() && !allDone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (partial) {
                    Button(
                        onClick = {
                            showConfetti = true
                            finishAndSave()
                        },
                        enabled = !saved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Guardar incompleto (${completed.size}/${exerciseList.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Button(
                    onClick = {
                        showConfetti = true
                        finishAndSave()
                    },
                    enabled = allDone && !saved,
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
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (allDone) "¡Cerrar sesión y guardar!" else "Marca todos los ejercicios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        ConfettiOverlay(active = showConfetti, onFinish = { showConfetti = false })
    }
}

private fun parseReps(s: String): Int {
    val first = s.takeWhile { it.isDigit() }
    return first.toIntOrNull() ?: 10
}

private fun formatRest(sec: Int): String {
    if (sec < 60) return "${sec}s"
    val m = sec / 60
    val s = sec % 60
    return if (s == 0) "${m}m" else "${m}m ${s}s"
}
