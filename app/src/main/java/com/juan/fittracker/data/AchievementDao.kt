package com.juan.fittracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementUnlock>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementUnlock>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(unlock: AchievementUnlock): Long
}
