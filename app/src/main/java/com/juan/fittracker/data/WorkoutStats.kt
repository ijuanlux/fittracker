package com.juan.fittracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutStats(
    val rangeLabel: String,
    val totalWorkouts: Int,
    val totalSets: Int,
    val totalReps: Long,
    val totalVolumeKg: Float,
    val totalDays: Int,        // distinct days with at least one workout
    val avgPerWeek: Float,
    val currentStreak: Int,
    val topExercises: List<Pair<String, Int>>,
    val topRoutines: List<Pair<String, Int>>,
    val setsByMuscleGroup: List<Pair<String, Int>>,
)

object WorkoutStatsComputer {
    fun compute(
        all: List<WorkoutWithSets>,
        startMs: Long,
        endMs: Long,
        rangeLabel: String,
    ): WorkoutStats {
        val filtered = all.filter { it.workout.dateEpochMs in startMs..endMs }
        val totalWorkouts = filtered.size
        val totalSets = filtered.sumOf { it.totalSets }
        val totalReps = filtered.sumOf { w -> w.sets.sumOf { it.reps.toLong() } }
        val totalVolumeKg = filtered.sumOf { w ->
            w.sets.sumOf { (it.reps * it.weightKg).toDouble() }
        }.toFloat()
        val zone = ZoneId.systemDefault()
        val distinctDays = filtered
            .map { Instant.ofEpochMilli(it.workout.dateEpochMs).atZone(zone).toLocalDate() }
            .toSet()
        val daysInRange = ((endMs - startMs) / (24L * 3_600_000L)).coerceAtLeast(1)
        val weeks = (daysInRange / 7f).coerceAtLeast(1f / 7f)
        val avgPerWeek = totalWorkouts / weeks
        val currentStreak = AchievementsEngine.workoutStreakDays(all)

        val exerciseCounts = filtered.flatMap { it.sets }
            .groupingBy { it.exerciseName }.eachCount()
        val topExercises = exerciseCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        val routineCounts = filtered
            .mapNotNull { w ->
                val n = w.workout.notes
                if (n.startsWith("Rutina ")) n.removePrefix("Rutina ") else null
            }
            .groupingBy { it }
            .eachCount()
        val topRoutines = routineCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        val muscleCounts = mutableMapOf<String, Int>()
        filtered.forEach { w ->
            w.sets.forEach { set ->
                val group = MuscleClassifier.classify(set.exerciseName, w.workout.notes)
                muscleCounts[group] = (muscleCounts[group] ?: 0) + 1
            }
        }
        val setsByMuscleGroup = muscleCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        return WorkoutStats(
            rangeLabel = rangeLabel,
            totalWorkouts = totalWorkouts,
            totalSets = totalSets,
            totalReps = totalReps,
            totalVolumeKg = totalVolumeKg,
            totalDays = distinctDays.size,
            avgPerWeek = avgPerWeek,
            currentStreak = currentStreak,
            topExercises = topExercises,
            topRoutines = topRoutines,
            setsByMuscleGroup = setsByMuscleGroup,
        )
    }
}

enum class StatsRangePreset(val label: String, val days: Int?) {
    Last7("Últimos 7 días", 7),
    Last30("Últimos 30 días", 30),
    Last90("Últimos 90 días", 90),
    AllTime("Todo el tiempo", null),
    Custom("Personalizado", null),
}
