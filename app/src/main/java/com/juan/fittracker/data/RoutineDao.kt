package com.juan.fittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAtMs ASC")
    fun observeAll(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines")
    suspend fun getAll(): List<RoutineWithExercises>

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Insert
    suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesFor(routineId: Long)

    @Query("UPDATE routines SET name = :name, emoji = :emoji WHERE id = :id")
    suspend fun updateRoutineMeta(id: Long, name: String, emoji: String)

    @Transaction
    suspend fun saveRoutine(routine: Routine, exercises: List<RoutineExerciseEntity>) {
        val id = if (routine.id == 0L) {
            insertRoutine(routine)
        } else {
            updateRoutineMeta(routine.id, routine.name, routine.emoji)
            deleteExercisesFor(routine.id)
            routine.id
        }
        if (exercises.isNotEmpty()) {
            insertExercises(exercises.map { it.copy(routineId = id) })
        }
    }
}
