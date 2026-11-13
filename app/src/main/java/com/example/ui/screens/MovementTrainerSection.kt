package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HudPrimaryButton
import com.example.ui.components.HudSecondaryButton
import com.example.ui.components.HudStatBadge
import com.example.ui.components.TacticalCard
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.drawBattlefieldGrid
import com.example.ui.theme.AmberTelemetry
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
import com.example.ui.viewmodel.MovementPattern
import com.example.ui.viewmodel.MovementTrainerViewModel
import com.example.util.FeedbackEngine
import java.util.Locale
import kotlin.math.pow

@Composable
fun MovementTrainerSection(
    viewModel: MovementTrainerViewModel,
    feedbackEngine: FeedbackEngine,
    hapticEnabled: Boolean,
    soundEnabled: Boolean
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Profile selector: RUSH / DEFENSIVE / SNIPER
        TacticalCard {
            Text(
                text = "MOVEMENT PROFILE",
                style = MaterialTheme.typography.labelLarge,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val profileIds = listOf("RUSH", "DEFENSIVE", "SNIPER")
                profileIds.forEach { pid ->
                    val selected = state.selectedProfile.profileId == pid
                    FilterChip(
                        selected = selected,
                        onClick = {
                            feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                            viewModel.selectProfile(pid)
                        },
                        label = {
                            Text(
                                text = pid,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = MatteBlack,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_chip_${pid.lowercase()}")
                    )
                }
            }
            Text(
                text = state.selectedProfile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Pattern selector (all 5 movement patterns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MovementPattern.entries.forEach { pattern ->
                val selected = state.selectedPattern == pattern
                FilterChip(
                    selected = selected,
                    onClick = {
                        feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                        viewModel.selectPattern(pattern)
                    },
                    label = { Text(pattern.title, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonRed,
                        selectedLabelColor = TextPrimary,
                        containerColor = ObsidianPanel,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("pattern_chip_${pattern.name.lowercase()}")
                )
            }
        }

        // Drill HUD + Interactive Canvas
        TacticalCard(contentPadding = 10.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudStatBadge(
                    label = "TIMER",
                    value = "${state.remainingSec}s",
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                HudStatBadge(
                    label = "PATH ACC",
                    value = String.format(Locale.US, "%.1f%%", state.movementAccuracyPercent),
                    accentColor = TacticalGreen,
                    modifier = Modifier.weight(1f)
                )
                if (state.isDrillRunning) {
                    HudSecondaryButton(
                        text = "STOP",
                        icon = Icons.Filled.Stop,
                        borderColor = CrimsonRed,
                        contentColor = CrimsonRed,
                        onClick = { viewModel.stopDrill(save = true) }
                    )
                } else {
                    HudPrimaryButton(
                        text = "START DRILL",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { viewModel.startDrill() },
                        modifier = Modifier.testTag("movement_start_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.selectedPattern.instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Movement Canvas Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp),
                shape = RoundedCornerShape(12.dp),
                color = MatteBlack,
                border = BorderStroke(
                    1.dp,
                    if (state.actionCueActive) AmberTelemetry else TacticalBorder
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.selectedPattern) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (state.selectedPattern == MovementPattern.DRAG_AIM) {
                                        viewModel.onDragAimMove(
                                            deltaXNorm = dragAmount.x / size.width.toFloat(),
                                            deltaYNorm = dragAmount.y / size.height.toFloat()
                                        )
                                    }
                                }
                            }
                    ) {
                        drawBattlefieldGrid(showGrid = true)

                        // Pattern guide geometry
                        when (state.selectedPattern) {
                            MovementPattern.LEFT_RIGHT -> {
                                drawLine(
                                    color = NeonOrange.copy(alpha = 0.35f),
                                    start = Offset(size.width * 0.20f, size.height * 0.55f),
                                    end = Offset(size.width * 0.80f, size.height * 0.55f),
                                    strokeWidth = 4.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                                )
                            }
                            MovementPattern.CIRCULAR -> {
                                drawOval(
                                    color = NeonOrange.copy(alpha = 0.35f),
                                    topLeft = Offset(size.width * 0.26f, size.height * 0.30f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.44f),
                                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                                )
                            }
                            MovementPattern.DRAG_AIM -> {
                                // Lock zone in upper region
                                drawCircle(
                                    color = TacticalGreen.copy(alpha = 0.35f),
                                    radius = 28.dp.toPx(),
                                    center = Offset(size.width * 0.50f, size.height * 0.24f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                val aimPos = Offset(state.dragAimX * size.width, state.dragAimY * size.height)
                                drawCircle(color = CrimsonRed, radius = 9.dp.toPx(), center = aimPos)
                                drawCircle(color = TextPrimary, radius = 3.dp.toPx(), center = aimPos)
                            }
                            else -> Unit
                        }

                        // Guide orb
                        val guidePos = Offset(state.guideX * size.width, state.guideY * size.height)
                        drawCircle(
                            color = TacticalGreen.copy(alpha = 0.25f),
                            radius = 24.dp.toPx(),
                            center = guidePos
                        )
                        drawCircle(
                            color = TacticalGreen,
                            radius = 12.dp.toPx(),
                            center = guidePos,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Movement path trail
                        if (state.pathTrail.size >= 2) {
                            val trailPath = Path().apply {
                                val first = state.pathTrail.first()
                                moveTo(first.xNorm * size.width, first.yNorm * size.height)
                                state.pathTrail.drop(1).forEach { pt ->
                                    lineTo(pt.xNorm * size.width, pt.yNorm * size.height)
                                }
                            }
                            drawPath(
                                path = trailPath,
                                color = NeonOrange.copy(alpha = 0.70f),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Player avatar orb (scales on crouch / jump)
                        val playerPos = Offset(
                            state.playerX * size.width,
                            (state.playerY - state.jumpElevation * 0.08f) * size.height
                        )
                        val radius = when {
                            state.isCrouching -> 8.dp.toPx()
                            state.jumpElevation > 0.1f -> 15.dp.toPx()
                            else -> 12.dp.toPx()
                        }
                        drawCircle(color = NeonOrange, radius = radius, center = playerPos)
                        drawCircle(color = MatteBlack, radius = radius * 0.4f, center = playerPos)
                    }

                    if (state.actionCueActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberTelemetry,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = state.actionCueLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MatteBlack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    VirtualJoystick(
                        deadZone = state.selectedProfile.joystickDeadZone,
                        size = 104.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        onVectorChange = { vx, vy -> viewModel.onJoystickMove(vx, vy) }
                    )

                    // Simulated Crouch / Jump controls on right
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        HudSecondaryButton(
                            text = if (state.isCrouching) "STAND" else "CROUCH",
                            borderColor = if (state.isCrouching) TacticalGreen else TacticalBorder,
                            contentColor = if (state.isCrouching) TacticalGreen else TextPrimary,
                            onClick = {
                                feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                                viewModel.triggerCrouch()
                            },
                            modifier = Modifier.testTag("movement_crouch_button")
                        )
                        HudSecondaryButton(
                            text = "JUMP",
                            borderColor = NeonOrange,
                            contentColor = NeonOrange,
                            onClick = {
                                feedbackEngine.onActionTap(view, hapticEnabled, soundEnabled)
                                viewModel.triggerJump()
                            },
                            modifier = Modifier.testTag("movement_jump_button")
                        )
                    }
                }
            }
        }

        // Profile Tuning Sliders + Response Curve Visualizer
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.selectedProfile.displayName} RESPONSE TUNING",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "AUTO-SAVED",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Response Curve Preview Graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .width(100.dp)
                        .height(72.dp)
                        .background(MatteBlack, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val exp = state.selectedProfile.responseCurveExponent
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        for (i in 1..20) {
                            val xNorm = i / 20f
                            val yNorm = xNorm.pow(exp)
                            lineTo(xNorm * size.width, size.height * (1f - yNorm))
                        }
                    }
                    drawLine(
                        color = TacticalBorder,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawPath(path, color = NeonOrange, style = Stroke(width = 2.5.dp.toPx()))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Response Curve: ${String.format(Locale.US, "%.2f", state.selectedProfile.responseCurveExponent)}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = "1.0 = linear; higher values soften micro-tilt near the dead zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ParameterSlider(
                label = "Joystick Dead Zone",
                valueText = String.format(Locale.US, "%.0f%%", state.selectedProfile.joystickDeadZone * 100f),
                value = state.selectedProfile.joystickDeadZone,
                range = 0.02f..0.35f,
                onValueChange = { viewModel.updateProfileParameters(deadZone = it) }
            )
            ParameterSlider(
                label = "Joystick Sensitivity",
                valueText = String.format(Locale.US, "%.2fx", state.selectedProfile.joystickSensitivity),
                value = state.selectedProfile.joystickSensitivity,
                range = 0.50f..2.50f,
                onValueChange = { viewModel.updateProfileParameters(sensitivity = it) }
            )
            ParameterSlider(
                label = "Movement Acceleration",
                valueText = String.format(Locale.US, "%.2fx", state.selectedProfile.movementAcceleration),
                value = state.selectedProfile.movementAcceleration,
                range = 0.50f..2.50f,
                onValueChange = { viewModel.updateProfileParameters(acceleration = it) }
            )
            ParameterSlider(
                label = "Response Curve",
                valueText = String.format(Locale.US, "%.2f", state.selectedProfile.responseCurveExponent),
                value = state.selectedProfile.responseCurveExponent,
                range = 0.80f..2.50f,
                onValueChange = { viewModel.updateProfileParameters(responseCurve = it) }
            )
            ParameterSlider(
                label = "Drag-Aim Sensitivity",
                valueText = String.format(Locale.US, "%.2fx", state.selectedProfile.dragAimSensitivity),
                value = state.selectedProfile.dragAimSensitivity,
                range = 0.50f..2.50f,
                onValueChange = { viewModel.updateProfileParameters(dragAimSensitivity = it) }
            )
        }
    }
}

@Composable
fun ParameterSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = NeonOrange)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonOrange,
                activeTrackColor = NeonOrange,
                inactiveTrackColor = ElevatedDarkGrey
            )
        )
    }
}
