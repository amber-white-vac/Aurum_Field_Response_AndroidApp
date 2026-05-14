package com.aurum.fieldresponse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HazardClassifierTest {
    private val classifier = HazardClassifier()

    @Test
    fun criticalDoseRoutesToDoseAlarm() {
        val flag = classifier.classify(reading(doseRate = 120.0))

        assertNotNull(flag)
        assertEquals(Severity.Critical, flag?.severity)
        assertEquals(ResponseWorkflow.DoseAlarm, flag?.workflow)
    }

    @Test
    fun lostSignalRoutesToSignalWorkflow() {
        val flag = classifier.classify(reading(connected = false))

        assertNotNull(flag)
        assertEquals(Severity.Elevated, flag?.severity)
        assertEquals(ResponseWorkflow.LostSignal, flag?.workflow)
    }

    @Test
    fun normalReadingDoesNotCreateFlag() {
        val flag = classifier.classify(
            reading(
                coordinate = FieldCoordinate(43.49570, -112.04520),
                doseRate = 14.0,
            ),
        )

        assertNull(flag)
    }

    private fun reading(
        coordinate: FieldCoordinate = FieldCoordinate(43.49570, -112.04520),
        doseRate: Double = 12.0,
        connected: Boolean = true,
    ): TrackerReading = TrackerReading(
        deviceId = "HELIOS-TEST",
        workerName = "Test Worker",
        role = "RCT",
        coordinate = coordinate,
        radiationDoseRateMrPerHr = doseRate,
        batteryPercent = 90,
        connected = connected,
        timestampMillis = 1_000L,
    )
}
