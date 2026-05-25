package com.juan.fittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_entries ORDER BY dateEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<MealEntry>>

    @Insert
    suspend fun insert(meal: MealEntry): Long

    @Delete
    suspend fun delete(meal: MealEntry)
}
