package com.aurum.fieldresponse.data

import com.aurum.fieldresponse.domain.HazardClassifier
import com.aurum.fieldresponse.domain.IncidentFlag
import com.aurum.fieldresponse.domain.TrackerReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FieldSnapshot(
    val readings: List<TrackerReading>,
    val flags: List<IncidentFlag>,
)

class FieldResponseRepository(
    private val trackerSource: TrackerSource,
    private val hazardClassifier: HazardClassifier,
) {
    fun snapshots(): Flow<FieldSnapshot> {
        val activeFlags = linkedMapOf<String, IncidentFlag>()
        return trackerSource.readings().map { readings ->
            readings.mapNotNull(hazardClassifier::classify).forEach { flag ->
                activeFlags.putIfAbsent(flag.stableKey(), flag)
            }
            FieldSnapshot(
                readings = readings,
                flags = activeFlags.values.sortedByDescending { it.createdAtMillis },
            )
        }
    }

    private fun IncidentFlag.stableKey(): String = "$deviceId-${workflow.name}"
}
