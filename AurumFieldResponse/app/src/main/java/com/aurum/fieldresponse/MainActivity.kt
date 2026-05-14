package com.aurum.fieldresponse

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurum.fieldresponse.domain.FieldCoordinate
import com.aurum.fieldresponse.domain.IncidentFlag
import com.aurum.fieldresponse.domain.ResponseWorkflow
import com.aurum.fieldresponse.domain.Severity
import com.aurum.fieldresponse.domain.TrackerReading
import com.aurum.fieldresponse.domain.flagColor
import com.aurum.fieldresponse.ui.FieldResponseUiState
import com.aurum.fieldresponse.ui.FieldResponseViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: FieldResponseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestNotificationPermission()

        setContent {
            AurumTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.latestAlert?.id) {
                    val alert = uiState.latestAlert ?: return@LaunchedEffect
                    showIncidentNotification(alert)
                    viewModel.clearAlert()
                }

                FieldResponseScreen(
                    uiState = uiState,
                    onFlagSelected = viewModel::selectFlag,
                    onWorkflowSelected = viewModel::selectWorkflow,
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INCIDENT_CHANNEL_ID,
                "Incident flags",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Map-based incident flags from field telemetry"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showIncidentNotification(flag: IncidentFlag) {
        val notification = NotificationCompat.Builder(this, INCIDENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(flag.title)
            .setContentText(flag.detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(flag.id.hashCode(), notification)
        }
    }

    private companion object {
        const val INCIDENT_CHANNEL_ID = "field_incident_flags"
    }
}

@Composable
private fun AurumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF1F6F5B),
            secondary = Color(0xFFC99A2E),
            tertiary = Color(0xFF245B8F),
            surface = Color(0xFFF7F8F4),
            background = Color(0xFFE9EEE8),
            onPrimary = Color.White,
            onSurface = Color(0xFF17201C),
        ),
        typography = MaterialTheme.typography,
        content = content,
    )
}

@Composable
private fun FieldResponseScreen(
    uiState: FieldResponseUiState,
    onFlagSelected: (IncidentFlag) -> Unit,
    onWorkflowSelected: (ResponseWorkflow) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE9EEE8)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                OperationalMap(
                    readings = uiState.readings,
                    flags = uiState.flags,
                    selectedFlag = uiState.selectedFlag,
                    onFlagSelected = onFlagSelected,
                )
                TopStatusBar(
                    activeCount = uiState.readings.count { it.connected },
                    flagCount = uiState.flags.size,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            ResponseSidebar(
                flags = uiState.flags,
                selectedFlag = uiState.selectedFlag,
                activeWorkflow = uiState.activeWorkflow,
                onFlagSelected = onFlagSelected,
                onWorkflowSelected = onWorkflowSelected,
            )
        }
    }
}

