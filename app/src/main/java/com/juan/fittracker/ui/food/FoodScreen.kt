package com.juan.fittracker.ui.food

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import com.juan.fittracker.data.AchievementUnlock
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.Db
import com.juan.fittracker.data.FoodClassifier
import com.juan.fittracker.data.FoodComments
import com.juan.fittracker.data.FoodCuisine
import com.juan.fittracker.data.FoodVibe
import com.juan.fittracker.data.Levels
import com.juan.fittracker.data.Nutrition
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.SoundFx
import com.juan.fittracker.ui.CookieAvatar
import com.juan.fittracker.ui.effects.ConfettiOverlay
import com.juan.fittracker.data.MealEntry
import com.juan.fittracker.data.MealType
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.formatLongDate
import com.juan.fittracker.data.formatShortDate
import com.juan.fittracker.data.quickFoodsFor
import com.juan.fittracker.data.todayOnly
import com.juan.fittracker.data.totalKcal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
}

@Composable
fun FoodScreen(profile: UserProfile) {
    var mode by remember { mutableStateOf<Mode>(Mode.List) }
    var lastComment by remember { mutableStateOf<String?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = mode is Mode.Adding) { mode = Mode.List }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = mode, animationSpec = tween(300), label = "food-mode") { current ->
            when (current) {
                Mode.List -> FoodList(
                    profile = profile,
                    bannerComment = lastComment,
                    onAdd = { mode = Mode.Adding },
                    onCommentShown = { lastComment = null },
                )
                Mode.Adding -> AddMealForm(
                    profile = profile,
                    onCancel = { mode = Mode.List },
                    onSaved = { comment ->
                        SoundFx.playSuccess()
                        showConfetti = true
                        lastComment = comment
                        mode = Mode.List
                    },
                )
            }
        }
        ConfettiOverlay(active = showConfetti, onFinish = { showConfetti = false })
    }
}

@Composable
private fun FoodList(
    profile: UserProfile,
    bannerComment: String?,
    onAdd: () -> Unit,
    onCommentShown: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).mealDao() }
    val achievementDao = remember(context) { Db.get(context).achievementDao() }
    val allMeals by dao.observeAll().collectAsState(initial = emptyList())
    val unlocks by achievementDao.observeAll().collectAsState(initial = emptyList())
    val todayMeals = allMeals.todayOnly()
    val todayKcal = todayMeals.totalKcal()
    val targetKcal = remember(profile) { Nutrition.targetKcal(profile).toInt() }
    val vibe = remember(todayMeals.size, todayKcal, targetKcal) {
        FoodClassifier.classify(todayMeals, targetKcal)
    }
    val galletoideLevel = Levels.level(Levels.totalXp(unlocks))
    val cookieMood = when (vibe) {
        FoodVibe.Empty -> CookieMood.Neutral
        FoodVibe.Fit -> CookieMood.Happy
        FoodVibe.Neutral -> CookieMood.Neutral
        FoodVibe.Heavy -> CookieMood.Stuffed
    }

    var ambientKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            ambientKey++
        }
    }
    val ambient = remember(ambientKey, profile.sex, vibe) {
        RolaPhrases.pick(FoodClassifier.poolFor(vibe), ambientKey, profile.sex)
    }
    var speaking by remember { mutableStateOf(true) }
    LaunchedEffect(ambient) {
        speaking = true
        delay(2200)
        speaking = false
    }

    LaunchedEffect(bannerComment) {
        if (bannerComment != null) {
            delay(5000)
            onCommentShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text("Comida", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lleva la cuenta de lo que has comido hoy",
                    color = OnDark.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                GalletoideMealCard(
                    quote = ambient,
                    mood = cookieMood,
                    level = galletoideLevel,
                    speaking = speaking,
                    onTap = { ambientKey++ },
                )
            }
            item {
                TodayKcalCard(todayKcal = todayKcal)
            }
            item {
                AnimatedVisibility(visible = bannerComment != null) {
                    CommentBanner(bannerComment.orEmpty())
                }
            }
            if (todayMeals.isEmpty()) {
                item {
                    EmptyTodayState()
                }
            } else {
                items(todayMeals, key = { it.id }) { meal ->
                    MealRow(meal = meal, onDelete = { scope.launch { dao.delete(meal) } })
                }
            }
            if (allMeals.size > todayMeals.size) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Días anteriores",
                        color = OnDark.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(allMeals - todayMeals.toSet(), key = { it.id }) { meal ->
                    MealRow(meal = meal, onDelete = { scope.launch { dao.delete(meal) } }, showDate = true)
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = Accent,
            contentColor = BgDark,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir comida")
        }
    }
}

