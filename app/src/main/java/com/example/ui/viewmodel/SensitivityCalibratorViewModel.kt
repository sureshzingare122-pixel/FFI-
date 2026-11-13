package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import com.example.data.model.TrainingMode
import com.example.data.model.TrainingSession
import com.example.data.repository.TrainingRepository
import com.example.engine.CalibrationRecommendation
import com.example.engine.TrainingMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.hypot
import kotlin.random.Random

data class CalibrationTarget(
    val index: Int,
    val xNorm: Float,
    val yNorm: Float,
    val radiusNorm: Float = 0.075f,
    val spawnedAtMs: Long = System.currentTimeMillis()
)

data class SensitivityCalibratorUiState(
    val settings: SensitivitySettings = SensitivitySettings(),
    val previewCrosshairX: Float = 0.50f,
    val previewCrosshairY: Float = 0.50f,
    val previewTrail: List<TrailPoint> = emptyList(),
    val isChallengeActive: Boolean = false,
    val totalChallengeTargets: Int = 6,
    val currentTargetIndex: Int = 0,
    val activeTarget: CalibrationTarget? = null,
    val hits: Int = 0,
    val misses: Int = 0,
    val overshootCount: Int = 0,
    val undershootCount: Int = 0,
    val reactionTimesMs: List<Long> = emptyList(),
    val lastRecommendation: CalibrationRecommendation? = null
)

class SensitivityCalibratorViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensitivityCalibratorUiState())
    val uiState: StateFlow<SensitivityCalibratorUiState> = _uiState.asStateFlow()

    private var challengeStartMs: Long = 0L
    private var previousTargetX: Float = 0.5f
    private var previousTargetY: Float = 0.5f
    private var lastShotDistanceToTarget: Float = 0f

    init {
        viewModelScope.launch {
            repository.sensitivitySettingsFlow.collect { saved ->
                _uiState.update { state ->
                    val rec = if (saved.lastCalibrationScore != null &&
                        saved.recommendedCrosshairSensitivity != null &&
                        saved.recommendedJoystickResponse != null &&
                        state.lastRecommendation == null
                    ) {
                        CalibrationRecommendation(
                            calibrationScore = saved.lastCalibrationScore,
                            recommendedCrosshairSensitivity = saved.recommendedCrosshairSensitivity,
                            recommendedJoystickResponse = saved.recommendedJoystickResponse,
                            diagnosis = "Previous calibration telemetry loaded from local storage."
                        )
                    } else {
                        state.lastRecommendation
                    }
                    state.copy(settings = saved, lastRecommendation = rec)
                }
            }
        }
    }

    fun selectPreset(preset: SensitivityPreset) {
        viewModelScope.launch {
            repository.applyPreset(preset)
        }
    }

    fun updateCrosshairSensitivity(value: Float) {
        val clamped = value.coerceIn(0.40f, 2.80f)
        val updated = _uiState.value.settings.copy(
            activePreset = SensitivityPreset.CUSTOM.name,
            crosshairSensitivity = clamped
        )
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch {
            repository.updateSensitivitySettings(updated)
        }
    }

    fun updateJoystickResponse(value: Float) {
        val clamped = value.coerceIn(0.50f, 2.50f)
        val updated = _uiState.value.settings.copy(
            activePreset = SensitivityPreset.CUSTOM.name,
            joystickResponse = clamped
        )
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch {
            repository.updateSensitivitySettings(updated)
        }
    }

    fun onPreviewDrag(deltaXNorm: Float, deltaYNorm: Float) {
        val sens = _uiState.value.settings.crosshairSensitivity
        _uiState.update { state ->
            val nx = (state.previewCrosshairX + deltaXNorm * sens).coerceIn(0.06f, 0.94f)
            val ny = (state.previewCrosshairY + deltaYNorm * sens).coerceIn(0.10f, 0.90f)
            val trail = (state.previewTrail + TrailPoint(nx, ny)).takeLast(24)
            state.copy(
                previewCrosshairX = nx,
                previewCrosshairY = ny,
                previewTrail = trail
            )
        }
    }

    fun startCalibrationChallenge() {
        challengeStartMs = System.currentTimeMillis()
        previousTargetX = 0.50f
        previousTargetY = 0.50f
        val firstTarget = createChallengeTarget(1)
        lastShotDistanceToTarget = hypot(firstTarget.xNorm - 0.5f, firstTarget.yNorm - 0.5f)
        _uiState.update {
            it.copy(
                isChallengeActive = true,
                previewCrosshairX = 0.50f,
                previewCrosshairY = 0.50f,
                previewTrail = emptyList(),
                currentTargetIndex = 1,
                activeTarget = firstTarget,
                hits = 0,
                misses = 0,
                overshootCount = 0,
                undershootCount = 0,
                reactionTimesMs = emptyList()
            )
        }
    }

    fun fireCalibrationShot() {
        val state = _uiState.value
        if (!state.isChallengeActive) return
        val target = state.activeTarget ?: return

        val now = System.currentTimeMillis()
        val eval = TrainingMath.detectTargetHit(
            crosshairX = state.previewCrosshairX,
            crosshairY = state.previewCrosshairY,
            targetX = target.xNorm,
            targetY = target.yNorm,
            targetRadius = target.radiusNorm
        )

        var newHits = state.hits
        var newMisses = state.misses
        var newOvershoots = state.overshootCount
        var newUndershoots = state.undershootCount
        val newReactions = state.reactionTimesMs.toMutableList()

        if (eval.isHit) {
            newHits++
            newReactions.add((now - target.spawnedAtMs).coerceIn(100L, 4000L))
        } else {
            newMisses++
            // Determine overshoot vs undershoot relative to travel vector from previous target
            val travelDist = hypot(target.xNorm - previousTargetX, target.yNorm - previousTargetY)
            val crosshairTravel = hypot(state.previewCrosshairX - previousTargetX, state.previewCrosshairY - previousTargetY)
            if (crosshairTravel > travelDist) {
                newOvershoots++
            } else {
                newUndershoots++
            }
        }

        val totalAttempts = newHits + newMisses
        if (state.currentTargetIndex >= state.totalChallengeTargets && (eval.isHit || totalAttempts >= state.totalChallengeTargets + 2)) {
            finishChallenge(
                hits = newHits,
                misses = newMisses,
                overshoots = newOvershoots,
                undershoots = newUndershoots,
                reactions = newReactions
            )
        } else if (eval.isHit || newMisses >= state.totalChallengeTargets * 2) {
            val nextIdx = state.currentTargetIndex + 1
            if (nextIdx > state.totalChallengeTargets) {
                finishChallenge(newHits, newMisses, newOvershoots, newUndershoots, newReactions)
            } else {
                previousTargetX = target.xNorm
                previousTargetY = target.yNorm
                val nextTarget = createChallengeTarget(nextIdx)
                _uiState.update {
                    it.copy(
                        currentTargetIndex = nextIdx,
                        activeTarget = nextTarget,
                        hits = newHits,
                        misses = newMisses,
                        overshootCount = newOvershoots,
                        undershootCount = newUndershoots,
                        reactionTimesMs = newReactions
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    hits = newHits,
                    misses = newMisses,
                    overshootCount = newOvershoots,
                    undershootCount = newUndershoots,
                    reactionTimesMs = newReactions
                )
            }
        }
    }

    fun applyRecommendedSettings() {
        val rec = _uiState.value.lastRecommendation ?: return
        val updated = _uiState.value.settings.copy(
            activePreset = SensitivityPreset.CUSTOM.name,
            crosshairSensitivity = rec.recommendedCrosshairSensitivity,
            joystickResponse = rec.recommendedJoystickResponse
        )
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch {
            repository.updateSensitivitySettings(updated)
        }
    }

    private fun finishChallenge(
        hits: Int,
        misses: Int,
        overshoots: Int,
        undershoots: Int,
        reactions: List<Long>
    ) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val avgReaction = if (reactions.isEmpty()) 900f else reactions.average().toFloat()
        val recommendation = TrainingMath.computeCalibrationRecommendation(
            currentCrosshairSens = state.settings.crosshairSensitivity,
            currentJoystickResp = state.settings.joystickResponse,
            hits = hits,
            totalTargets = state.totalChallengeTargets,
            overshootCount = overshoots,
            undershootCount = undershoots,
            avgReactionMs = avgReaction
        )

        val updatedSettings = state.settings.copy(
            recommendedCrosshairSensitivity = recommendation.recommendedCrosshairSensitivity,
            recommendedJoystickResponse = recommendation.recommendedJoystickResponse,
            lastCalibrationScore = recommendation.calibrationScore,
            lastCalibratedAtMs = now
        )

        _uiState.update {
            it.copy(
                isChallengeActive = false,
                activeTarget = null,
                hits = hits,
                misses = misses,
                overshootCount = overshoots,
                undershootCount = undershoots,
                reactionTimesMs = reactions,
                settings = updatedSettings,
                lastRecommendation = recommendation
            )
        }

        viewModelScope.launch {
            repository.updateSensitivitySettings(updatedSettings)
            val totalAttempts = (hits + misses).coerceAtLeast(1)
            val acc = TrainingMath.computeAccuracyPercent(hits, totalAttempts)
            repository.recordSession(
                TrainingSession(
                    sessionKey = UUID.randomUUID().toString(),
                    mode = TrainingMode.SENSITIVITY_CALIBRATION.name,
                    subMode = state.settings.activePreset,
                    profileName = state.settings.activePreset,
                    timestampMs = now,
                    durationMs = (now - challengeStartMs).coerceAtLeast(1000L),
                    totalAttempts = totalAttempts,
                    successfulHits = hits,
                    missedTargets = misses,
                    accuracyPercent = acc,
                    centerZoneAccuracyPercent = acc,
                    avgReactionTimeMs = avgReaction,
                    flickAccuracyPercent = acc,
                    score = recommendation.calibrationScore * 10,
                    performanceRatio = TrainingMath.computePerformanceRatio(acc, avgReaction, acc)
                )
            )
        }
    }

    private fun createChallengeTarget(index: Int): CalibrationTarget {
        val x = Random.nextFloat() * 0.68f + 0.16f
        val y = Random.nextFloat() * 0.60f + 0.18f
        return CalibrationTarget(
            index = index,
            xNorm = x,
            yNorm = y,
            radiusNorm = 0.080f,
            spawnedAtMs = System.currentTimeMillis()
        )
    }
}
