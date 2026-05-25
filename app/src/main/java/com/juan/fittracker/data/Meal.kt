package com.juan.fittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class MealType(val label: String, val emoji: String) {
    Desayuno("Desayuno", "🌅"),
    Almuerzo("Almuerzo", "🍽"),
    Cena("Cena", "🌙"),
    Snack("Snack", "🍪"),
    ;

    companion object {
        fun ofHour(hour: Int): MealType = when (hour) {
            in 5..10 -> Desayuno
            in 11..15 -> Almuerzo
            in 16..21 -> Cena
            else -> Snack
        }

        fun now(): MealType = ofHour(LocalDateTime.now().hour)
    }
}

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Int,
    val type: String,
    val dateEpochMs: Long,
    val photoPath: String? = null,
) {
    val mealType: MealType
        get() = runCatching { MealType.valueOf(type) }.getOrDefault(MealType.Snack)
}

fun List<MealEntry>.todayOnly(): List<MealEntry> {
    val today = java.time.LocalDate.now()
    return filter {
        java.time.Instant.ofEpochMilli(it.dateEpochMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate() == today
    }
}

fun List<MealEntry>.totalKcal(): Int = sumOf { it.kcal }
