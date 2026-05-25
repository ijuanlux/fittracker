package com.juan.fittracker.data

import java.time.Instant
import java.time.ZoneId

data class FoodStats(
    val rangeLabel: String,
    val totalMeals: Int,
    val totalKcal: Int,
    val daysWithMeals: Int,
    val avgPerDay: Float,
    val topFoods: List<Pair<String, Int>>,
    val mealsByType: List<Pair<String, Int>>,
    val kcalByType: List<Pair<String, Int>>,
)

object FoodStatsComputer {
    fun compute(
        all: List<MealEntry>,
        startMs: Long,
        endMs: Long,
        rangeLabel: String,
    ): FoodStats {
        val filtered = all.filter { it.dateEpochMs in startMs..endMs }
        val totalMeals = filtered.size
        val totalKcal = filtered.sumOf { it.kcal }
        val zone = ZoneId.systemDefault()
        val distinctDays = filtered
            .map { Instant.ofEpochMilli(it.dateEpochMs).atZone(zone).toLocalDate() }
            .toSet()
        val avgPerDay = if (distinctDays.isEmpty()) 0f else totalKcal.toFloat() / distinctDays.size

        val topFoods = filtered
            .groupingBy { it.name.trim() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val mealsByType = filtered
            .groupingBy { it.mealType }
            .eachCount()
            .entries
            .map { it.key.label to it.value }
            .sortedByDescending { it.second }

        val kcalByType = filtered
            .groupBy { it.mealType }
            .map { (type, list) -> type.label to list.sumOf { it.kcal } }
            .sortedByDescending { it.second }

        return FoodStats(
            rangeLabel = rangeLabel,
            totalMeals = totalMeals,
            totalKcal = totalKcal,
            daysWithMeals = distinctDays.size,
            avgPerDay = avgPerDay,
            topFoods = topFoods,
            mealsByType = mealsByType,
            kcalByType = kcalByType,
        )
    }
}
