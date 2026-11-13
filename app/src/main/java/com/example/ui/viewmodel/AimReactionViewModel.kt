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
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.random.Random

enum class AimDifficulty(
    val label: String,
    val targetRadiusNorm: Float,
    val moveSpeed: Float,
    val maxTargetLifetimeMs: Long,
    val targetCount: Int
) {
    EASY("EASY", 0.080f, 0.08f, 4500L, 2),
    MEDIUM("MEDIUM", 0.064f, 0.14f, 3200L, 3),
    HARD("HARD", 0.050f, 0.21f, 2400L, 3),
    PRO("PRO", 0.038f, 0.29f, 1800L, 4)
}

data class AimReactionUiState(
    val difficulty: AimDifficulty = AimDifficulty.MEDIUM,
    val phase: SessionPhase = SessionPhase.IDLE,
    val durationSec: Int = 30,
    val remainingSec: Int = 30,
    val crosshairX: Float = 0.50f,
    val crosshairY: Float = 0.45f,
    val targets: List<ArenaTarget> = emptyList(),
    val bursts: List<HitFeedbackBurst> = emptyList(),
    val totalAttempts: Int = 0,
    val successfulHits: Int = 0,
    val missedTargets: Int = 0,
    val centerZoneHits: Int = 0,
    val flickAttempts: Int = 0,
    val flickSuccesses: Int = 0,
    val reactionTimesMs: List<Long> = emptyList(),
    val totalScore: Int = 0,
    val hitFlashUntilMs: Long = 0L,
    val preferences: AppPreferences = AppPreferences(),
    val sensitivity: SensitivitySettings = SensitivitySettings(),
    val sessionSaved: Boolean = false
) {
    val accuracyPercent: Float get() = TrainingMath.computeAccuracyPercent(successfulHits, totalAttempts)
    val centerZoneAccuracyPercent: Float get() = TrainingMath.computeAccuracyPercent(centerZoneHits, successfulHits)
    val flickAccuracyPercent: Float get() = TrainingMath.computeAccuracyPercent(flickSuccesses, flickAttempts)
    val avgReactionTimeMs: Float
        get() = if (reactionTimesMs.isEmpty()) 0f else reactionTimesMs.average().toFloat()
}

