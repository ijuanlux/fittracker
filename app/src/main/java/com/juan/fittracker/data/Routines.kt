package com.juan.fittracker.data

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "💪",
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseName: String,
    val sets: Int,
    val repsText: String,
    val orderIndex: Int,
    val restSeconds: Int = 60,
    val weightKg: Float = 0f,
)

data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>,
) {
    val sortedExercises: List<RoutineExerciseEntity>
        get() = exercises.sortedBy { it.orderIndex }
}

// ============ Seed templates ============

data class SeedExercise(val name: String, val sets: Int, val reps: String, val rest: Int = 60, val weight: Float = 0f)
data class SeedRoutine(val name: String, val emoji: String, val exercises: List<SeedExercise>)

object DefaultRoutines {
    val list: List<SeedRoutine> = listOf(
        SeedRoutine(
            name = "Pierna",
            emoji = "🦵",
            exercises = listOf(
                SeedExercise("Sentadilla", 4, "8-10", rest = 120),
                SeedExercise("Prensa de pierna", 4, "10-12", rest = 90),
                SeedExercise("Peso muerto rumano", 4, "8", rest = 120),
                SeedExercise("Curl femoral", 3, "12", rest = 60),
                SeedExercise("Extensión de cuádriceps", 3, "15", rest = 60),
                SeedExercise("Gemelo de pie", 4, "20", rest = 45),
            ),
        ),
        SeedRoutine(
            name = "Pecho y tríceps",
            emoji = "💪",
            exercises = listOf(
                SeedExercise("Press banca", 4, "8", rest = 120),
                SeedExercise("Press inclinado", 3, "10", rest = 90),
                SeedExercise("Aperturas con mancuernas", 3, "12", rest = 60),
                SeedExercise("Fondos en paralelas", 3, "10", rest = 90),
                SeedExercise("Press francés", 3, "10", rest = 60),
                SeedExercise("Extensión polea tríceps", 3, "12", rest = 45),
            ),
        ),
        SeedRoutine(
            name = "Espalda y bíceps",
            emoji = "🏋",
            exercises = listOf(
                SeedExercise("Dominadas", 4, "8", rest = 120),
                SeedExercise("Remo con barra", 4, "10", rest = 90),
                SeedExercise("Jalón al pecho", 3, "12", rest = 60),
                SeedExercise("Remo en polea baja", 3, "10", rest = 60),
                SeedExercise("Curl bíceps con barra", 4, "10", rest = 60),
                SeedExercise("Curl martillo", 3, "12", rest = 45),
            ),
        ),
        SeedRoutine(
            name = "Hombros",
            emoji = "🤸",
            exercises = listOf(
                SeedExercise("Press militar", 4, "8", rest = 120),
                SeedExercise("Elevaciones laterales", 4, "12", rest = 45),
                SeedExercise("Pájaro deltoides posterior", 3, "12", rest = 45),
                SeedExercise("Press Arnold", 3, "10", rest = 90),
                SeedExercise("Encogimientos", 3, "15", rest = 45),
            ),
        ),
        SeedRoutine(
            name = "Cardio",
            emoji = "🏃",
            exercises = listOf(
                SeedExercise("Calentamiento caminata", 1, "5 min", rest = 0),
                SeedExercise("Cinta o bici intensa", 1, "20 min", rest = 0),
                SeedExercise("Cuerda", 3, "3 min", rest = 60),
                SeedExercise("Burpees", 3, "15", rest = 45),
                SeedExercise("Mountain climbers", 3, "30s", rest = 30),
                SeedExercise("Estiramiento", 1, "5 min", rest = 0),
            ),
        ),
        SeedRoutine(
            name = "Full body",
            emoji = "🔥",
            exercises = listOf(
                SeedExercise("Sentadilla goblet", 3, "10", rest = 90),
                SeedExercise("Press banca", 3, "10", rest = 90),
                SeedExercise("Remo con mancuerna", 3, "10", rest = 75),
                SeedExercise("Press hombro mancuernas", 3, "10", rest = 75),
                SeedExercise("Plancha", 3, "60s", rest = 45),
                SeedExercise("Curl + press", 3, "10", rest = 60),
            ),
        ),
    )

    suspend fun seedIfEmpty(context: Context) {
        val dao = Db.get(context).routineDao()
        if (dao.getAll().isNotEmpty()) return
        list.forEach { seed ->
            dao.saveRoutine(
                routine = Routine(name = seed.name, emoji = seed.emoji),
                exercises = seed.exercises.mapIndexed { idx, e ->
                    RoutineExerciseEntity(
                        routineId = 0L,
                        exerciseName = e.name,
                        sets = e.sets,
                        repsText = e.reps,
                        orderIndex = idx,
                        restSeconds = e.rest,
                        weightKg = e.weight,
                    )
                },
            )
        }
    }
}
