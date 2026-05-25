package com.juan.fittracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class CardioType(val label: String, val emoji: String, val metBase: Double) {
    Correr("Correr", "🏃", 8.5),
    Caminar("Caminar", "🚶", 3.8),
    Bici("Bicicleta", "🚴", 7.5),
    Eliptica("Elíptica", "🌀", 5.0),
    Natacion("Natación", "🏊", 7.0),
    Baile("Baile / Salsa", "💃", 5.5),
    Futbol("Fútbol", "⚽", 7.0),
    Hiit("HIIT", "🔥", 9.0),
    Otro("Otro", "🏅", 5.0),
}

enum class CardioIntensity(val label: String, val multiplier: Double) {
    Suave("Suave", 0.85),
    Media("Media", 1.0),
    Fuerte("Fuerte", 1.20),
    Brutal("Brutal", 1.40),
}

enum class CardioSource(val label: String) {
    Manual("Manual"),
    HealthConnect("Reloj"),
}

@Entity(tableName = "cardio_sessions")
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMs: Long,
    val typeKey: String,
    val durationMin: Int,
    val distanceKm: Float? = null,
    val intensityKey: String = CardioIntensity.Media.name,
    val kcal: Int? = null,
    val sourceKey: String = CardioSource.Manual.name,
    val externalId: String? = null,
    val notes: String? = null,
) {
    val type: CardioType
        get() = runCatching { CardioType.valueOf(typeKey) }.getOrDefault(CardioType.Otro)
    val intensity: CardioIntensity
        get() = runCatching { CardioIntensity.valueOf(intensityKey) }.getOrDefault(CardioIntensity.Media)
    val source: CardioSource
        get() = runCatching { CardioSource.valueOf(sourceKey) }.getOrDefault(CardioSource.Manual)
}

object CardioKcal {
    fun estimate(type: CardioType, intensity: CardioIntensity, durationMin: Int, weightKg: Float): Int {
        if (durationMin <= 0) return 0
        val w = if (weightKg > 0f) weightKg else 70f
        val met = type.metBase * intensity.multiplier
        val kcal = met * w * (durationMin / 60.0)
        return kcal.toInt().coerceAtLeast(0)
    }
}

@Dao
interface CardioDao {
    @Query("SELECT * FROM cardio_sessions ORDER BY dateEpochMs DESC")
    fun observeAll(): Flow<List<CardioSession>>

    @Query("SELECT * FROM cardio_sessions WHERE dateEpochMs BETWEEN :fromMs AND :toMs ORDER BY dateEpochMs DESC")
    suspend fun getRange(fromMs: Long, toMs: Long): List<CardioSession>

    @Query("SELECT * FROM cardio_sessions WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternalId(externalId: String): CardioSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CardioSession): Long

    @Query("DELETE FROM cardio_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