@Composable
private fun TodayKcalCard(todayKcal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Hoy has ingerido", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$todayKcal", color = OnDark, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text("kcal", color = OnDark.copy(alpha = 0.6f), fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun GalletoideMealCard(
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
            Spacer(Modifier.width(12.dp))
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
private fun CommentBanner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.15f)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🍪", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Text(text, color = OnDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyTodayState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                tint = Accent.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Aún no has registrado comidas hoy",
                color = OnDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Toca el + para añadir tu primera",
                color = OnDark.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MealRow(meal: MealEntry, onDelete: () -> Unit, showDate: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (meal.photoPath != null) {
                MealThumbnail(path = meal.photoPath, size = 56.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(meal.mealType.emoji, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, color = OnDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (showDate) "${meal.mealType.label} · ${formatShortDate(meal.dateEpochMs)}"
                    else meal.mealType.label,
                    color = OnDark.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Text("${meal.kcal} kcal", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = Danger.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun MealThumbnail(path: String, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(path) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Foto del plato",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = OnDark.copy(alpha = 0.4f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealForm(
    profile: UserProfile,
    onCancel: () -> Unit,
    onSaved: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { Db.get(context).mealDao() }

    var name by remember { mutableStateOf("") }
    var kcalText by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.now()) }
    var saving by remember { mutableStateOf(false) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var dateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var cuisine by remember { mutableStateOf(FoodCuisine.Rola) }
    val selectedPresets = remember { mutableStateListOf<com.juan.fittracker.data.FoodPreset>() }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingFile
        if (success && file != null && file.exists() && file.length() > 0) {
            photoPath = file.absolutePath
        } else {
            pendingFile?.delete()
        }
        pendingFile = null
    }

    val canSave = name.isNotBlank() && (kcalText.toIntOrNull() ?: 0) > 0 && !saving
    val bro = when (profile.sex) {
        Sex.Male -> "hermano"
        Sex.Female -> "hermana"
        Sex.Unspecified -> "marica"
    }

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
            Text("Nueva comida", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Date selector
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
                    Spacer(Modifier.width(12.dp))
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

            Text("Tipo de comida", color = OnDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealType.entries.forEach { mt ->
                    FilterChip(
                        selected = mealType == mt,
                        onClick = { mealType = mt },
                        label = { Text("${mt.emoji} ${mt.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = OnDark,
                            selectedContainerColor = Accent.copy(alpha = 0.25f),
                            selectedLabelColor = Accent,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Photo preview / capture
            PhotoSection(
                photoPath = photoPath,
                onTake = {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return@PhotoSection
                    dir.mkdirs()
                    val file = File(dir, "meal_${UUID.randomUUID()}.jpg")
                    pendingFile = file
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    takePictureLauncher.launch(uri)
                },
                onRemove = {
                    photoPath?.let { File(it).delete() }
                    photoPath = null
                },
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre", color = OnDark.copy(alpha = 0.6f)) },
                placeholder = { Text("Tamal, ajiaco, ensalada...", color = OnDark.copy(alpha = 0.35f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = kcalText,
                onValueChange = { v -> kcalText = v.filter { it.isDigit() }.take(5) },
                label = { Text("Calorías (kcal)", color = OnDark.copy(alpha = 0.6f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors(),
            )

            Spacer(Modifier.height(20.dp))
            Text("Atajos", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Toca un plato para autocompletar",
                color = OnDark.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            CuisineToggle(cuisine = cuisine, onChange = { cuisine = it })
            Spacer(Modifier.height(12.dp))
            QuickFoodsGrid(
                foods = quickFoodsFor(cuisine),
                selected = selectedPresets,
                onToggle = { p ->
                    if (selectedPresets.contains(p)) selectedPresets.remove(p)
                    else selectedPresets.add(p)
                },
            )
            if (selectedPresets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        val toInsert = selectedPresets.toList()
                        scope.launch {
                            toInsert.forEach { p ->
                                dao.insert(
                                    MealEntry(
                                        name = p.name,
                                        kcal = p.kcal,
                                        type = mealType.name,
                                        dateEpochMs = dateMs,
                                        photoPath = null,
                                    ),
                                )
                            }
                            val first = toInsert.first().name
                            val comment = FoodComments.commentFor(first, bro)
                            onSaved(comment ?: "¡Añadidos ${toInsert.size} platos, ${bro}!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                    Text(
                        "Añadir ${selectedPresets.size} seleccionados",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    val meal = MealEntry(
                        name = name.trim(),
                        kcal = kcalText.toInt(),
                        type = mealType.name,
                        dateEpochMs = dateMs,
                        photoPath = photoPath,
                    )
                    dao.insert(meal)
                    val comment = FoodComments.commentFor(meal.name, bro)
                    onSaved(comment)
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
            Text("Añadir comida", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuisineToggle(cuisine: FoodCuisine, onChange: (FoodCuisine) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        FoodCuisine.entries.forEachIndexed { idx, c ->
            SegmentedButton(
                selected = cuisine == c,
                onClick = { onChange(c) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = FoodCuisine.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Accent.copy(alpha = 0.25f),
                    activeContentColor = Accent,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = OnDark.copy(alpha = 0.7f),
                    activeBorderColor = Accent,
                    inactiveBorderColor = OnDark.copy(alpha = 0.2f),
                ),
            ) {
                Text(c.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoPath: String?,
    onTake: () -> Unit,
    onRemove: () -> Unit,
) {
    if (photoPath != null) {
        val bitmap = remember(photoPath) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(photoPath, opts)?.asImageBitmap()
            }.getOrNull()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto del plato",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onTake,
                    label = { Text("Repetir", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        labelColor = OnDark,
                        leadingIconContentColor = OnDark,
                    ),
                )
                AssistChip(
                    onClick = onRemove,
                    label = { Text("Quitar", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        labelColor = Danger,
                        leadingIconContentColor = Danger,
                    ),
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onTake,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = BgDark,
                    ),
                ) {
                    Text("Tomar foto", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("(opcional)", color = OnDark.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QuickFoodsGrid(
    foods: List<com.juan.fittracker.data.FoodPreset>,
    selected: List<com.juan.fittracker.data.FoodPreset>,
    onToggle: (com.juan.fittracker.data.FoodPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        foods.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { food ->
                    val isSelected = selected.contains(food)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(food) },
                        label = {
                            Text(
                                "${food.name} · ${food.kcal} kcal",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = OnDark,
                            selectedContainerColor = Accent.copy(alpha = 0.25f),
                            selectedLabelColor = Accent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
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
