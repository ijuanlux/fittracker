package com.juan.fittracker.data

import android.content.Context
import android.content.Intent

object ShareHelper {
    private const val APP_URL = "https://github.com/ijuanlux/fittracker/releases/latest"

    fun shareApp(context: Context) {
        val text = """
            🍪 Galleta FitTracker — La app fitness rola con galletoide colombiana que te acompaña.

            - Rutinas guiadas, registro de comidas, logros y stats con quesitos
            - Frases bogotanas (Bachué, Quirigua, Pan Yiyo, Monserrate...)
            - 100% gratis y sin cuenta

            Bájala acá: $APP_URL
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Galleta FitTracker")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, "Compartir Galleta FitTracker")
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
        }
    }

    fun shareWorkout(context: Context, workout: WorkoutWithSets) {
        val date = formatShortDate(workout.workout.dateEpochMs)
        val rutina = workout.workout.notes.takeIf { it.isNotBlank() } ?: "Entreno libre"
        val volume = workout.totalVolumeKg.toInt()
        val exercises = workout.exerciseNames.joinToString(" · ")
        val text = """
            🏋 Galleta FitTracker — $rutina

            📅 $date
            💪 ${workout.exerciseCount} ejercicios · ${workout.totalSets} series · ${volume} kg de volumen

            $exercises

            Galletoide rola lo confirma. 🍪
            $APP_URL
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mi entreno de Galleta FitTracker")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, "Compartir entreno")
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
        }
    }
}
