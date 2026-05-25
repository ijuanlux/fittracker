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
