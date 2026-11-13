package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.model.TrainingMode
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import com.example.data.repository.TrainingRepository
import com.example.engine.TrainingMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class MovementPattern(val title: String, val instruction: String) {
    LEFT_RIGHT("Left-Right", "Strafe smoothly along the horizontal corridor matching the guide orb."),
    CIRCULAR("Circular", "Trace the 360-degree orbital ring steadily using the virtual joystick."),
    CROUCH("Crouch Sim", "Hold center zone and trigger CROUCH on cue windows for recoil stability."),
    JUMP("Jump Timing", "Time JUMP triggers inside highlighted pulse windows while maintaining movement."),
    DRAG_AIM("Drag-Aim", "Move with the left joystick while dragging the aim dot upward into the lock box.")
}

data class TrailPoint(
    val xNorm: Float,
    val yNorm: Float
)

data class MovementTrainerUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile = UserProfile(
        profileId = "RUSH",
        displayName = "RUSH",
        description = "High-tempo entry movement.",
        joystickDeadZone = 0.06f,
        joystickSensitivity = 1.65f,
        movementAcceleration = 1.55f,
        responseCurveExponent = 1.15f,
        dragAimSensitivity = 1.55f
    ),
    val selectedPattern: MovementPattern = MovementPattern.LEFT_RIGHT,
    val isDrillRunning: Boolean = false,
    val drillDurationSec: Int = 25,
    val remainingSec: Int = 25,
    val playerX: Float = 0.5f,
    val playerY: Float = 0.55f,
    val guideX: Float = 0.5f,
    val guideY: Float = 0.55f,
    val dragAimX: Float = 0.5f,
    val dragAimY: Float = 0.45f,
    val isCrouching: Boolean = false,
    val jumpElevation: Float = 0f,
    val actionCueActive: Boolean = false,
    val actionCueLabel: String = "",
    val pathTrail: List<TrailPoint> = emptyList(),
    val movementAccuracyPercent: Float = 0f,
    val totalSamples: Int = 0,
    val onTrackSamples: Int = 0,
    val drillCompleted: Boolean = false,
    val lastScore: Int = 0,
    val joystickX: Float = 0f,
    val joystickY: Float = 0f
)

