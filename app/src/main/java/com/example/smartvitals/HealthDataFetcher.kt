package com.example.smartvitals

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

object HealthDataFetcher {

    interface FetchCallback {
        fun onSuccess(steps: Long, avgHeartRate: Long)
        fun onError(error: String)
    }

    fun fetchRealData(client: HealthConnectClient, callback: FetchCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Instant.now()
                val past24Hours = now.minus(24, ChronoUnit.HOURS)
                val timeRange = TimeRangeFilter.between(past24Hours, now)

                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
                val stepsResponse = client.readRecords(stepsRequest)
                val totalSteps = stepsResponse.records.sumOf { it.count }

                val hrRequest = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeRange
                )
                val hrResponse = client.readRecords(hrRequest)
                var totalBeats = 0L
                var count = 0
                hrResponse.records.forEach { record ->
                    record.samples.forEach { sample ->
                        totalBeats += sample.beatsPerMinute
                        count++
                    }
                }
                val avgHr = if (count > 0) totalBeats / count else 0L

                withContext(Dispatchers.Main) {
                    callback.onSuccess(totalSteps, avgHr)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "Unknown error")
                }
            }
        }
    }
}