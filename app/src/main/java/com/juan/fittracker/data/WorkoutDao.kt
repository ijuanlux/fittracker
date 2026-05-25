package com.juan.fittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY dateEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutWithSets>>

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Insert
    suspend fun insertSets(sets: List<ExerciseSet>)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Transaction
    suspend fun saveWorkout(workout: Workout, sets: List<ExerciseSet>) {
        val id = insertWorkout(workout)
        if (sets.isNotEmpty()) {
            insertSets(sets.map { it.copy(workoutId = id) })
        }
    }
}