class MovementTrainerViewModel(
    private val repository: TrainingRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementTrainerUiState())
    val uiState: StateFlow<MovementTrainerUiState> = _uiState.asStateFlow()

    private var drillJob: Job? = null
    private var sessionKey: String = UUID.randomUUID().toString()
    private var drillStartMs: Long = 0L
    private var phaseTimeSec: Float = 0f
    private var actionCueCooldownSec: Float = 0f
    private var cueHits: Int = 0
    private var cueOpportunities: Int = 0
    private var cueSatisfiedThisWindow: Boolean = false

    init {
        viewModelScope.launch {
            combine(
                repository.profilesFlow,
                preferencesRepository.preferencesFlow
            ) { profiles, prefs ->
                profiles to prefs.activeMovementProfileId
            }.collect { (profiles, activeId) ->
                if (profiles.isNotEmpty()) {
                    val active = profiles.firstOrNull { it.profileId == activeId } ?: profiles.first()
                    _uiState.update { state ->
                        state.copy(
                            profiles = profiles,
                            selectedProfile = active
                        )
                    }
                }
            }
        }
    }

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            preferencesRepository.setActiveMovementProfileId(profileId)
        }
    }

    fun selectPattern(pattern: MovementPattern) {
        stopDrill(save = false)
        _uiState.update {
            it.copy(
                selectedPattern = pattern,
                playerX = 0.5f,
                playerY = 0.55f,
                guideX = 0.5f,
                guideY = 0.55f,
                dragAimX = 0.5f,
                dragAimY = 0.45f,
                pathTrail = emptyList(),
                drillCompleted = false,
                movementAccuracyPercent = 0f,
                totalSamples = 0,
                onTrackSamples = 0
            )
        }
    }

    fun updateProfileParameters(
        deadZone: Float? = null,
        sensitivity: Float? = null,
        acceleration: Float? = null,
        responseCurve: Float? = null,
        dragAimSensitivity: Float? = null
    ) {
        val current = _uiState.value.selectedProfile
        val updated = current.copy(
            joystickDeadZone = (deadZone ?: current.joystickDeadZone).coerceIn(0.02f, 0.35f),
            joystickSensitivity = (sensitivity ?: current.joystickSensitivity).coerceIn(0.5f, 2.5f),
            movementAcceleration = (acceleration ?: current.movementAcceleration).coerceIn(0.5f, 2.5f),
            responseCurveExponent = (responseCurve ?: current.responseCurveExponent).coerceIn(0.8f, 2.5f),
            dragAimSensitivity = (dragAimSensitivity ?: current.dragAimSensitivity).coerceIn(0.5f, 2.5f)
        )
        _uiState.update { it.copy(selectedProfile = updated) }
        viewModelScope.launch {
            repository.updateProfile(updated)
        }
    }

    fun onJoystickMove(rawX: Float, rawY: Float) {
        _uiState.update { it.copy(joystickX = rawX, joystickY = rawY) }
    }

    fun onDragAimMove(deltaXNorm: Float, deltaYNorm: Float) {
        val dragSens = _uiState.value.selectedProfile.dragAimSensitivity
        _uiState.update { state ->
            state.copy(
                dragAimX = (state.dragAimX + deltaXNorm * dragSens).coerceIn(0.10f, 0.90f),
                dragAimY = (state.dragAimY + deltaYNorm * dragSens).coerceIn(0.12f, 0.85f)
            )
        }
    }

    fun triggerCrouch() {
        val state = _uiState.value
        val togglingOn = !state.isCrouching
        if (state.isDrillRunning &&
            state.selectedPattern == MovementPattern.CROUCH &&
            state.actionCueActive &&
            togglingOn &&
            !cueSatisfiedThisWindow
        ) {
            cueHits++
            cueSatisfiedThisWindow = true
        }
        _uiState.update { it.copy(isCrouching = togglingOn) }
    }

    fun triggerJump() {
        val state = _uiState.value
        if (state.jumpElevation <= 0.05f) {
            if (state.isDrillRunning &&
                state.selectedPattern == MovementPattern.JUMP &&
                state.actionCueActive &&
                !cueSatisfiedThisWindow
            ) {
                cueHits++
                cueSatisfiedThisWindow = true
            }
            _uiState.update { it.copy(jumpElevation = 1.0f) }
        }
    }

    fun startDrill() {
        drillJob?.cancel()
        sessionKey = UUID.randomUUID().toString()
        drillStartMs = System.currentTimeMillis()
        phaseTimeSec = 0f
        actionCueCooldownSec = 0f
        cueHits = 0
        cueOpportunities = 0
        cueSatisfiedThisWindow = false

        _uiState.update {
            it.copy(
                isDrillRunning = true,
                remainingSec = it.drillDurationSec,
                playerX = 0.5f,
                playerY = 0.55f,
                dragAimX = 0.5f,
                dragAimY = 0.45f,
                pathTrail = emptyList(),
                totalSamples = 0,
                onTrackSamples = 0,
                movementAccuracyPercent = 0f,
                drillCompleted = false,
                isCrouching = false,
                jumpElevation = 0f
            )
        }

        drillJob = viewModelScope.launch {
            var lastMs = System.currentTimeMillis()
            while (_uiState.value.isDrillRunning) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = ((now - lastMs).coerceIn(1L, 50L)) / 1000f
                lastMs = now
                phaseTimeSec += dt

                val elapsedSec = ((now - drillStartMs) / 1000L).toInt()
                val remaining = (_uiState.value.drillDurationSec - elapsedSec).coerceAtLeast(0)
                stepSimulation(dt, remaining)

                if (remaining <= 0) {
                    stopDrill(save = true)
                    break
                }
            }
        }
    }

    fun stopDrill(save: Boolean = true) {
        val wasRunning = _uiState.value.isDrillRunning
        drillJob?.cancel()
        if (!wasRunning) return

        val state = _uiState.value
        val now = System.currentTimeMillis()
        val durationMs = (now - drillStartMs).coerceAtLeast(1000L)
        val accuracy = state.movementAccuracyPercent
        val score = (accuracy * 12f).toInt()

        _uiState.update {
            it.copy(
                isDrillRunning = false,
                drillCompleted = save,
                lastScore = score
            )
        }

        if (save && state.totalSamples > 10) {
            val session = TrainingSession(
                sessionKey = sessionKey,
                mode = TrainingMode.MOVEMENT_TRAINER.name,
                subMode = state.selectedPattern.name,
                profileName = state.selectedProfile.displayName,
                timestampMs = now,
                durationMs = durationMs,
                totalAttempts = state.totalSamples,
                successfulHits = state.onTrackSamples,
                missedTargets = (state.totalSamples - state.onTrackSamples).coerceAtLeast(0),
                accuracyPercent = accuracy,
                centerZoneAccuracyPercent = accuracy,
                avgReactionTimeMs = 0f,
                flickAccuracyPercent = accuracy,
                score = score,
                performanceRatio = TrainingMath.computePerformanceRatio(
                    accuracyPercent = accuracy,
                    avgReactionTimeMs = 340f,
                    centerZoneAccuracyPercent = accuracy
                )
            )
            viewModelScope.launch {
                repository.recordSession(session)
            }
        }
    }

    private fun stepSimulation(dt: Float, remainingSec: Int) {
        _uiState.update { state ->
            val p = state.selectedProfile
            val vec = TrainingMath.applyJoystickResponse(
                rawX = state.joystickX,
                rawY = state.joystickY,
                deadZone = p.joystickDeadZone,
                sensitivity = p.joystickSensitivity,
                acceleration = p.movementAcceleration,
                responseCurveExponent = p.responseCurveExponent
            )

            val crouchFactor = if (state.isCrouching) 0.55f else 1.0f
            val nextX = (state.playerX + vec.x * dt * 0.34f * crouchFactor).coerceIn(0.08f, 0.92f)
            val nextY = (state.playerY + vec.y * dt * 0.34f * crouchFactor).coerceIn(0.16f, 0.86f)
            val nextJump = (state.jumpElevation - dt * 2.1f).coerceAtLeast(0f)

            // Compute guide position based on selected movement pattern
            val (gx, gy) = when (state.selectedPattern) {
                MovementPattern.LEFT_RIGHT -> {
                    val x = 0.5f + 0.30f * sin(phaseTimeSec * 1.9f)
                    x to 0.55f
                }
                MovementPattern.CIRCULAR -> {
                    val angle = phaseTimeSec * 1.8f
                    (0.5f + 0.24f * cos(angle)) to (0.52f + 0.22f * sin(angle))
                }
                MovementPattern.CROUCH -> {
                    val x = 0.5f + 0.20f * sin(phaseTimeSec * 1.3f)
                    x to 0.55f
                }
                MovementPattern.JUMP -> {
                    val x = 0.5f + 0.24f * sin(phaseTimeSec * 1.6f)
                    x to 0.55f
                }
                MovementPattern.DRAG_AIM -> {
                    val x = 0.5f + 0.22f * sin(phaseTimeSec * 1.4f)
                    x to 0.58f
                }
            }

            // Cue windows for Crouch / Jump patterns (every ~2.8s)
            val cyclePos = phaseTimeSec % 2.8f
            val cueNow = (state.selectedPattern == MovementPattern.CROUCH ||
                state.selectedPattern == MovementPattern.JUMP) && cyclePos in 1.6f..2.5f

            if (cueNow && !state.actionCueActive) {
                cueOpportunities++
                cueSatisfiedThisWindow = false
            }

            val cueLabel = when {
                !cueNow -> ""
                state.selectedPattern == MovementPattern.CROUCH -> "CROUCH NOW!"
                state.selectedPattern == MovementPattern.JUMP -> "JUMP NOW!"
                else -> ""
            }

            val pathDist = hypot(nextX - gx, nextY - gy)
            val isOnPath = pathDist <= 0.16f

            val dragAimOnTarget = if (state.selectedPattern == MovementPattern.DRAG_AIM) {
                val targetAimX = 0.5f + 0.16f * cos(phaseTimeSec * 1.2f)
                val targetAimY = 0.24f
                hypot(state.dragAimX - targetAimX, state.dragAimY - targetAimY) <= 0.14f
            } else {
                true
            }

            val cueBonusOk = when (state.selectedPattern) {
                MovementPattern.CROUCH, MovementPattern.JUMP ->
                    if (cueOpportunities == 0) true else cueSatisfiedThisWindow || !cueNow
                else -> true
            }

            val sampleHit = isOnPath && dragAimOnTarget && cueBonusOk
            val nextSamples = state.totalSamples + 1
            val nextOnTrack = state.onTrackSamples + if (sampleHit) 1 else 0
            val baseAcc = TrainingMath.computeAccuracyPercent(nextOnTrack, nextSamples)
            val blendedAcc = if ((state.selectedPattern == MovementPattern.CROUCH || state.selectedPattern == MovementPattern.JUMP) && cueOpportunities > 0) {
                val cueAcc = (cueHits.toFloat() / cueOpportunities.toFloat()) * 100f
                (baseAcc * 0.65f + cueAcc * 0.35f).coerceIn(0f, 100f)
            } else {
                baseAcc
            }

            val trail = (state.pathTrail + TrailPoint(nextX, nextY)).takeLast(45)

            state.copy(
                remainingSec = remainingSec,
                playerX = nextX,
                playerY = nextY,
                guideX = gx,
                guideY = gy,
                jumpElevation = nextJump,
                actionCueActive = cueNow,
                actionCueLabel = cueLabel,
                pathTrail = trail,
                totalSamples = nextSamples,
                onTrackSamples = nextOnTrack,
                movementAccuracyPercent = blendedAcc
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        drillJob?.cancel()
    }
}
