package com.juan.fittracker.ui.workouts

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.Routine
import com.juan.fittracker.data.RoutineExerciseEntity
import com.juan.fittracker.data.RoutineWithExercises
import kotlinx.coroutines.launch

@Composable
fun RoutinesEditorScreen(onBack: () -> Unit) {
    var editing by remember { mutableStateOf<RoutineWithExercises?>(null) }
    var creating by remember { mutableStateOf(false) }
    BackHandler(enabled = editing != null || creating) {
        editing = null
        creating = false
    }
    BackHandler(enabled = editing == null && !creating) { onBack() }

    when {
        creating -> RoutineFormScreen(
            initial = null,
            onCancel = { creating = false },
            onSaved = { creating = false },
        )
        editing != null -> RoutineFormScreen(
            initial = editing,
            onCancel = { editing = null },
            onSaved = { editing = null },
        )
        else -> RoutinesListScreen(
            onBack = onBack,
            onEdit = { editing = it },
            onCreate = { creating = true },
        )
    }
}

@Composable
private fun RoutinesListScreen(
    onBack: () -> Unit,
    onEdit: (RoutineWithExercises) -> Unit,
    onCreate: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).routineDao() }
    val routines by dao.observeAll().collectAsState(initial = emptyList())
    var pendingDelete by remember { mutableStateOf<RoutineWithExercises?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "Mis rutinas",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(routines, key = { it.routine.id }) { rwx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(rwx) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rwx.routine.emoji, fontSize = 30.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rwx.routine.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${rwx.exercises.size} ejercicios",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                )
                            }
                            IconButton(onClick = { onEdit(rwx) }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { pendingDelete = rwx }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Borrar",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onCreate,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nueva rutina")
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Borrar rutina?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se eliminará \"${toDelete.routine.name}\" y todos sus ejercicios.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { dao.deleteRoutine(toDelete.routine) }
                }) {
                    Text(
                        "Borrar",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
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

private class RoutineExerciseForm(
    name: String = "",
    sets: String = "3",
    reps: String = "10",
    rest: String = "60",
) {
    var name by mutableStateOf(name)
    var sets by mutableStateOf(sets)
    var reps by mutableStateOf(reps)
    var rest by mutableStateOf(rest)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineFormScreen(
    initial: RoutineWithExercises?,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).routineDao() }

    var name by remember { mutableStateOf(initial?.routine?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.routine?.emoji ?: "💪") }
    val exercises = remember {
        mutableStateListOf<RoutineExerciseForm>().apply {
            if (initial != null) {
                addAll(initial.sortedExercises.map {
                    RoutineExerciseForm(it.exerciseName, it.sets.toString(), it.repsText, it.restSeconds.toString())
                })
            } else {
                add(RoutineExerciseForm())
            }
        }
    }
    var saving by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank() &&
        exercises.any { it.name.isNotBlank() && (it.sets.toIntOrNull() ?: 0) > 0 } && !saving

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
                if (initial == null) "Nueva rutina" else "Editar rutina",
                color = MaterialTheme.colorScheme.primary,
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("Emoji", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                    colors = darkTfColors(),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre rutina") },
                    placeholder = { Text("Pierna, Pecho...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = darkTfColors(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Ejercicios",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            exercises.forEachIndexed { idx, ex ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${idx + 1}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(24.dp),
                            )
                            OutlinedTextField(
                                value = ex.name,
                                onValueChange = { ex.name = it },
                                label = { Text("Ejercicio", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = darkTfColors(),
                            )
                            if (exercises.size > 1) {
                                IconButton(onClick = { exercises.removeAt(idx) }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Quitar",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row {
                            OutlinedTextField(
                                value = ex.sets,
                                onValueChange = { v -> ex.sets = v.filter { it.isDigit() }.take(2) },
                                label = { Text("Series", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = darkTfColors(),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = ex.reps,
                                onValueChange = { v -> ex.reps = v.take(10) },
                                label = { Text("Reps", fontSize = 11.sp) },
                                placeholder = { Text("8-10, 30s...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = darkTfColors(),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = ex.rest,
                                onValueChange = { v -> ex.rest = v.filter { it.isDigit() }.take(4) },
                                label = { Text("Descanso (s)", fontSize = 11.sp) },
                                placeholder = { Text("60", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = darkTfColors(),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { exercises.add(RoutineExerciseForm()) }) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text("Añadir ejercicio", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    val routine = Routine(
                        id = initial?.routine?.id ?: 0L,
                        name = name.trim(),
                        emoji = emoji.ifBlank { "💪" },
                        createdAtMs = initial?.routine?.createdAtMs ?: System.currentTimeMillis(),
                    )
                    val exs = exercises
                        .filter { it.name.isNotBlank() }
                        .mapIndexed { i, e ->
                            RoutineExerciseEntity(
                                routineId = 0L,
                                exerciseName = e.name.trim(),
                                sets = e.sets.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                repsText = e.reps.ifBlank { "10" },
                                orderIndex = i,
                                restSeconds = e.rest.toIntOrNull()?.coerceIn(0, 600) ?: 60,
                            )
                        }
                    dao.saveRoutine(routine, exs)
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            ),
        ) {
            Text(
                if (initial == null) "Crear rutina" else "Guardar cambios",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkTfColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
)
