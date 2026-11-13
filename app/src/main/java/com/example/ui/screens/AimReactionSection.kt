package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HudPrimaryButton
import com.example.ui.components.HudStatBadge
import com.example.ui.components.TacticalCard
import com.example.ui.components.drawBattlefieldGrid
import com.example.ui.components.drawGeometricTarget
import com.example.ui.components.drawTacticalCrosshair
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AimDifficulty
import com.example.ui.viewmodel.AimReactionViewModel
import com.example.ui.viewmodel.PracticeFeedbackEvent
import com.example.ui.viewmodel.SessionPhase
import com.example.util.FeedbackEngine
import java.util.Locale

@Composable
fun AimReactionSection(
    viewModel: AimReactionViewModel,
    feedbackEngine: FeedbackEngine
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val h = state.preferences.hapticFeedbackEnabled
            val s = state.preferences.soundEffectsEnabled
            when (event) {
                is PracticeFeedbackEvent.Hit -> feedbackEngine.onHit(view, h, s, event.centerZone)
                PracticeFeedbackEvent.Miss -> feedbackEngine.onMiss(view, h, s)
                PracticeFeedbackEvent.SessionCompleted -> feedbackEngine.onSessionComplete(view, h, s)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Difficulty Selector: EASY / MEDIUM / HARD / PRO
        TacticalCard {
            Text(
                text = "AIM & REACTION DIFFICULTY",
                style = MaterialTheme.typography.labelLarge,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AimDifficulty.entries.forEach { diff ->
                    val selected = state.difficulty == diff
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectDifficulty(diff) },
                        label = { Text(diff.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (diff == AimDifficulty.PRO) CrimsonRed else NeonOrange,
                            selectedLabelColor = if (diff == AimDifficulty.PRO) TextPrimary else MatteBlack,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("difficulty_chip_${diff.label.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudStatBadge(
                    label = "TIME",
                    value = "${state.remainingSec}s",
                    accentColor = if (state.remainingSec <= 8 && state.phase == SessionPhase.RUNNING) CrimsonRed else NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                HudStatBadge(
                    label = "SCORE",
                    value = "${state.totalScore}",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                HudPrimaryButton(
                    text = if (state.phase == SessionPhase.RUNNING) "ACTIVE" else "START",
                    icon = Icons.Filled.PlayArrow,
                    enabled = state.phase != SessionPhase.RUNNING,
                    onClick = { viewModel.startChallenge() },
                    modifier = Modifier.testTag("aim_start_button")
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedDarkGrey,
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    IconButton(
                        onClick = { viewModel.resetChallenge() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset aim trainer",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }

        // Interactive Aim & Flick Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(14.dp),
            color = MatteBlack,
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val isFlashing = System.currentTimeMillis() < state.hitFlashUntilMs

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("aim_trainer_canvas")
                        .pointerInput(widthPx, heightPx) {
                            detectTapGestures { offset ->
                                viewModel.onTapTargetFire(
                                    tapXNorm = offset.x / widthPx,
                                    tapYNorm = offset.y / heightPx
                                )
                            }
                        }
                        .pointerInput(widthPx, heightPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.onAimDrag(
                                    deltaXNorm = dragAmount.x / widthPx,
                                    deltaYNorm = dragAmount.y / heightPx
                                )
                            }
                        }
                ) {
                    drawBattlefieldGrid(showGrid = state.preferences.showHudGrid)

                    val minDim = size.minDimension
                    state.targets.forEach { t ->
                        val c = Offset(t.xNorm * size.width, t.yNorm * size.height)
                        val r = t.radiusNorm * minDim
                        val dist = kotlin.math.hypot(
                            (state.crosshairX - t.xNorm) * size.width,
                            (state.crosshairY - t.yNorm) * size.height
                        )
                        drawGeometricTarget(center = c, radius = r, isHighlighted = dist <= r)
                    }

                    state.bursts.forEach { b ->
                        drawCircle(
                            color = if (b.isHit) TacticalGreen.copy(alpha = 0.35f) else CrimsonRed.copy(alpha = 0.35f),
                            radius = 20.dp.toPx(),
                            center = Offset(b.xNorm * size.width, b.yNorm * size.height)
                        )
                    }

                    drawTacticalCrosshair(
                        center = Offset(state.crosshairX * size.width, state.crosshairY * size.height),
                        style = state.preferences.crosshairStyle,
                        hitFlash = isFlashing
                    )
                }

                // Bottom helper + Fire button for drag-then-fire users
                Text(
                    text = "TAP TARGETS TO FLICK OR DRAG + FIRE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                Button(
                    onClick = { viewModel.fireAtCurrentCrosshair() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = TextPrimary),
                    border = BorderStroke(1.5.dp, NeonOrange),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(64.dp)
                        .testTag("aim_fire_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Fire at crosshair"
                    )
                }
            }
        }

        // Complete 8-metric live telemetry breakdown
        TacticalCard {
            Text(
                text = "LIVE AIM & FLICK TELEMETRY",
                style = MaterialTheme.typography.titleMedium,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudStatBadge("ATTEMPTS", "${state.totalAttempts}", TextPrimary, Modifier.weight(1f))
                HudStatBadge("HITS", "${state.successfulHits}", TacticalGreen, Modifier.weight(1f))
                HudStatBadge("MISSES", "${state.missedTargets}", CrimsonRed, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudStatBadge("ACCURACY", String.format(Locale.US, "%.1f%%", state.accuracyPercent), NeonOrange, Modifier.weight(1f))
                HudStatBadge("CENTER ZONE", String.format(Locale.US, "%.1f%%", state.centerZoneAccuracyPercent), TacticalGreen, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudStatBadge(
                    "AVG REACTION",
                    if (state.avgReactionTimeMs > 0f) "${state.avgReactionTimeMs.toInt()} ms" else "--",
                    NeonOrange,
                    Modifier.weight(1f)
                )
                HudStatBadge(
                    "FLICK ACC",
                    String.format(Locale.US, "%.1f%%", state.flickAccuracyPercent),
                    TextPrimary,
                    Modifier.weight(1f)
                )
            }
            if (state.phase == SessionPhase.COMPLETED && state.sessionSaved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Challenge complete — metrics recorded to Analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalGreen
                )
            }
        }
    }
}
