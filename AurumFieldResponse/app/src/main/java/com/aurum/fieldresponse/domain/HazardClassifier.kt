package com.aurum.fieldresponse.domain

class HazardClassifier {
    fun classify(reading: TrackerReading): IncidentFlag? {
        val createdAt = reading.timestampMillis
        return when {
            !reading.connected -> IncidentFlag(
                id = "${reading.deviceId}-signal-$createdAt",
                deviceId = reading.deviceId,
                title = "Lost signal: ${reading.workerName}",
                detail = "${reading.role} tracker stopped reporting. Last battery ${reading.batteryPercent}%.",
                coordinate = reading.coordinate,
                severity = Severity.Elevated,
                workflow = ResponseWorkflow.LostSignal,
                createdAtMillis = createdAt,
            )

            reading.radiationDoseRateMrPerHr >= CRITICAL_DOSE_RATE -> IncidentFlag(
                id = "${reading.deviceId}-dose-$createdAt",
                deviceId = reading.deviceId,
                title = "Critical dose rate",
                detail = "${reading.workerName} is reading ${reading.radiationDoseRateMrPerHr.formatDose()} mR/hr.",
                coordinate = reading.coordinate,
                severity = Severity.Critical,
                workflow = ResponseWorkflow.DoseAlarm,
                createdAtMillis = createdAt,
            )

            isInsideRestrictedBoundary(reading.coordinate) -> IncidentFlag(
                id = "${reading.deviceId}-boundary-$createdAt",
                deviceId = reading.deviceId,
                title = "Boundary breach",
                detail = "${reading.workerName} entered a controlled radiological boundary.",
                coordinate = reading.coordinate,
                severity = Severity.Elevated,
                workflow = ResponseWorkflow.BoundaryBreach,
                createdAtMillis = createdAt,
            )

            reading.radiationDoseRateMrPerHr >= ELEVATED_DOSE_RATE -> IncidentFlag(
                id = "${reading.deviceId}-dose-watch-$createdAt",
                deviceId = reading.deviceId,
                title = "Dose rate trending up",
                detail = "${reading.workerName} is above the facility watch setpoint.",
                coordinate = reading.coordinate,
                severity = Severity.Advisory,
                workflow = ResponseWorkflow.DoseAlarm,
                createdAtMillis = createdAt,
            )

            else -> null
        }
    }

    private fun isInsideRestrictedBoundary(coordinate: FieldCoordinate): Boolean {
        val latitudeInRange = coordinate.latitude in 43.49478..43.49518
        val longitudeInRange = coordinate.longitude in -112.04465..-112.04398
        return latitudeInRange && longitudeInRange
    }

    private fun Double.formatDose(): String = "%,.1f".format(this)

    private companion object {
        const val ELEVATED_DOSE_RATE = 45.0
        const val CRITICAL_DOSE_RATE = 95.0
    }
}
