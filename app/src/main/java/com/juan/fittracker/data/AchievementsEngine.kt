package com.juan.fittracker.data

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object AchievementsEngine {

    private const val BOGOTANO_KEYWORDS_REGEX =
        "ajiaco|tamal|changua|almojabana|almojábana|aguapanela|buñuelo|caldo de costilla|empanada|arepa con queso|sancocho"

    suspend fun evaluateAndPersist(
        context: Context,
        workouts: List<WorkoutWithSets>,
        meals: List<MealEntry>,
        todaySteps: Long,
        intakeToday: Int,
        targetKcal: Int,
    ): List<Achievement> {
        val dao = Db.get(context).achievementDao()
        val already = dao.getAll().map { it.id }.toSet()
        val now = System.currentTimeMillis()
        val newlyUnlocked = mutableListOf<Achievement>()

        val candidates = computeCandidates(workouts, meals, todaySteps, intakeToday, targetKcal)
        for (a in candidates) {
            if (a.id !in already) {
                dao.unlock(AchievementUnlock(id = a.id, unlockedAtMs = now))
                newlyUnlocked += a
            }
        }
        return newlyUnlocked
    }

    fun computeCandidates(
        workouts: List<WorkoutWithSets>,
        meals: List<MealEntry>,
        todaySteps: Long,
        intakeToday: Int,
        targetKcal: Int,
    ): Set<Achievement> {
        val out = mutableSetOf<Achievement>()

        if (workouts.isNotEmpty()) out += Achievement.FirstWorkout
        val streak = workoutStreakDays(workouts)
        if (streak >= 3) out += Achievement.StreakThree
        if (streak >= 7) out += Achievement.StreakSeven
        if (todaySteps >= 10_000) out += Achievement.TenKSteps
        if (workouts.any { hourOf(it.workout.dateEpochMs) < 8 }) out += Achievement.Madrugador
        if (workouts.any { hourOf(it.workout.dateEpochMs) >= 22 }) out += Achievement.Trasnochador
        val regex = Regex(BOGOTANO_KEYWORDS_REGEX, RegexOption.IGNORE_CASE)
        if (meals.any { regex.containsMatchIn(it.name) }) out += Achievement.PrimerAjiaco
        if (targetKcal > 0 && intakeToday > targetKcal * 1.3f) out += Achievement.Cachetona
        // Marathon: total step count tracked in HC daily metrics is not summed here; use workouts as proxy isn't right.
        // We approximate from sumOf today + accumulated metric via separate path; for now skip until last7Days passed.
        if (meals.any { it.photoPath != null }) out += Achievement.PrimeraFoto
        if (meals.map { it.name.lowercase().trim() }.distinct().size >= 10) out += Achievement.DiezPlatos
        val totalKg = workouts.sumOf { w ->
            w.sets.sumOf { (it.reps * it.weightKg).toDouble() }
        }
        if (totalKg >= 10_000) out += Achievement.Volumen10t

        return out
    }

    fun workoutStreakDays(workouts: List<WorkoutWithSets>): Int {
        if (workouts.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val dates = workouts
            .map { Instant.ofEpochMilli(it.workout.dateEpochMs).atZone(zone).toLocalDate() }
            .toSortedSet()
            .toList()
            .reversed()
        val today = LocalDate.now()
        if (dates[0] != today && dates[0] != today.minusDays(1)) return 0
        var streak = 1
        for (i in 1 until dates.size) {
            if (dates[i] == dates[i - 1].minusDays(1)) streak++ else break
        }
        return streak
    }

    private fun hourOf(epochMs: Long): Int =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()).hour
}
