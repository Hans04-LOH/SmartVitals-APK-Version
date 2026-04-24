package com.example.smartvitals

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit

data class HealthSummary(
    val totalSteps: Long,
    val avgHeartRate: Long,
    val maxHeartRate: Long,
    val hasHeartData: Boolean
)

object HealthConnectBridge {

    @JvmStatic
    fun buildReadPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }

    @JvmStatic
    fun hasAllPermissionsBlocking(
        context: Context,
        permissions: Set<String>
    ): Boolean = runBlocking {
        hasAllPermissions(context, permissions)
    }

    @JvmStatic
    fun readLast24HoursBlocking(
        context: Context
    ): HealthSummary = runBlocking {
        readLast24Hours(context)
    }

    @JvmStatic
    suspend fun hasAllPermissions(
        context: Context,
        permissions: Set<String>
    ): Boolean {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    @JvmStatic
    suspend fun readLast24Hours(context: Context): HealthSummary {
        val client = HealthConnectClient.getOrCreate(context)

        val endTime = Instant.now()
        val startTime = endTime.minus(1, ChronoUnit.DAYS)

        val stepResult = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        val totalSteps = stepResult[StepsRecord.COUNT_TOTAL] ?: 0L

        val heartResult = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    HeartRateRecord.BPM_AVG,
                    HeartRateRecord.BPM_MAX
                ),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        val avgHeartRate = (heartResult[HeartRateRecord.BPM_AVG] as? Number)?.toLong() ?: 0L
        val maxHeartRate = (heartResult[HeartRateRecord.BPM_MAX] as? Number)?.toLong() ?: avgHeartRate

        val heartRecords = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        val hasHeartData = heartRecords.records.isNotEmpty() || avgHeartRate > 0L

        return HealthSummary(
            totalSteps = totalSteps,
            avgHeartRate = avgHeartRate,
            maxHeartRate = maxHeartRate,
            hasHeartData = hasHeartData
        )
    }
}