package com.juan.fittracker.data

enum class WorkoutVibe { Empty, Lazy, Regular, Strong, Beast }

object WorkoutClassifier {
    fun classify(todayWorkouts: List<WorkoutWithSets>): WorkoutVibe {
        if (todayWorkouts.isEmpty()) return WorkoutVibe.Empty
        val totalSets = todayWorkouts.sumOf { it.totalSets }
        val totalVolume = todayWorkouts.sumOf { it.totalVolumeKg.toDouble() }
        return when {
            totalSets >= 25 || totalVolume >= 5000 -> WorkoutVibe.Beast
            totalSets >= 15 || totalVolume >= 2000 -> WorkoutVibe.Strong
            totalSets >= 6 -> WorkoutVibe.Regular
            else -> WorkoutVibe.Lazy
        }
    }

    fun poolFor(vibe: WorkoutVibe): List<String> = when (vibe) {
        WorkoutVibe.Empty -> RolaPhrases.workoutEmpty
        WorkoutVibe.Lazy -> RolaPhrases.workoutLazy
        WorkoutVibe.Regular -> RolaPhrases.workoutRegular
        WorkoutVibe.Strong -> RolaPhrases.workoutStrong
        WorkoutVibe.Beast -> RolaPhrases.workoutBeast
    }
}
