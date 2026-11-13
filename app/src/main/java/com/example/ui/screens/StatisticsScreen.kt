package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionMetricPoint
import com.example.data.model.TrainingSession
import com.example.ui.components.HudPrimaryButton
import com.example.ui.components.HudSecondaryButton
import com.example.ui.components.HudStatBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalCard
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StatisticsUiState
import com.example.ui.viewmodel.StatisticsViewModel
import com.example.util.AnalyticsExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("statistics_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "PERFORMANCE ANALYTICS",
            subtitle = "Real telemetry calculated from locally stored sessions"
        )

        statusMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElevatedDarkGrey,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text("DISMISS", color = NeonOrange)
                    }
                }
            }
        }

        when (val state = uiState) {
            StatisticsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonOrange)
                }
            }

            StatisticsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("statistics_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    TacticalCard(
                        borderColor = TacticalBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = null,
                                tint = NeonOrange,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "NO TRAINING DATA RECORDED",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Complete a session in Practice Arena or Trainer to generate live accuracy, reaction speed, and performance ratio charts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            is StatisticsUiState.Content -> {
                val stats = state.statistics
                val totalMin = stats.totalTrainingDurationMs / 60_000L
                val totalSec = (stats.totalTrainingDurationMs % 60_000L) / 1000L

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action Row: CSV Export + Share Summary + Clear
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HudPrimaryButton(
                                text = "CSV EXPORT",
                                icon = Icons.Filled.FileDownload,
                                onClick = {
                                    AnalyticsExporter.shareCsv(context, state.sessions)
                                        .onSuccess { viewModel.setStatusMessage("CSV report generated: ${it.name}") }
                                        .onFailure { viewModel.setStatusMessage("CSV export failed: ${it.message}") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_csv_button")
                            )
                            HudSecondaryButton(
                                text = "SHARE",
                                icon = Icons.Filled.Share,
                                borderColor = NeonOrange,
                                contentColor = NeonOrange,
                                onClick = {
                                    AnalyticsExporter.shareSummary(context, stats)
                                        .onFailure { viewModel.setStatusMessage("Share failed: ${it.message}") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_summary_button")
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElevatedDarkGrey,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f))
                            ) {
                                IconButton(
                                    onClick = { showClearDialog = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("clear_history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteSweep,
                                        contentDescription = "Clear all training sessions",
                                        tint = CrimsonRed
                                    )
                                }
                            }
                        }
                    }

                    // KPI Summary Grid
                    item {
                        TacticalCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HudStatBadge("SESSIONS", "${stats.totalSessions}", NeonOrange, Modifier.weight(1f))
                                HudStatBadge("BEST SCORE", "${stats.bestScore}", TacticalGreen, Modifier.weight(1f))
                                HudStatBadge("AVG SCORE", String.format(Locale.US, "%.0f", stats.averageScore), TextPrimary, Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HudStatBadge(
                                    "ACCURACY",
                                    String.format(Locale.US, "%.1f%%", stats.overallAccuracyPercent),
                                    NeonOrange,
                                    Modifier.weight(1f)
                                )
                                HudStatBadge(
                                    "CENTER ZONE",
                                    String.format(Locale.US, "%.1f%%", stats.centerZoneAccuracyPercent),
                                    TacticalGreen,
                                    Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HudStatBadge(
                                    "AVG REACTION",
                                    if (stats.averageReactionTimeMs > 0f) "${stats.averageReactionTimeMs.toInt()} ms" else "--",
                                    NeonOrange,
                                    Modifier.weight(1f)
                                )
                                HudStatBadge(
                                    "PERF RATIO",
                                    String.format(Locale.US, "%.2fx", stats.simulatedPerformanceRatio),
                                    TextPrimary,
                                    Modifier.weight(1f)
                                )
                                HudStatBadge(
                                    "DURATION",
                                    "${totalMin}m ${totalSec}s",
                                    TextSecondary,
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Accuracy History Chart
                    item {
                        MetricTrendChartCard(
                            title = "ACCURACY HISTORY (%)",
                            points = stats.accuracyHistory,
                            lineColor = NeonOrange,
                            unitSuffix = "%",
                            maxYOverride = 100f
                        )
                    }

                    // Reaction-Time History Chart
                    val reactionPoints = stats.reactionTimeHistory.filter { it.value > 0f }
                    if (reactionPoints.isNotEmpty()) {
                        item {
                            MetricTrendChartCard(
                                title = "REACTION-TIME HISTORY (ms)",
                                points = reactionPoints,
                                lineColor = CrimsonRed,
                                unitSuffix = " ms",
                                maxYOverride = null
                            )
                        }
                    }

                    // Session History Header
                    item {
                        Text(
                            text = "SESSION HISTORY (${state.sessions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonOrange,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(state.sessions, key = { it.id }) { session ->
                        SessionHistoryRow(
                            session = session,
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = ObsidianPanel,
            title = { Text("Clear Training History?", color = TextPrimary) },
            text = {
                Text(
                    "All recorded practice sessions and analytics charts will be permanently deleted from local storage.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("CLEAR ALL", color = CrimsonRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun MetricTrendChartCard(
    title: String,
    points: List<SessionMetricPoint>,
    lineColor: Color,
    unitSuffix: String,
    maxYOverride: Float?
) {
    TacticalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            val latest = points.lastOrNull()?.value ?: 0f
            Text(
                text = "LATEST: ${String.format(Locale.US, "%.1f", latest)}$unitSuffix",
                style = MaterialTheme.typography.labelMedium,
                color = lineColor
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(MatteBlack, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            val maxY = maxYOverride ?: (points.maxOfOrNull { it.value }?.coerceAtLeast(100f) ?: 100f)
            val minY = 0f
            val rangeY = (maxY - minY).coerceAtLeast(1f)

            // Reference horizontal grid lines
            listOf(0.25f, 0.50f, 0.75f).forEach { frac ->
                val y = size.height * frac
                drawLine(
                    color = TacticalBorder.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            if (points.size == 1) {
                val y = size.height * (1f - ((points[0].value - minY) / rangeY).coerceIn(0f, 1f))
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(size.width / 2f, y))
            } else if (points.size >= 2) {
                val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                val linePath = Path()
                val fillPath = Path()

                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val y = size.height * (1f - ((pt.value - minY) / rangeY).coerceIn(0f, 1f))
                    if (idx == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo((points.size - 1) * stepX, size.height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { idx, pt ->
                    val x = idx * stepX
                    val y = size.height * (1f - ((pt.value - minY) / rangeY).coerceIn(0f, 1f))
                    drawCircle(color = MatteBlack, radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = lineColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryRow(
    session: TrainingSession,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    TacticalCard(contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.mode.replace('_', ' '),
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = "• ${session.subMode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "${dateFormat.format(Date(session.timestampMs))}  |  Profile: ${session.profileName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Score: ${session.score}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Acc: ${String.format(Locale.US, "%.1f%%", session.accuracyPercent)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TacticalGreen
                    )
                    if (session.avgReactionTimeMs > 0f) {
                        Text(
                            text = "${session.avgReactionTimeMs.toInt()}ms",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonOrange
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete session",
                    tint = TextMuted
                )
            }
        }
    }
}
