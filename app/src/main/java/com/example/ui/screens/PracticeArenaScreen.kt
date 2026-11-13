package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HudPrimaryButton
import com.example.ui.components.HudSecondaryButton
import com.example.ui.components.HudStatBadge
import com.example.ui.components.TacticalCard
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.drawBattlefieldGrid
import com.example.ui.components.drawGeometricTarget
import com.example.ui.components.drawTacticalCrosshair
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
import com.example.ui.viewmodel.PracticeArenaViewModel
import com.example.ui.viewmodel.PracticeFeedbackEvent
import com.example.ui.viewmodel.SessionPhase
import com.example.util.FeedbackEngine
import java.util.Locale

@Composable
fun PracticeArenaScreen(
    viewModel: PracticeArenaViewModel,
    feedbackEngine: FeedbackEngine,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val haptic = state.preferences.hapticFeedbackEnabled
            val sound = state.preferences.soundEffectsEnabled
            when (event) {
                is PracticeFeedbackEvent.Hit -> feedbackEngine.onHit(view, haptic, sound, event.centerZone)
                PracticeFeedbackEvent.Miss -> feedbackEngine.onMiss(view, haptic, sound)
                PracticeFeedbackEvent.SessionCompleted -> feedbackEngine.onSessionComplete(view, haptic, sound)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("practice_arena_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top HUD Telemetry Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mins = state.remainingSec / 60
            val secs = state.remainingSec % 60
            HudStatBadge(
                label = "TIME",
                value = String.format(Locale.US, "%02d:%02d", mins, secs),
                accentColor = if (state.remainingSec <= 10 && state.phase == SessionPhase.RUNNING) CrimsonRed else NeonOrange,
                modifier = Modifier.weight(1f)
            )
            HudStatBadge(
                label = "SCORE",
                value = state.score.toString(),
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            HudStatBadge(
                label = "ACC",
                value = String.format(Locale.US, "%.0f%%", state.accuracyPercent),
                accentColor = TacticalGreen,
                modifier = Modifier.weight(1f)
            )
            HudStatBadge(
                label = "REACT",
                value = if (state.avgReactionTimeMs > 0f) "${state.avgReactionTimeMs.toInt()}ms" else "--",
                accentColor = NeonOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Control Bar: Start / Stop / Reset + Profile indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianPanel,
                border = BorderStroke(1.dp, TacticalBorder),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PROFILE: ${state.activeProfileName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = "H:${state.hits}  M:${state.misses}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (state.phase == SessionPhase.RUNNING) {
                HudSecondaryButton(
                    text = "END",
                    icon = Icons.Filled.Stop,
                    borderColor = CrimsonRed,
                    contentColor = CrimsonRed,
                    onClick = { viewModel.finishEarlyAndSave() },
                    modifier = Modifier.testTag("practice_end_button")
                )
            } else {
                HudPrimaryButton(
                    text = if (state.phase == SessionPhase.COMPLETED) "RETRY" else "START",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { viewModel.startSession() },
                    modifier = Modifier.testTag("practice_start_button")
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElevatedDarkGrey,
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                IconButton(
                    onClick = { viewModel.resetSession() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("practice_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset practice session",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Interactive Canvas Arena + Dual-Thumb HUD Controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = ObsidianPanel,
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val now = System.currentTimeMillis()
                val isHitFlashing = now < state.hitFlashUntilMs

                // Aim Control Drag Layer covering the arena
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("practice_arena_canvas")
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

                    // Draw moving geometric targets
                    val minDim = size.minDimension
                    state.targets.forEach { target ->
                        val center = Offset(target.xNorm * size.width, target.yNorm * size.height)
                        val radiusPx = target.radiusNorm * minDim
                        val dist = kotlin.math.hypot(
                            (state.crosshairX - target.xNorm) * size.width,
                            (state.crosshairY - target.yNorm) * size.height
                        )
                        drawGeometricTarget(
                            center = center,
                            radius = radiusPx,
                            isHighlighted = dist <= radiusPx
                        )
                    }

                    // Hit / Miss pulse rings
                    state.feedbackBursts.forEach { burst ->
                        val pos = Offset(burst.xNorm * size.width, burst.yNorm * size.height)
                        val burstColor = when {
                            burst.isCenterZone -> TacticalGreen
                            burst.isHit -> NeonOrange
                            else -> CrimsonRed
                        }
                        drawCircle(
                            color = burstColor.copy(alpha = 0.35f),
                            radius = 22.dp.toPx(),
                            center = pos
                        )
                    }

                    // Crosshair reticle
                    drawTacticalCrosshair(
                        center = Offset(state.crosshairX * size.width, state.crosshairY * size.height),
                        style = state.preferences.crosshairStyle,
                        hitFlash = isHitFlashing
                    )
                }

                // Helper banner when IDLE
                if (state.phase == SessionPhase.IDLE) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MatteBlack.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "DRAG AIM AREA • STEER JOYSTICK • TAP FIRE TO BEGIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonOrange,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Left-Bottom Virtual Joystick
                VirtualJoystick(
                    deadZone = state.activeDeadZone,
                    size = 124.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    onVectorChange = { vx, vy -> viewModel.onJoystickMove(vx, vy) }
                )

                // Right-Bottom Simulated Action / Fire Button + Aim Zone hint
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "AIM PAD ABOVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Button(
                        onClick = { viewModel.fireAction() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonRed,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(2.dp, NeonOrange),
                        modifier = Modifier
                            .size(84.dp)
                            .testTag("practice_fire_button")
                            .semantics { contentDescription = "Simulated fire action button" }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "FIRE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Completed Session Debrief Overlay
                if (state.phase == SessionPhase.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MatteBlack.copy(alpha = 0.84f))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TacticalCard(
                            borderColor = NeonOrange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("practice_completed_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = TacticalGreen
                                )
                                Text(
                                    text = "PRACTICE SESSION COMPLETE",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HudStatBadge("FINAL SCORE", "${state.score}", NeonOrange, Modifier.weight(1f))
                                HudStatBadge(
                                    "ACCURACY",
                                    String.format(Locale.US, "%.1f%%", state.accuracyPercent),
                                    TacticalGreen,
                                    Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HudStatBadge(
                                    "CENTER ZONE",
                                    String.format(Locale.US, "%.1f%%", state.centerZoneAccuracyPercent),
                                    TextPrimary,
                                    Modifier.weight(1f)
                                )
                                HudStatBadge(
                                    "AVG REACT",
                                    "${state.avgReactionTimeMs.toInt()} ms",
                                    NeonOrange,
                                    Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (state.sessionSaved) {
                                    "Session telemetry saved locally to analytics database."
                                } else if (state.totalAttempts == 0) {
                                    "No shots recorded — fire at targets to log analytics."
                                } else {
                                    "Saving telemetry..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HudPrimaryButton(
                                    text = "PLAY AGAIN",
                                    icon = Icons.Filled.PlayArrow,
                                    onClick = { viewModel.startSession() },
                                    modifier = Modifier.weight(1f)
                                )
                                HudSecondaryButton(
                                    text = "RESET",
                                    icon = Icons.Filled.Refresh,
                                    onClick = { viewModel.resetSession() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
