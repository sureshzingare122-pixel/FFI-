package com.example.engine

import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

data class JoystickVector(
    val x: Float,
    val y: Float,
    val rawMagnitude: Float,
    val curvedMagnitude: Float
)

data class HitEvaluation(
    val isHit: Boolean,
    val isCenterZone: Boolean,
    val distance: Float
)

data class CalibrationRecommendation(
    val calibrationScore: Int,
    val recommendedCrosshairSensitivity: Float,
    val recommendedJoystickResponse: Float,
    val diagnosis: String
)

object TrainingMath {

    fun applyJoystickResponse(
        rawX: Float,
        rawY: Float,
        deadZone: Float,
        sensitivity: Float,
        acceleration: Float = 1.0f,
        responseCurveExponent: Float = 1.0f
    ): JoystickVector {
        val clampedDeadZone = deadZone.coerceIn(0.01f, 0.50f)
        val rawMag = hypot(rawX, rawY).coerceIn(0f, 1f)
        if (rawMag <= clampedDeadZone || rawMag == 0f) {
            return JoystickVector(0f, 0f, rawMag, 0f)
        }
        val remapped = ((rawMag - clampedDeadZone) / (1f - clampedDeadZone)).coerceIn(0f, 1f)
        val curved = remapped.pow(responseCurveExponent.coerceIn(0.5f, 3.0f))
        val gain = curved * sensitivity.coerceIn(0.3f, 3.0f) * acceleration.coerceIn(0.5f, 2.5f)
        val dirX = rawX / rawMag
        val dirY = rawY / rawMag
        return JoystickVector(
            x = dirX * gain,
            y = dirY * gain,
            rawMagnitude = rawMag,
            curvedMagnitude = curved
        )
    }

    fun detectTargetHit(
        crosshairX: Float,
        crosshairY: Float,
        targetX: Float,
        targetY: Float,
        targetRadius: Float
    ): HitEvaluation {
        val dx = crosshairX - targetX
        val dy = crosshairY - targetY
        val dist = sqrt(dx * dx + dy * dy)
        val isHit = dist <= targetRadius
        val isCenter = dist <= targetRadius * 0.40f
        return HitEvaluation(
            isHit = isHit,
            isCenterZone = isCenter,
            distance = dist
        )
    }

    fun computeAccuracyPercent(hits: Int, attempts: Int): Float {
        if (attempts <= 0) return 0f
        return ((hits.toFloat() / attempts.toFloat()) * 100f).coerceIn(0f, 100f)
    }

    fun computePerformanceRatio(
        accuracyPercent: Float,
        avgReactionTimeMs: Float,
        centerZoneAccuracyPercent: Float
    ): Float {
        if (accuracyPercent <= 0f && centerZoneAccuracyPercent <= 0f) return 0f
        val accScore = (accuracyPercent / 100f).coerceIn(0f, 1f)
        val centerScore = (centerZoneAccuracyPercent / 100f).coerceIn(0f, 1f)
        val reactionScore = if (avgReactionTimeMs > 0f) {
            (420f / avgReactionTimeMs.coerceIn(140f, 1500f)).coerceIn(0.25f, 1.5f)
        } else {
            0.75f
        }
        return ((accScore * 0.55f + centerScore * 0.25f) * 2.0f * (0.6f + 0.4f * reactionScore))
            .coerceIn(0f, 3.0f)
    }

    fun computeCalibrationRecommendation(
        currentCrosshairSens: Float,
        currentJoystickResp: Float,
        hits: Int,
        totalTargets: Int,
        overshootCount: Int,
        undershootCount: Int,
        avgReactionMs: Float
    ): CalibrationRecommendation {
        val accuracy = computeAccuracyPercent(hits, totalTargets.coerceAtLeast(1))
        val speedFactor = if (avgReactionMs > 0f) {
            (600f / avgReactionMs.coerceIn(180f, 1800f)).coerceIn(0.35f, 1.25f)
        } else {
            0.5f
        }
        val overshootPenalty = (overshootCount * 4).coerceAtMost(24)
        val rawScore = (accuracy * 0.70f + speedFactor * 30f - overshootPenalty).toInt().coerceIn(0, 100)

        var recCrosshair = currentCrosshairSens
        var recJoystick = currentJoystickResp
        val diagnosis = when {
            overshootCount > undershootCount + 1 -> {
                recCrosshair = (currentCrosshairSens * 0.86f).coerceIn(0.50f, 2.50f)
                recJoystick = (currentJoystickResp * 0.92f).coerceIn(0.60f, 2.20f)
                "Frequent crosshair overshoot detected — lowering crosshair sensitivity improves target lock stability."
            }
            undershootCount > overshootCount + 1 || (avgReactionMs > 780f && accuracy < 75f) -> {
                recCrosshair = (currentCrosshairSens * 1.15f).coerceIn(0.50f, 2.50f)
                recJoystick = (currentJoystickResp * 1.08f).coerceIn(0.60f, 2.20f)
                "Slow target acquisition / undershoot detected — boosting sensitivity speeds up flick transitions."
            }
            accuracy >= 85f && avgReactionMs in 150f..520f -> {
                "Optimal tracking balance achieved. Current simulator settings are well matched to your control tempo."
            }
            else -> {
                recCrosshair = (currentCrosshairSens * 0.97f).coerceIn(0.50f, 2.50f)
                "Minor precision tuning suggested to tighten center-zone consistency."
            }
        }

        return CalibrationRecommendation(
            calibrationScore = rawScore,
            recommendedCrosshairSensitivity = recCrosshair,
            recommendedJoystickResponse = recJoystick,
            diagnosis = diagnosis
        )
    }
}
