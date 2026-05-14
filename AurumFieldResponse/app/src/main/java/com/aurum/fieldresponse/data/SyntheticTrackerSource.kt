package com.aurum.fieldresponse.data

import com.aurum.fieldresponse.domain.FieldCoordinate
import com.aurum.fieldresponse.domain.TrackerReading
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.cos
import kotlin.math.sin

interface TrackerSource {
    fun readings(): Flow<List<TrackerReading>>
}

class SyntheticTrackerSource : TrackerSource {
    override fun readings(): Flow<List<TrackerReading>> = flow {
        var tick = 0
        while (true) {
            emit(buildReadings(tick))
            tick += 1
            delay(1_200)
        }
    }

    private fun buildReadings(tick: Int): List<TrackerReading> {
        val now = System.currentTimeMillis()
        val doseSpike = tick in 8..13
        val signalDrop = tick in 16..19
        return listOf(
            TrackerReading(
                deviceId = "HELIOS-014",
                workerName = "Mara Jensen",
                role = "RCT",
                coordinate = orbit(
                    center = FieldCoordinate(43.49495, -112.04428),
                    tick = tick,
                    radiusLat = 0.00028,
                    radiusLon = 0.00042,
                    phase = 0.2,
                ),
                radiationDoseRateMrPerHr = if (doseSpike) 118.0 else 18.0 + (tick % 4) * 3.5,
                batteryPercent = 86 - (tick % 12),
                connected = true,
                timestampMillis = now,
            ),
            TrackerReading(
                deviceId = "HELIOS-022",
                workerName = "Cal Ortiz",
                role = "Supervisor",
                coordinate = orbit(
                    center = FieldCoordinate(43.49455, -112.04492),
                    tick = tick,
                    radiusLat = 0.00018,
                    radiusLon = 0.00035,
                    phase = 1.8,
                ),
                radiationDoseRateMrPerHr = 12.0 + (tick % 3) * 2.0,
                batteryPercent = 73 - (tick % 8),
                connected = !signalDrop,
                timestampMillis = now,
            ),
            TrackerReading(
                deviceId = "HELIOS-031",
                workerName = "Nika Rowe",
                role = "Field Tech",
                coordinate = orbit(
                    center = FieldCoordinate(43.49534, -112.04396),
                    tick = tick,
                    radiusLat = 0.00020,
                    radiusLon = 0.00030,
                    phase = 3.2,
                ),
                radiationDoseRateMrPerHr = if (tick in 22..26) 52.0 else 22.0,
                batteryPercent = 91 - (tick % 10),
                connected = true,
                timestampMillis = now,
            ),
        )
    }

    private fun orbit(
        center: FieldCoordinate,
        tick: Int,
        radiusLat: Double,
        radiusLon: Double,
        phase: Double,
    ): FieldCoordinate {
        val time = tick / 3.0 + phase
        return FieldCoordinate(
            latitude = center.latitude + sin(time) * radiusLat,
            longitude = center.longitude + cos(time) * radiusLon,
        )
    }
}
