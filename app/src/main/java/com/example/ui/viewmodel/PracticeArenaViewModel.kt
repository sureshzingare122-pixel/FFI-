package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AppPreferences
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.model.SensitivitySettings
import com.example.data.model.TrainingMode
import com.example.data.model.TrainingSession
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SessionPhase {
    IDLE,
    RUNNING,
    COMPLETED
}

data class ArenaTarget(
    val id: Long,
    val xNorm: Float,
    val yNorm: Float,
    val vxNorm: Float,
    val vyNorm: Float,
    val radiusNorm: Float,
    val spawnedAtMs: Long
)

data class HitFeedbackBurst(
    val id: Long,
    val xNorm: Float,
    val yNorm: Float,
    val isHit: Boolean,
    val isCenterZone: Boolean,
    val label: String,
    val createdAtMs: Long
)

data class PracticeArenaUiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val totalDurationSec: Int = 45,
    val remainingSec: Int = 45,
    val crosshairX: Float = 0.50f,
    val crosshairY: Float = 0.42f,
    val joystickInputX: Float = 0f,
    val joystickInputY: Float = 0f,
    val targets: List<ArenaTarget> = emptyList(),
    val feedbackBursts: List<HitFeedbackBurst> = emptyList(),
    val score: Int = 0,
    val hits: Int = 0,
    val misses: Int = 0,
    val centerHits: Int = 0,
    val reactionTimesMs: List<Long> = emptyList(),
    val lastShotCenterZone: Boolean = false,
    val hitFlashUntilMs: Long = 0L,
    val preferences: AppPreferences = AppPreferences(),
    val sensitivity: SensitivitySettings = SensitivitySettings(),
    val activeDeadZone: Float = 0.08f,
    val activeProfileName: String = "RUSH",
    val sessionSaved: Boolean = false
) {
    val totalAttempts: Int get() = hits + misses
    val accuracyPercent: Float get() = TrainingMath.computeAccuracyPercent(hits, totalAttempts)
    val centerZoneAccuracyPercent: Float get() = TrainingMath.computeAccuracyPercent(centerHits, hits)
    val avgReactionTimeMs: Float
        get() = if (reactionTimesMs.isEmpty()) 0f else reactionTimesMs.average().toFloat()
}

sealed interface PracticeFeedbackEvent {
    data class Hit(val centerZone: Boolean) : PracticeFeedbackEvent
    data object Miss : PracticeFeedbackEvent
    data object SessionCompleted : PracticeFeedbackEvent
}

