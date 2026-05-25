package com.juan.fittracker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.random.Random

class WorkoutReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = "Galletoide te espera 🍪"
        val body = ReminderMessages.body(Random.nextInt())
        Notifier.showReminder(applicationContext, title, body)
        return Result.success()
    }
}

object ReminderMessages {
    private val pool = listOf(
        "Mijitica, ya es hora del entreno. No me joda.",
        "Galletoide pide pista: dele al gym, marica.",
        "Ya casi llega… el entreno, no el metro. Dele.",
        "Pa' ayer es tarde, hermano. A moverse.",
        "Una caminadita al Bachué y volvemos a la vida.",
        "Hoy no se me raje, sea berraco.",
        "Vamos pa' el gym o pa' septimazo, usted dirá.",
        "Una sesionita corta y cerramos el día.",
        "Sin afanes pero sin pereza, dele.",
        "La pereza no quema calorías, marica.",
        "Aproveche y métale al cardio, no se haga.",
        "El reloj corre. Las pesas también.",
    )

    fun body(seed: Int): String {
        val idx = ((seed % pool.size) + pool.size) % pool.size
        return pool[idx]
    }
}
