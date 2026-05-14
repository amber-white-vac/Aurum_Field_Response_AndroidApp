package com.aurum.fieldresponse.domain

import androidx.compose.ui.graphics.Color

data class FieldCoordinate(
    val latitude: Double,
    val longitude: Double,
)

data class TrackerReading(
    val deviceId: String,
    val workerName: String,
    val role: String,
    val coordinate: FieldCoordinate,
    val radiationDoseRateMrPerHr: Double,
    val batteryPercent: Int,
    val connected: Boolean,
    val timestampMillis: Long,
)

enum class Severity {
    Advisory,
    Elevated,
    Critical,
}

enum class ResponseWorkflow(
    val title: String,
    val shortLabel: String,
    val steps: List<String>,
) {
    DoseAlarm(
        title = "Dose Alarm Response",
        shortLabel = "Dose",
        steps = listOf(
            "Stop work and confirm the worker's last known location.",
            "Route the nearest RCT/HPT to verify instrument readings.",
            "Move the worker to the low-dose boundary if communication is confirmed.",
            "Start ALARA review and capture survey documentation.",
        ),
    ),
    BoundaryBreach(
        title = "Radiological Boundary Breach",
        shortLabel = "Boundary",
        steps = listOf(
            "Freeze area access and mark the affected boundary.",
            "Notify supervisor and radiation safety lead.",
            "Validate personnel accountability from tracker history.",
            "Open a corrective action record before resuming work.",
        ),
    ),
    LostSignal(
        title = "Lost Device Signal",
        shortLabel = "Signal",
        steps = listOf(
            "Attempt push-to-talk or supervisor contact.",
            "Check last telemetry packet and battery state.",
            "Dispatch buddy check from the closest safe route.",
            "Escalate if no contact within the site response window.",
        ),
    ),
}

data class IncidentFlag(
    val id: String,
    val deviceId: String,
    val title: String,
    val detail: String,
    val coordinate: FieldCoordinate,
    val severity: Severity,
    val workflow: ResponseWorkflow,
    val createdAtMillis: Long,
)

fun Severity.flagColor(): Color = when (this) {
    Severity.Advisory -> Color(0xFF2F80ED)
    Severity.Elevated -> Color(0xFFF2C94C)
    Severity.Critical -> Color(0xFFEB5757)
}
