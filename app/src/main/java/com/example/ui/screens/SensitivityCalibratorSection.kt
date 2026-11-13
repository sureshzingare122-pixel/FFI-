package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CrosshairStyle
import com.example.data.model.SensitivityPreset
import com.example.ui.components.HudPrimaryButton
import com.example.ui.components.HudStatBadge
import com.example.ui.components.TacticalCard
import com.example.ui.components.drawBattlefieldGrid
import com.example.ui.components.drawGeometricTarget
import com.example.ui.components.drawTacticalCrosshair
import com.example.ui.theme.AmberTelemetry
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonOrangeDim
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SensitivityCalibratorViewModel
import com.example.util.FeedbackEngine
import java.util.Locale

@Composable
fun SensitivityCalibratorSection(
    viewModel: SensitivityCalibratorViewModel,
    feedbackEngine: FeedbackEngine,
    hapticEnabled: Boolean,
    soundEnabled: Boolean
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Explicit Independent Simulator Disclaimer
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeonOrangeDim,
            border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = NeonOrange
                )
                Text(
                    text = stringResource(R.string.calibration_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }

        // Live Movement Preview & Guided Calibration Challenge
        TacticalCard(contentPadding = 10.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isChallengeActive) {
                            "CALIBRATION TARGET ${state.currentTargetIndex}/${state.totalChallengeTargets}"
                        } else {
                            "LIVE MOVEMENT PREVIEW & CHALLENGE"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.isChallengeActive) {
                            "Drag crosshair onto the target ring, then tap FIRE."
                        } else {
                            "Drag inside the pad to test feel, or start the 6-target calibration test."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (!state.isChallengeActive) {
                    HudPrimaryButton(
                        text = "CALIBRATE",
                        icon = Icons.Filled.Tune,
                        onClick = { viewModel.startCalibrationChallenge() },
                        modifier = Modifier.testTag("start_calibration_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(12.dp),
                color = MatteBlack,
                border = BorderStroke(1.dp, if (state.isChallengeActive) NeonOrange else TacticalBorder)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(widthPx, heightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    viewModel.onPreviewDrag(
                                        deltaXNorm = dragAmount.x / widthPx,
                                        deltaYNorm = dragAmount.y / heightPx
                                    )
                                }
                            }
                    ) {
                        drawBattlefieldGrid(showGrid = true)

                        if (state.previewTrail.size >= 2) {
                            val trailPath = Path().apply {
                                val first = state.previewTrail.first()
                                moveTo(first.xNorm * size.width, first.yNorm * size.height)
                                state.previewTrail.drop(1).forEach { pt ->
                                    lineTo(pt.xNorm * size.width, pt.yNorm * size.height)
                                }
                            }
                            drawPath(
                                path = trailPath,
                                color = NeonOrange.copy(alpha = 0.55f),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        state.activeTarget?.let { t ->
                            drawGeometricTarget(
                                center = Offset(t.xNorm * size.width, t.yNorm * size.height),
                                radius = t.radiusNorm * size.minDimension,
                                isHighlighted = false
                            )
                        }

                        drawTacticalCrosshair(
                            center = Offset(
                                state.previewCrosshairX * size.width,
                                state.previewCrosshairY * size.height
                            ),
                            style = CrosshairStyle.TACTICAL_DOT
                        )
                    }

                    if (state.isChallengeActive) {
                        Button(
                            onClick = {
                                feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                                viewModel.fireCalibrationShot()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = TextPrimary),
                            border = BorderStroke(1.5.dp, NeonOrange),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(62.dp)
                                .testTag("calibration_fire_button")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Fire calibration shot")
                        }
                    }
                }
            }
        }

        // Preset selector + Sensitivity sliders
        TacticalCard {
            Text(
                text = "SENSITIVITY PRESETS",
                style = MaterialTheme.typography.labelLarge,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensitivityPreset.entries.forEach { preset ->
                    val selected = state.settings.activePreset == preset.name
                    FilterChip(
                        selected = selected,
                        onClick = {
                            feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                            viewModel.selectPreset(preset)
                        },
                        label = { Text(preset.name, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = MatteBlack,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preset_chip_${preset.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ParameterSlider(
                label = "Crosshair Sensitivity",
                valueText = String.format(Locale.US, "%.2fx", state.settings.crosshairSensitivity),
                value = state.settings.crosshairSensitivity,
                range = 0.40f..2.80f,
                onValueChange = { viewModel.updateCrosshairSensitivity(it) }
            )
            ParameterSlider(
                label = "Joystick Response",
                valueText = String.format(Locale.US, "%.2fx", state.settings.joystickResponse),
                value = state.settings.joystickResponse,
                range = 0.50f..2.50f,
                onValueChange = { viewModel.updateJoystickResponse(it) }
            )
        }

        // Calibration Score & Recommendation Card
        state.lastRecommendation?.let { rec ->
            TacticalCard(borderColor = TacticalGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALIBRATION RESULT",
                        style = MaterialTheme.typography.titleMedium,
                        color = TacticalGreen
                    )
                    HudStatBadge(
                        label = "CAL SCORE",
                        value = "${rec.calibrationScore}/100",
                        accentColor = if (rec.calibrationScore >= 75) TacticalGreen else AmberTelemetry
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rec.diagnosis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudStatBadge(
                        label = "REC CROSSHAIR",
                        value = String.format(Locale.US, "%.2fx", rec.recommendedCrosshairSensitivity),
                        accentColor = NeonOrange,
                        modifier = Modifier.weight(1f)
                    )
                    HudStatBadge(
                        label = "REC JOYSTICK",
                        value = String.format(Locale.US, "%.2fx", rec.recommendedJoystickResponse),
                        accentColor = NeonOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                HudPrimaryButton(
                    text = "APPLY SIMULATOR RECOMMENDATION",
                    icon = Icons.Filled.Check,
                    onClick = { viewModel.applyRecommendedSettings() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_recommendation_button")
                )
            }
        }
    }
}