class PracticeArenaViewModel(
    private val repository: TrainingRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeArenaUiState())
    val uiState: StateFlow<PracticeArenaUiState> = _uiState.asStateFlow()

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<PracticeFeedbackEvent>(extraBufferCapacity = 8)
    val events = _events

    private var loopJob: Job? = null
    private var currentSessionKey: String = UUID.randomUUID().toString()
    private var sessionStartElapsedMs: Long = 0L
    private var nextTargetId: Long = 1L
    private var nextBurstId: Long = 1L

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferencesFlow,
                repository.sensitivitySettingsFlow,
                repository.profilesFlow
            ) { prefs, sens, profiles ->
                Triple(prefs, sens, profiles)
            }.collect { (prefs, sens, profiles) ->
                val activeProfile = profiles.firstOrNull { it.profileId == prefs.activeMovementProfileId }
                    ?: profiles.firstOrNull()
                _uiState.update { state ->
                    val updatedRemaining = if (state.phase == SessionPhase.IDLE) {
                        prefs.practiceDurationSeconds
                    } else {
                        state.remainingSec
                    }
                    state.copy(
                        preferences = prefs,
                        sensitivity = sens,
                        totalDurationSec = prefs.practiceDurationSeconds,
                        remainingSec = updatedRemaining,
                        activeDeadZone = activeProfile?.joystickDeadZone ?: 0.08f,
                        activeProfileName = activeProfile?.displayName ?: prefs.activeMovementProfileId
                    )
                }
            }
        }
        spawnInitialTargets()
    }

    private fun spawnInitialTargets() {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(
                targets = List(3) { createRandomTarget(now) }
            )
        }
    }

    fun startSession() {
        loopJob?.cancel()
        currentSessionKey = UUID.randomUUID().toString()
        sessionStartElapsedMs = System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val duration = _uiState.value.preferences.practiceDurationSeconds

        _uiState.update { state ->
            state.copy(
                phase = SessionPhase.RUNNING,
                totalDurationSec = duration,
                remainingSec = duration,
                crosshairX = 0.50f,
                crosshairY = 0.42f,
                score = 0,
                hits = 0,
                misses = 0,
                centerHits = 0,
                reactionTimesMs = emptyList(),
                feedbackBursts = emptyList(),
                targets = List(3) { createRandomTarget(now) },
                sessionSaved = false
            )
        }

        loopJob = viewModelScope.launch {
            var lastFrameMs = System.currentTimeMillis()
            while (_uiState.value.phase == SessionPhase.RUNNING) {
                delay(16L) // ~60 FPS simulation tick
                val currentMs = System.currentTimeMillis()
                val dtSec = ((currentMs - lastFrameMs).coerceIn(1L, 50L)) / 1000f
                lastFrameMs = currentMs

                val elapsedSec = ((currentMs - sessionStartElapsedMs) / 1000L).toInt()
                val remaining = (duration - elapsedSec).coerceAtLeast(0)

                tickFrame(dtSec, currentMs, remaining)

                if (remaining <= 0) {
                    completeSession()
                    break
                }
            }
        }
    }

    fun resetSession() {
        loopJob?.cancel()
        currentSessionKey = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val duration = _uiState.value.preferences.practiceDurationSeconds
        _uiState.update { state ->
            state.copy(
                phase = SessionPhase.IDLE,
                remainingSec = duration,
                crosshairX = 0.50f,
                crosshairY = 0.42f,
                joystickInputX = 0f,
                joystickInputY = 0f,
                score = 0,
                hits = 0,
                misses = 0,
                centerHits = 0,
                reactionTimesMs = emptyList(),
                feedbackBursts = emptyList(),
                targets = List(3) { createRandomTarget(now) },
                sessionSaved = false
            )
        }
    }

    fun finishEarlyAndSave() {
        if (_uiState.value.phase == SessionPhase.RUNNING) {
            loopJob?.cancel()
            completeSession()
        }
    }

    fun onJoystickMove(rawX: Float, rawY: Float) {
        _uiState.update { it.copy(joystickInputX = rawX, joystickInputY = rawY) }
    }

    fun onAimDrag(deltaXNorm: Float, deltaYNorm: Float) {
        _uiState.update { state ->
            val sens = state.sensitivity.crosshairSensitivity
            val newX = (state.crosshairX + deltaXNorm * sens).coerceIn(0.05f, 0.95f)
            val newY = (state.crosshairY + deltaYNorm * sens).coerceIn(0.08f, 0.86f)
            state.copy(crosshairX = newX, crosshairY = newY)
        }
    }

    fun fireAction() {
        val state = _uiState.value
        if (state.phase == SessionPhase.IDLE) {
            startSession()
        }
        val current = _uiState.value
        if (current.phase != SessionPhase.RUNNING) return

        val now = System.currentTimeMillis()
        var hitTarget: ArenaTarget? = null
        var bestDist = Float.MAX_VALUE
        var isCenter = false

        for (target in current.targets) {
            val eval = TrainingMath.detectTargetHit(
                crosshairX = current.crosshairX,
                crosshairY = current.crosshairY,
                targetX = target.xNorm,
                targetY = target.yNorm,
                targetRadius = target.radiusNorm
            )
            if (eval.isHit && eval.distance < bestDist) {
                bestDist = eval.distance
                hitTarget = target
                isCenter = eval.isCenterZone
            }
        }

        if (hitTarget != null) {
            val reactionMs = (now - hitTarget.spawnedAtMs).coerceIn(80L, 4000L)
            val reactionBonus = ((900L - reactionMs).coerceAtLeast(0L) / 15L).toInt()
            val points = (if (isCenter) 150 else 100) + reactionBonus
            val burst = HitFeedbackBurst(
                id = nextBurstId++,
                xNorm = hitTarget.xNorm,
                yNorm = hitTarget.yNorm,
                isHit = true,
                isCenterZone = isCenter,
                label = if (isCenter) "+$points CORE" else "+$points HIT",
                createdAtMs = now
            )

            val updatedTargets = current.targets.filterNot { it.id == hitTarget.id } + createRandomTarget(now)

            _uiState.update { s ->
                s.copy(
                    targets = updatedTargets,
                    feedbackBursts = (s.feedbackBursts + burst).takeLast(6),
                    score = s.score + points,
                    hits = s.hits + 1,
                    centerHits = s.centerHits + if (isCenter) 1 else 0,
                    reactionTimesMs = s.reactionTimesMs + reactionMs,
                    lastShotCenterZone = isCenter,
                    hitFlashUntilMs = now + 140L
                )
            }
            _events.tryEmit(PracticeFeedbackEvent.Hit(isCenter))
        } else {
            val burst = HitFeedbackBurst(
                id = nextBurstId++,
                xNorm = current.crosshairX,
                yNorm = current.crosshairY,
                isHit = false,
                isCenterZone = false,
                label = "MISS",
                createdAtMs = now
            )
            _uiState.update { s ->
                s.copy(
                    feedbackBursts = (s.feedbackBursts + burst).takeLast(6),
                    misses = s.misses + 1,
                    score = (s.score - 15).coerceAtLeast(0),
                    lastShotCenterZone = false
                )
            }
            _events.tryEmit(PracticeFeedbackEvent.Miss)
        }
    }

    private fun tickFrame(dtSec: Float, nowMs: Long, remainingSec: Int) {
        _uiState.update { state ->
            val joystickVec = TrainingMath.applyJoystickResponse(
                rawX = state.joystickInputX,
                rawY = state.joystickInputY,
                deadZone = state.activeDeadZone,
                sensitivity = state.sensitivity.joystickResponse
            )

            val nextCrosshairX = (state.crosshairX + joystickVec.x * dtSec * 0.42f).coerceIn(0.05f, 0.95f)
            val nextCrosshairY = (state.crosshairY + joystickVec.y * dtSec * 0.42f).coerceIn(0.08f, 0.86f)

            // Keep targets safely inside visible arena bounds
            val minX = 0.10f
            val maxX = 0.90f
            val minY = 0.12f
            val maxY = 0.68f

            val movedTargets = state.targets.map { t ->
                var nx = t.xNorm + t.vxNorm * dtSec
                var ny = t.yNorm + t.vyNorm * dtSec
                var nvx = t.vxNorm
                var nvy = t.vyNorm

                if (nx - t.radiusNorm < minX) {
                    nx = minX + t.radiusNorm
                    nvx = kotlin.math.abs(nvx)
                } else if (nx + t.radiusNorm > maxX) {
                    nx = maxX - t.radiusNorm
                    nvx = -kotlin.math.abs(nvx)
                }
                if (ny - t.radiusNorm < minY) {
                    ny = minY + t.radiusNorm
                    nvy = kotlin.math.abs(nvy)
                } else if (ny + t.radiusNorm > maxY) {
                    ny = maxY - t.radiusNorm
                    nvy = -kotlin.math.abs(nvy)
                }

                t.copy(xNorm = nx, yNorm = ny, vxNorm = nvx, vyNorm = nvy)
            }

            val activeBursts = state.feedbackBursts.filter { nowMs - it.createdAtMs < 650L }

            state.copy(
                remainingSec = remainingSec,
                crosshairX = nextCrosshairX,
                crosshairY = nextCrosshairY,
                targets = movedTargets,
                feedbackBursts = activeBursts
            )
        }
    }

    private fun completeSession() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val elapsedDuration = (now - sessionStartElapsedMs).coerceAtLeast(1000L)

        _uiState.update { it.copy(phase = SessionPhase.COMPLETED, remainingSec = 0) }
        _events.tryEmit(PracticeFeedbackEvent.SessionCompleted)

        if (state.totalAttempts > 0 && !state.sessionSaved) {
            val session = TrainingSession(
                sessionKey = currentSessionKey,
                mode = TrainingMode.PRACTICE_ARENA.name,
                subMode = "FREE_RANGE",
                profileName = state.activeProfileName,
                timestampMs = now,
                durationMs = elapsedDuration,
                totalAttempts = state.totalAttempts,
                successfulHits = state.hits,
                missedTargets = state.misses,
                accuracyPercent = state.accuracyPercent,
                centerZoneAccuracyPercent = state.centerZoneAccuracyPercent,
                avgReactionTimeMs = state.avgReactionTimeMs,
                flickAccuracyPercent = state.accuracyPercent,
                score = state.score,
                performanceRatio = TrainingMath.computePerformanceRatio(
                    accuracyPercent = state.accuracyPercent,
                    avgReactionTimeMs = state.avgReactionTimeMs,
                    centerZoneAccuracyPercent = state.centerZoneAccuracyPercent
                )
            )
            viewModelScope.launch {
                val inserted = repository.recordSession(session)
                if (inserted) {
                    _uiState.update { it.copy(sessionSaved = true) }
                }
            }
        }
    }

    private fun createRandomTarget(nowMs: Long): ArenaTarget {
        val radius = Random.nextFloat() * 0.020f + 0.052f
        val x = Random.nextFloat() * 0.70f + 0.15f
        val y = Random.nextFloat() * 0.44f + 0.16f
        val angle = Random.nextFloat() * (2f * Math.PI.toFloat())
        val speed = Random.nextFloat() * 0.14f + 0.12f
        return ArenaTarget(
            id = nextTargetId++,
            xNorm = x,
            yNorm = y,
            vxNorm = cos(angle) * speed,
            vyNorm = sin(angle) * speed,
            radiusNorm = radius,
            spawnedAtMs = nowMs
        )
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}