class AimReactionViewModel(
    private val repository: TrainingRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AimReactionUiState())
    val uiState: StateFlow<AimReactionUiState> = _uiState.asStateFlow()

    val events = MutableSharedFlow<PracticeFeedbackEvent>(extraBufferCapacity = 8)

    private var loopJob: Job? = null
    private var sessionKey: String = UUID.randomUUID().toString()
    private var startTimeMs: Long = 0L
    private var lastShotOriginX: Float = 0.5f
    private var lastShotOriginY: Float = 0.45f
    private var nextTargetId: Long = 100L
    private var nextBurstId: Long = 100L

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferencesFlow,
                repository.sensitivitySettingsFlow
            ) { prefs, sens -> prefs to sens }.collect { (prefs, sens) ->
                _uiState.update { it.copy(preferences = prefs, sensitivity = sens) }
            }
        }
        resetChallenge()
    }

    fun selectDifficulty(difficulty: AimDifficulty) {
        loopJob?.cancel()
        _uiState.update { it.copy(difficulty = difficulty) }
        resetChallenge()
    }

    fun startChallenge() {
        loopJob?.cancel()
        sessionKey = UUID.randomUUID().toString()
        startTimeMs = System.currentTimeMillis()
        lastShotOriginX = 0.5f
        lastShotOriginY = 0.45f
        val diff = _uiState.value.difficulty

        _uiState.update {
            it.copy(
                phase = SessionPhase.RUNNING,
                remainingSec = it.durationSec,
                crosshairX = 0.5f,
                crosshairY = 0.45f,
                targets = List(diff.targetCount) { spawnTarget(diff, startTimeMs) },
                bursts = emptyList(),
                totalAttempts = 0,
                successfulHits = 0,
                missedTargets = 0,
                centerZoneHits = 0,
                flickAttempts = 0,
                flickSuccesses = 0,
                reactionTimesMs = emptyList(),
                totalScore = 0,
                sessionSaved = false
            )
        }

        loopJob = viewModelScope.launch {
            var lastFrame = System.currentTimeMillis()
            while (_uiState.value.phase == SessionPhase.RUNNING) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = ((now - lastFrame).coerceIn(1L, 50L)) / 1000f
                lastFrame = now
                val elapsed = ((now - startTimeMs) / 1000L).toInt()
                val remaining = (_uiState.value.durationSec - elapsed).coerceAtLeast(0)

                stepArena(dt, now, remaining)

                if (remaining <= 0) {
                    completeChallenge()
                    break
                }
            }
        }
    }

    fun resetChallenge() {
        loopJob?.cancel()
        val now = System.currentTimeMillis()
        val diff = _uiState.value.difficulty
        _uiState.update {
            it.copy(
                phase = SessionPhase.IDLE,
                remainingSec = it.durationSec,
                crosshairX = 0.5f,
                crosshairY = 0.45f,
                targets = List(diff.targetCount) { spawnTarget(diff, now) },
                bursts = emptyList(),
                totalAttempts = 0,
                successfulHits = 0,
                missedTargets = 0,
                centerZoneHits = 0,
                flickAttempts = 0,
                flickSuccesses = 0,
                reactionTimesMs = emptyList(),
                totalScore = 0,
                sessionSaved = false
            )
        }
    }

    fun onAimDrag(deltaXNorm: Float, deltaYNorm: Float) {
        _uiState.update { state ->
            val sens = state.sensitivity.crosshairSensitivity
            state.copy(
                crosshairX = (state.crosshairX + deltaXNorm * sens).coerceIn(0.05f, 0.95f),
                crosshairY = (state.crosshairY + deltaYNorm * sens).coerceIn(0.08f, 0.88f)
            )
        }
    }

    fun onTapTargetFire(tapXNorm: Float, tapYNorm: Float) {
        if (_uiState.value.phase == SessionPhase.IDLE) {
            startChallenge()
        }
        val state = _uiState.value
        if (state.phase != SessionPhase.RUNNING) return

        // Move crosshair toward tap point for direct tap-flick mode, or evaluate at tap
        _uiState.update {
            it.copy(
                crosshairX = tapXNorm.coerceIn(0.05f, 0.95f),
                crosshairY = tapYNorm.coerceIn(0.08f, 0.88f)
            )
        }
        fireAtCurrentCrosshair()
    }

    fun fireAtCurrentCrosshair() {
        if (_uiState.value.phase == SessionPhase.IDLE) {
            startChallenge()
        }
        val state = _uiState.value
        if (state.phase != SessionPhase.RUNNING) return

        val now = System.currentTimeMillis()
        val diff = state.difficulty
        val flickDistance = hypot(state.crosshairX - lastShotOriginX, state.crosshairY - lastShotOriginY)
        val isFlick = flickDistance >= 0.14f

        var hitTarget: ArenaTarget? = null
        var minDist = Float.MAX_VALUE
        var isCenter = false

        for (t in state.targets) {
            val eval = TrainingMath.detectTargetHit(
                crosshairX = state.crosshairX,
                crosshairY = state.crosshairY,
                targetX = t.xNorm,
                targetY = t.yNorm,
                targetRadius = t.radiusNorm
            )
            if (eval.isHit && eval.distance < minDist) {
                minDist = eval.distance
                hitTarget = t
                isCenter = eval.isCenterZone
            }
        }

        lastShotOriginX = state.crosshairX
        lastShotOriginY = state.crosshairY

        if (hitTarget != null) {
            val reactionMs = (now - hitTarget.spawnedAtMs).coerceIn(80L, 4000L)
            val diffMultiplier = when (diff) {
                AimDifficulty.EASY -> 1.0f
                AimDifficulty.MEDIUM -> 1.25f
                AimDifficulty.HARD -> 1.5f
                AimDifficulty.PRO -> 2.0f
            }
            val speedBonus = ((850L - reactionMs).coerceAtLeast(0L) / 10L).toInt()
            val pts = (((if (isCenter) 160 else 100) + speedBonus) * diffMultiplier).toInt()

            val burst = HitFeedbackBurst(
                id = nextBurstId++,
                xNorm = hitTarget.xNorm,
                yNorm = hitTarget.yNorm,
                isHit = true,
                isCenterZone = isCenter,
                label = if (isCenter) "+$pts BULLSEYE" else "+$pts",
                createdAtMs = now
            )

            val refreshedTargets = state.targets.filterNot { it.id == hitTarget.id } + spawnTarget(diff, now)

            _uiState.update { s ->
                s.copy(
                    targets = refreshedTargets,
                    bursts = (s.bursts + burst).takeLast(6),
                    totalAttempts = s.totalAttempts + 1,
                    successfulHits = s.successfulHits + 1,
                    centerZoneHits = s.centerZoneHits + if (isCenter) 1 else 0,
                    flickAttempts = s.flickAttempts + if (isFlick) 1 else 0,
                    flickSuccesses = s.flickSuccesses + if (isFlick) 1 else 0,
                    reactionTimesMs = s.reactionTimesMs + reactionMs,
                    totalScore = s.totalScore + pts,
                    hitFlashUntilMs = now + 130L
                )
            }
            events.tryEmit(PracticeFeedbackEvent.Hit(isCenter))
        } else {
            val burst = HitFeedbackBurst(
                id = nextBurstId++,
                xNorm = state.crosshairX,
                yNorm = state.crosshairY,
                isHit = false,
                isCenterZone = false,
                label = "MISS",
                createdAtMs = now
            )
            _uiState.update { s ->
                s.copy(
                    bursts = (s.bursts + burst).takeLast(6),
                    totalAttempts = s.totalAttempts + 1,
                    missedTargets = s.missedTargets + 1,
                    flickAttempts = s.flickAttempts + if (isFlick) 1 else 0,
                    totalScore = (s.totalScore - 20).coerceAtLeast(0)
                )
            }
            events.tryEmit(PracticeFeedbackEvent.Miss)
        }
    }

    private fun stepArena(dt: Float, now: Long, remainingSec: Int) {
        _uiState.update { state ->
            val diff = state.difficulty
            var expiredCount = 0
            val surviving = mutableListOf<ArenaTarget>()

            for (t in state.targets) {
                if (now - t.spawnedAtMs > diff.maxTargetLifetimeMs) {
                    expiredCount++
                } else {
                    var nx = t.xNorm + t.vxNorm * dt
                    var ny = t.yNorm + t.vyNorm * dt
                    var nvx = t.vxNorm
                    var nvy = t.vyNorm

                    if (nx - t.radiusNorm < 0.08f) {
                        nx = 0.08f + t.radiusNorm
                        nvx = kotlin.math.abs(nvx)
                    } else if (nx + t.radiusNorm > 0.92f) {
                        nx = 0.92f - t.radiusNorm
                        nvx = -kotlin.math.abs(nvx)
                    }
                    if (ny - t.radiusNorm < 0.12f) {
                        ny = 0.12f + t.radiusNorm
                        nvy = kotlin.math.abs(nvy)
                    } else if (ny + t.radiusNorm > 0.84f) {
                        ny = 0.84f - t.radiusNorm
                        nvy = -kotlin.math.abs(nvy)
                    }
                    surviving.add(t.copy(xNorm = nx, yNorm = ny, vxNorm = nvx, vyNorm = nvy))
                }
            }

            repeat(expiredCount) {
                surviving.add(spawnTarget(diff, now))
            }

            state.copy(
                remainingSec = remainingSec,
                targets = surviving,
                totalAttempts = state.totalAttempts + expiredCount,
                missedTargets = state.missedTargets + expiredCount,
                bursts = state.bursts.filter { now - it.createdAtMs < 600L }
            )
        }
    }

    private fun completeChallenge() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val elapsedMs = (now - startTimeMs).coerceAtLeast(1000L)

        _uiState.update { it.copy(phase = SessionPhase.COMPLETED, remainingSec = 0) }
        events.tryEmit(PracticeFeedbackEvent.SessionCompleted)

        if (state.totalAttempts > 0 && !state.sessionSaved) {
            val session = TrainingSession(
                sessionKey = sessionKey,
                mode = TrainingMode.AIM_REACTION.name,
                subMode = state.difficulty.name,
                profileName = state.preferences.activeMovementProfileId,
                timestampMs = now,
                durationMs = elapsedMs,
                totalAttempts = state.totalAttempts,
                successfulHits = state.successfulHits,
                missedTargets = state.missedTargets,
                accuracyPercent = state.accuracyPercent,
                centerZoneAccuracyPercent = state.centerZoneAccuracyPercent,
                avgReactionTimeMs = state.avgReactionTimeMs,
                flickAccuracyPercent = state.flickAccuracyPercent,
                score = state.totalScore,
                performanceRatio = TrainingMath.computePerformanceRatio(
                    accuracyPercent = state.accuracyPercent,
                    avgReactionTimeMs = state.avgReactionTimeMs,
                    centerZoneAccuracyPercent = state.centerZoneAccuracyPercent
                )
            )
            viewModelScope.launch {
                if (repository.recordSession(session)) {
                    _uiState.update { it.copy(sessionSaved = true) }
                }
            }
        }
    }

    private fun spawnTarget(difficulty: AimDifficulty, nowMs: Long): ArenaTarget {
        val x = Random.nextFloat() * 0.72f + 0.14f
        val y = Random.nextFloat() * 0.56f + 0.18f
        val angle = Random.nextFloat() * (2f * PI.toFloat())
        val speed = difficulty.moveSpeed * (0.85f + Random.nextFloat() * 0.3f)
        return ArenaTarget(
            id = nextTargetId++,
            xNorm = x,
            yNorm = y,
            vxNorm = cos(angle) * speed,
            vyNorm = sin(angle) * speed,
            radiusNorm = difficulty.targetRadiusNorm,
            spawnedAtMs = nowMs
        )
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}
