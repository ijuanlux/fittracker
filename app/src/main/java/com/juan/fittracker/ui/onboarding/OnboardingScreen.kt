package com.juan.fittracker.ui.onboarding

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.TrainingFrequency
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.ui.CookieAvatar

private val BgTop = Color(0xFF15100B)
private val BgBottom = Color(0xFF2E1F14)
private val Accent = Color(0xFFFFC58A)
private val OnDark = Color(0xFFEDE3D6)

@Composable
fun OnboardingScreen(onFinished: (UserProfile) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 5
    var age by remember { mutableIntStateOf(UserProfile.Default.age) }
    var sex by remember { mutableStateOf(UserProfile.Default.sex) }
    var frequency by remember { mutableStateOf(UserProfile.Default.frequency) }
    var heightCm by remember { mutableIntStateOf(UserProfile.Default.heightCm) }
    var weightKg by remember { mutableIntStateOf(UserProfile.Default.weightKg) }

    BackHandler(enabled = step > 0) { step-- }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
        ) {
            if (step > 0) {
                LinearProgressIndicator(
                    progress = { step.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Accent,
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
                Spacer(Modifier.height(32.dp))
            }

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
                label = "step",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(onStart = { step = 1 })
                    1 -> AgeStep(
                        value = age,
                        onChange = { age = it },
                        onBack = { step-- },
                        onNext = { step++ },
                    )
                    2 -> SexStep(
                        selected = sex,
                        onSelect = { sex = it },
                        onBack = { step-- },
                        onNext = { step++ },
                    )
                    3 -> FrequencyStep(
                        selected = frequency,
                        onSelect = { frequency = it },
                        onBack = { step-- },
                        onNext = { step++ },
                    )
                    4 -> BodyStep(
                        heightCm = heightCm,
                        weightKg = weightKg,
                        onHeightChange = { heightCm = it },
                        onWeightChange = { weightKg = it },
                        onBack = { step-- },
                        onConfirm = {
                            onFinished(
                                UserProfile(
                                    age = age,
                                    sex = sex,
                                    frequency = frequency,
                                    heightCm = heightCm,
                                    weightKg = weightKg,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CookieAvatar(modifier = Modifier.size(220.dp))
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Bienvenido a",
            color = OnDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = "Galleta FitTracker",
            color = Accent,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tu compañera de entreno colombiana.\nRegistra, evoluciona, sube de nivel.",
            color = OnDark.copy(alpha = 0.75f),
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color(0xFF15100B),
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AgeStep(value: Int, onChange: (Int) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "¿Cuántos años tienes?",
        subtitle = "Para adaptar las recomendaciones de entreno.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true,
    ) {
        BigValue("$value", suffix = "años")
        Spacer(Modifier.height(40.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 13f..90f,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun SexStep(selected: Sex, onSelect: (Sex) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "¿Cómo te identificas?",
        subtitle = "Usamos esto solo para cálculos de calorías.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Sex.entries.forEach { option ->
                ChoiceCard(
                    label = option.label,
                    selected = selected == option,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun FrequencyStep(
    selected: TrainingFrequency,
    onSelect: (TrainingFrequency) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(
        title = "¿Con qué frecuencia entrenas?",
        subtitle = "Puedes cambiarlo más tarde en tu perfil.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainingFrequency.entries.forEach { option ->
                ChoiceCard(
                    label = option.label,
                    sublabel = option.days,
                    selected = selected == option,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun BodyStep(
    heightCm: Int,
    weightKg: Int,
    onHeightChange: (Int) -> Unit,
    onWeightChange: (Int) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    StepScaffold(
        title = "Tu cuerpo",
        subtitle = "Última pregunta. Promesa.",
        onBack = onBack,
        onNext = onConfirm,
        nextLabel = "Empezar",
        nextEnabled = true,
    ) {
        Text("Altura", color = OnDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        BigValue("$heightCm", suffix = "cm", small = true)
        Slider(
            value = heightCm.toFloat(),
            onValueChange = { onHeightChange(it.toInt()) },
            valueRange = 140f..220f,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
        )
        Spacer(Modifier.height(28.dp))
        Text("Peso", color = OnDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        BigValue("$weightKg", suffix = "kg", small = true)
        Slider(
            value = weightKg.toFloat(),
            onValueChange = { onWeightChange(it.toInt()) },
            valueRange = 35f..200f,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String = "Siguiente",
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            color = OnDark,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = OnDark.copy(alpha = 0.65f),
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(40.dp))
        Column(modifier = Modifier.weight(1f, fill = true)) { content() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Text("Atrás", color = OnDark)
            }
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF15100B),
                ),
            ) {
                Text(nextLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BigValue(value: String, suffix: String, small: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            color = Accent,
            fontSize = if (small) 56.sp else 88.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = suffix,
            color = OnDark.copy(alpha = 0.6f),
            fontSize = if (small) 18.sp else 24.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = if (small) 10.dp else 16.dp),
        )
    }
}

@Composable
private fun ChoiceCard(
    label: String,
    sublabel: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (sublabel != null) {
                    Text(sublabel, fontSize = 13.sp, color = OnDark.copy(alpha = 0.6f))
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.06f),
            labelColor = OnDark,
            selectedContainerColor = Accent.copy(alpha = 0.20f),
            selectedLabelColor = Accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.15f),
            selectedBorderColor = Accent,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    )
}
