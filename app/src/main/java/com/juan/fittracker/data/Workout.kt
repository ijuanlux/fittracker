package com.juan.fittracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMs: Long,
    val notes: String = "",
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val exerciseIndex: Int,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Float,
)

data class WorkoutWithSets(
    @Embedded val workout: Workout,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val sets: List<ExerciseSet>,
) {
    val exerciseCount: Int get() = sets.map { it.exerciseIndex }.distinct().size
    val totalSets: Int get() = sets.size
    val totalVolumeKg: Float get() = sets.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
    val exerciseNames: List<String>
        get() = sets.sortedBy { it.exerciseIndex }
            .map { it.exerciseIndex to it.exerciseName }
            .distinct()
            .map { it.second }
    fun setsByExercise(): List<Pair<String, List<ExerciseSet>>> =
        sets.groupBy { it.exerciseIndex }
            .toSortedMap()
            .map { (_, groupSets) ->
                val name = groupSets.firstOrNull()?.exerciseName.orEmpty()
                name to groupSets.sortedBy { it.setIndex }
            }
}
