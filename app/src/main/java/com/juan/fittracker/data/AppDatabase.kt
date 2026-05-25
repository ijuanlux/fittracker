package com.juan.fittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Database(
    entities = [Workout::class, ExerciseSet::class, MealEntry::class, AchievementUnlock::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun mealDao(): MealDao
    abstract fun achievementDao(): AchievementDao
}

object Db {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fittracker.db",
        )
            .fallbackToDestructiveMigration()
            .build()
            .also { instance = it }
    }
}

private val SpanishLocale = Locale("es", "ES")
private val ShortFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", SpanishLocale)
private val LongFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SpanishLocale)

fun formatShortDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(ShortFormatter)

fun formatLongDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(LongFormatter)
        .replaceFirstChar { it.uppercase(SpanishLocale) }
