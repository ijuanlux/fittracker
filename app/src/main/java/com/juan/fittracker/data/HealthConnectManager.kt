package com.juan.fittracker.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayHealthStats(
    val steps: Long,
    val activeKcal: Double,
)

data class DailyMetric(
    val date: LocalDate,
    val steps: Long,
    val activeKcal: Double,
)

data class HeartRateStats(
    val latestBpm: Long?,
    val dayAvg: Long?,
    val dayMin: Long?,
    val dayMax: Long?,
) {
    val hasData: Boolean get() = latestBpm != null || dayAvg != null
}

data class SleepStats(
    val totalMinutes: Long,
    val sessionsCount: Int,
) {
    val hours: Float get() = totalMinutes / 60f
    val display: String get() = "${totalMinutes / 60}h ${totalMinutes % 60}m"
    val hasData: Boolean get() = totalMinutes > 0
}

object HealthConnectManager {
    val Permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    fun sdkStatus(context: Context): Int = HealthConnectClient.getSdkStatus(context)

    fun isAvailable(context: Context): Boolean =
        sdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun clientOrNull(context: Context): HealthConnectClient? =
        if (isAvailable(context)) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(client: HealthConnectClient): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(Permissions)
    }

    suspend fun readTodayStats(client: HealthConnectClient): TodayHealthStats {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val resp = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return TodayHealthStats(
            steps = resp[StepsRecord.COUNT_TOTAL] ?: 0L,
            activeKcal = resp[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
        )
    }

    suspend fun readLastNDays(client: HealthConnectClient, days: Int): List<DailyMetric> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val result = mutableListOf<DailyMetric>()
        for (offset in (days - 1) downTo 0) {
            val date = today.minusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (offset == 0) Instant.now()
            else date.plusDays(1).atStartOfDay(zone).toInstant()
            val resp = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            result += DailyMetric(
                date = date,
                steps = resp[StepsRecord.COUNT_TOTAL] ?: 0L,
                activeKcal = resp[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
            )
        }
        return result
    }

    suspend fun readHeartRate(client: HealthConnectClient): HeartRateStats {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val agg = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    HeartRateRecord.BPM_AVG,
                    HeartRateRecord.BPM_MIN,
                    HeartRateRecord.BPM_MAX,
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        val latest = records.records.firstOrNull()?.samples?.maxByOrNull { it.time }?.beatsPerMinute
        return HeartRateStats(
            latestBpm = latest,
            dayAvg = agg[HeartRateRecord.BPM_AVG],
            dayMin = agg[HeartRateRecord.BPM_MIN],
            dayMax = agg[HeartRateRecord.BPM_MAX],
        )
    }

    suspend fun readLastNightSleep(client: HealthConnectClient): SleepStats {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
        val end = today.atTime(18, 0).atZone(zone).toInstant().let {
            if (it.isAfter(Instant.now())) Instant.now() else it
        }
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records
        val total = records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        return SleepStats(totalMinutes = total, sessionsCount = records.size)
    }
}
