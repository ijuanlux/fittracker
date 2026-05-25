package com.juan.fittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val xp: Int,
) {
    FirstWorkout("first_workout", "Primera pesa", "Tu primera sesión registrada", "🏋️", 10),
    StreakThree("streak_3", "Racha de 3 días", "Entrenó 3 días seguidos", "🔥", 25),
    StreakSeven("streak_7", "Una semana brava", "7 días seguidos sin parar", "💪", 50),
    TenKSteps("ten_k_steps", "10 mil pasos", "Caminó 10.000+ en un día", "👟", 20),
    Madrugador("madrugador", "Madrugador rolo", "Entrenó antes de las 8 a.m.", "🌅", 15),
    Trasnochador("trasnochador", "Trasnochador", "Entrenó después de las 22 h", "🌙", 15),
    PrimerAjiaco("primer_ajiaco", "Sabor santafereño", "Registró un plato bogotano", "🍲", 10),
    Cachetona("cachetona", "Cachetón total", "Se pasó con la comida del día", "😋", 5),
    Marathon("marathon", "Maratón acumulada", "100.000 pasos en total", "🏃", 75),
    PrimeraFoto("primera_foto", "Foto al plato", "Le hizo foto a una comida", "📸", 10),
    DiezPlatos("diez_platos", "Diez platos distintos", "Variedad culinaria registrada", "🍽️", 30),
    Volumen10t("volumen_10t", "10 toneladas", "Levantó 10.000 kg acumulados", "🏋️‍♂️", 50),
    LegDay("leg_day", "Día de pierna", "Primera rutina de pierna completada", "🦵", 15),
    ChestDay("chest_day", "Pecho rolo", "Primera rutina de pecho completada", "💪", 15),
    BackDay("back_day", "Espalda firme", "Primera rutina de espalda completada", "🏋", 15),
    ShouldersDay("shoulders_day", "Hombros pa' arriba", "Primera rutina de hombros completada", "🤸", 15),
    CardioDay("cardio_day", "Cardio rolo", "Primera rutina de cardio completada", "🏃", 15),
    FullBodyDay("full_body_day", "Full body completo", "Primera rutina full body completada", "🔥", 15),
    RoutineWarrior("routine_warrior", "Guerrero de rutina", "Misma rutina completada 10 veces", "⚔", 60),
    VarietyKing("variety_king", "Variado al máx", "4 zonas distintas entrenadas", "🎯", 40),
    CustomRoutine("custom_routine", "Rutina propia", "Completó una rutina propia (no plantilla)", "✨", 20),
    CardioStreakWeek("cardio_streak_week", "Constante cardio", "3+ sesiones de cardio en una semana", "💓", 30),
    Maratonista("maratonista", "Maratonista rolo", "10 km de cardio acumulados", "🥇", 40),
    CiclaRola("cicla_rola", "Cicla rola", "100 km en bici acumulados", "🚴", 50),
    Salsero("salsero", "Salsero del Bachué", "5 sesiones de baile registradas", "💃", 25),
    ;

    companion object {
        fun byId(id: String): Achievement? = entries.find { it.id == id }
    }
}

@Entity(tableName = "achievements")
data class AchievementUnlock(
    @PrimaryKey val id: String,
    val unlockedAtMs: Long,
)