@Composable
private fun OperationalMap(
    readings: List<TrackerReading>,
    flags: List<IncidentFlag>,
    selectedFlag: IncidentFlag?,
    onFlagSelected: (IncidentFlag) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1512))) {
        val density = LocalDensity.current
        val mapWidthPx = with(density) { maxWidth.toPx() }
        val mapHeightPx = with(density) { maxHeight.toPx() }
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF22352F), Color(0xFF10201B), Color(0xFF0E1512)),
                    ),
                )

                val gridColor = Color.White.copy(alpha = 0.08f)
                val step = size.minDimension / 8f
                var x = 0f
                while (x <= size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                drawFacilityFootprint(size)
                drawRestrictedBoundary(size)

                readings.forEach { reading ->
                    val point = reading.coordinate.project(size)
                    val color = if (reading.connected) Color(0xFF73D99F) else Color(0xFF8E9A94)
                    drawCircle(color.copy(alpha = 0.22f), radius = 22f, center = point)
                    drawCircle(color, radius = 8f, center = point)
                    drawLine(
                        color = color.copy(alpha = 0.65f),
                        start = point,
                        end = point + Offset(0f, -26f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            readings.forEach { reading ->
                val point = reading.coordinate.projectForOverlay(mapWidthPx, mapHeightPx)
                WorkerBadge(
                    reading = reading,
                    modifier = Modifier.offset { IntOffset(point.x.roundToInt() - 42, point.y.roundToInt() + 12) },
                )
            }

            flags.forEach { flag ->
                val point = flag.coordinate.projectForOverlay(mapWidthPx, mapHeightPx)
                FlagMarker(
                    flag = flag,
                    selected = flag.id == selectedFlag?.id,
                    onClick = { onFlagSelected(flag) },
                    modifier = Modifier.offset { IntOffset(point.x.roundToInt() - 18, point.y.roundToInt() - 42) },
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFacilityFootprint(size: Size) {
    val path = Path().apply {
        moveTo(size.width * 0.16f, size.height * 0.70f)
        lineTo(size.width * 0.38f, size.height * 0.20f)
        lineTo(size.width * 0.78f, size.height * 0.26f)
        lineTo(size.width * 0.86f, size.height * 0.74f)
        lineTo(size.width * 0.46f, size.height * 0.86f)
        close()
    }
    drawPath(path, color = Color(0xFF2C4B42).copy(alpha = 0.55f))
    drawPath(path, color = Color(0xFF9DB3AA).copy(alpha = 0.42f), style = Stroke(width = 3f))
    drawLine(
        color = Color(0xFFC99A2E).copy(alpha = 0.65f),
        start = Offset(size.width * 0.22f, size.height * 0.66f),
        end = Offset(size.width * 0.78f, size.height * 0.32f),
        strokeWidth = 6f,
        cap = StrokeCap.Round,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRestrictedBoundary(size: Size) {
    drawRect(
        color = Color(0xFFC99A2E).copy(alpha = 0.14f),
        topLeft = Offset(size.width * 0.44f, size.height * 0.35f),
        size = Size(size.width * 0.24f, size.height * 0.24f),
    )
    drawRect(
        color = Color(0xFFC99A2E).copy(alpha = 0.72f),
        topLeft = Offset(size.width * 0.44f, size.height * 0.35f),
        size = Size(size.width * 0.24f, size.height * 0.24f),
        style = Stroke(width = 4f),
    )
}

@Composable
private fun WorkerBadge(reading: TrackerReading, modifier: Modifier = Modifier) {
    val background = if (reading.connected) Color(0xDD17201C) else Color(0xDD343B38)
    Column(
        modifier = modifier
            .width(92.dp)
            .background(background, RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        Text(
            text = reading.workerName,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${reading.radiationDoseRateMrPerHr.roundToInt()} mR/hr",
            color = Color(0xFFDDEBE4),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun FlagMarker(
    flag: IncidentFlag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = if (selected) 42.dp else 36.dp
    Box(
        modifier = modifier
            .size(size)
            .background(flag.severity.flagColor(), CircleShape)
            .border(3.dp, Color.White.copy(alpha = if (selected) 0.95f else 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

@Composable
private fun TopStatusBar(activeCount: Int, flagCount: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .padding(14.dp)
            .fillMaxWidth()
            .background(Color(0xEE17201C), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (maxWidth < 260.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Aurum Field Response",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Live location map",
                    color = Color(0xFFB8C8C0),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("$activeCount active", Color(0xFF73D99F))
                    StatusPill("$flagCount flags", Color(0xFFE7D09A))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Aurum Field Response", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Live location map", color = Color(0xFFB8C8C0), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$activeCount active", color = Color(0xFF73D99F), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("$flagCount flags", color = Color(0xFFE7D09A), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun ResponseSidebar(
    flags: List<IncidentFlag>,
    selectedFlag: IncidentFlag?,
    activeWorkflow: ResponseWorkflow,
    onFlagSelected: (IncidentFlag) -> Unit,
    onWorkflowSelected: (ResponseWorkflow) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(184.dp)
            .fillMaxHeight()
            .background(Color(0xFFF7F8F4))
            .padding(10.dp),
    ) {
        Text("Response", color = Color(0xFF17201C), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Tap a map flag to route the workflow.", color = Color(0xFF52635C), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        ResponseWorkflow.entries.forEach { workflow ->
            val selected = workflow == activeWorkflow
            TextButton(
                onClick = { onWorkflowSelected(workflow) },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(7.dp),
            ) {
                Text(
                    text = workflow.shortLabel,
                    color = if (selected) Color(0xFF1F6F5B) else Color(0xFF31413A),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Flags", color = Color(0xFF17201C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(flags, key = { it.id }) { flag ->
                FlagListItem(
                    flag = flag,
                    selected = selectedFlag?.id == flag.id,
                    onClick = { onFlagSelected(flag) },
                )
            }
        }

        WorkflowPanel(
            workflow = activeWorkflow,
            accentColor = selectedFlag
                ?.takeIf { it.workflow == activeWorkflow }
                ?.severity
                ?.flagColor()
                ?: activeWorkflow.defaultAccentColor(),
        )
    }
}

@Composable
private fun FlagListItem(flag: IncidentFlag, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE8F2ED) else Color.White,
        ),
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(flag.severity.flagColor(), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    flag.title,
                    color = Color(0xFF17201C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(flag.workflow.title, color = Color(0xFF52635C), fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun WorkflowPanel(workflow: ResponseWorkflow, accentColor: Color) {
    var isDetailOpen by remember { mutableStateOf(false) }

    if (isDetailOpen) {
        WorkflowDetailDialog(
            workflow = workflow,
            accentColor = accentColor,
            onDismiss = { isDetailOpen = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, accentColor, RoundedCornerShape(8.dp))
            .background(Color(0xFF17201C), RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(accentColor, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(workflow.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        workflow.steps.forEachIndexed { index, step ->
            Text("${index + 1}. $step", color = Color(0xFFDDEBE4), fontSize = 11.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(5.dp))
        }
        Button(
            onClick = { isDetailOpen = true },
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(7.dp),
        ) {
            Text("Open", fontSize = 12.sp)
        }
    }
}

@Composable
private fun WorkflowDetailDialog(
    workflow: ResponseWorkflow,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(accentColor, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = workflow.title,
                    color = Color(0xFF17201C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                workflow.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        color = Color(0xFF30443B),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun ResponseWorkflow.defaultAccentColor(): Color = when (this) {
    ResponseWorkflow.DoseAlarm -> Severity.Critical.flagColor()
    ResponseWorkflow.BoundaryBreach -> Severity.Elevated.flagColor()
    ResponseWorkflow.LostSignal -> Severity.Advisory.flagColor()
}

private fun FieldCoordinate.project(size: Size): Offset {
    val x = ((longitude - SITE_WEST) / (SITE_EAST - SITE_WEST)).toFloat() * size.width
    val y = (1f - ((latitude - SITE_SOUTH) / (SITE_NORTH - SITE_SOUTH)).toFloat()) * size.height
    return Offset(x.coerceIn(24f, size.width - 24f), y.coerceIn(84f, size.height - 64f))
}

private fun FieldCoordinate.projectForOverlay(widthPx: Float, heightPx: Float): Offset {
    val x = ((longitude - SITE_WEST) / (SITE_EAST - SITE_WEST)).toFloat() * widthPx
    val y = (1f - ((latitude - SITE_SOUTH) / (SITE_NORTH - SITE_SOUTH)).toFloat()) * heightPx
    return Offset(x.coerceIn(24f, widthPx - 24f), y.coerceIn(84f, heightPx - 64f))
}

private const val SITE_NORTH = 43.49585
private const val SITE_SOUTH = 43.49412
private const val SITE_WEST = -112.04535
private const val SITE_EAST = -112.04342
