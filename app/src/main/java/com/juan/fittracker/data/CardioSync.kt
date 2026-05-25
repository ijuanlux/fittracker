package com.juan.fittracker.data

import android.content.Context
import androidx.health.connect.client.records.ExerciseSessionRecord

object CardioSync {
    private fun mapHcType(exerciseType: Int): CardioType? = when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> CardioType.Correr
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> CardioType.Caminar
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> CardioType.Bici
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> CardioType.Eliptica
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> CardioType.Natacion
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> CardioType.Baile
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER,
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> CardioType.Futbol
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> CardioType.Hiit
        else -> null
    }

    suspend fun syncRecent(context: Context, days: Int = 14, defaultWeightKg: Float): Int {
        val client = HealthConnectManager.clientOrNull(context) ?: return 0
        if (!HealthConnectManager.hasAllPermissions(client)) return 0
        val sessions = HealthConnectManager.readExerciseSessions(client, days)
        val dao = Db.get(context).cardioDao()
        var imported = 0
        sessions.forEach { hc ->
            val type = mapHcType(hc.exerciseType) ?: return@forEach
            if (hc.durationMin <= 0) return@forEach
            if (dao.findByExternalId(hc.externalId) != null) return@forEach
            val intensity = CardioIntensity.Media
            val kcal = CardioKcal.estimate(type, intensity, hc.durationMin, defaultWeightKg)
            dao.insert(
                CardioSession(
                    dateEpochMs = hc.startEpochMs,
                    typeKey = type.name,
                    durationMin = hc.durationMin,
                    distanceKm = null,
                    intensityKey = intensity.name,
                    kcal = kcal,
                    sourceKey = CardioSource.HealthConnect.name,
                    externalId = hc.externalId,
                    notes = hc.title,
                ),
            )
            imported++
        }
        return imported
    }
}
