package com.example

import com.example.data.model.TrainingSession
import com.example.data.repository.TrainingRepository
import com.example.engine.TrainingMath
import com.example.util.AnalyticsExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineUnitTest {

    @Test
    fun `joystick dead zone filters micro drift and scales outside dead zone`() {
        val inside = TrainingMath.applyJoystickResponse(
            rawX = 0.05f,
            rawY = 0.04f,
            deadZone = 0.10f,
            sensitivity = 1.5f
        )
        assertEquals(0f, inside.x, 0.0001f)
        assertEquals(0f, inside.y, 0.0001f)

        val outside = TrainingMath.applyJoystickResponse(
            rawX = 1.0f,
            rawY = 0.0f,
            deadZone = 0.10f,
            sensitivity = 1.5f,
            acceleration = 1.0f,
            responseCurveExponent = 1.0f
        )
        assertEquals(1.5f, outside.x, 0.01f)
        assertEquals(0f, outside.y, 0.0001f)
    }

    @Test
    fun `target hit detection distinguishes center zone vs outer ring vs miss`() {
        val centerHit = TrainingMath.detectTargetHit(
            crosshairX = 0.50f,
            crosshairY = 0.50f,
            targetX = 0.51f,
            targetY = 0.50f,
            targetRadius = 0.08f
        )
        assertTrue(centerHit.isHit)
        assertTrue(centerHit.isCenterZone)

        val outerHit = TrainingMath.detectTargetHit(
            crosshairX = 0.56f,
            crosshairY = 0.50f,
            targetX = 0.50f,
            targetY = 0.50f,
            targetRadius = 0.08f
        )
        assertTrue(outerHit.isHit)
        assertFalse(outerHit.isCenterZone)

        val miss = TrainingMath.detectTargetHit(
            crosshairX = 0.70f,
            crosshairY = 0.50f,
            targetX = 0.50f,
            targetY = 0.50f,
            targetRadius = 0.08f
        )
        assertFalse(miss.isHit)
        assertFalse(miss.isCenterZone)
    }

    @Test
    fun `calibration recommendation lowers sensitivity on frequent overshoots`() {
        val rec = TrainingMath.computeCalibrationRecommendation(
            currentCrosshairSens = 1.60f,
            currentJoystickResp = 1.30f,
            hits = 4,
            totalTargets = 6,
            overshootCount = 4,
            undershootCount = 0,
            avgReactionMs = 400f
        )
        assertTrue(rec.recommendedCrosshairSensitivity < 1.60f)
        assertTrue(rec.calibrationScore in 0..100)
    }

    @Test
    fun `statistics calculation handles empty and populated sessions accurately`() {
        val emptyStats = TrainingRepository.computeStatistics(emptyList())
        assertEquals(0, emptyStats.totalSessions)
        assertEquals(0, emptyStats.bestScore)
        assertEquals(0f, emptyStats.overallAccuracyPercent, 0.01f)

        val s1 = TrainingSession(
            id = 1,
            sessionKey = "k1",
            mode = "PRACTICE_ARENA",
            subMode = "FREE_RANGE",
            profileName = "RUSH",
            timestampMs = 1000L,
            durationMs = 30_000L,
            totalAttempts = 10,
            successfulHits = 8,
            missedTargets = 2,
            accuracyPercent = 80f,
            centerZoneAccuracyPercent = 50f,
            avgReactionTimeMs = 320f,
            flickAccuracyPercent = 80f,
            score = 1000,
            performanceRatio = 1.2f
        )
        val s2 = s1.copy(
            id = 2,
            sessionKey = "k2",
            timestampMs = 2000L,
            totalAttempts = 10,
            successfulHits = 6,
            missedTargets = 4,
            accuracyPercent = 60f,
            centerZoneAccuracyPercent = 30f,
            avgReactionTimeMs = 400f,
            score = 700,
            performanceRatio = 0.9f
        )
        val stats = TrainingRepository.computeStatistics(listOf(s1, s2))
        assertEquals(2, stats.totalSessions)
        assertEquals(1000, stats.bestScore)
        assertEquals(850f, stats.averageScore, 0.1f)
        assertEquals(70f, stats.overallAccuracyPercent, 0.1f)
        assertEquals(360f, stats.averageReactionTimeMs, 0.1f)
        assertEquals(60_000L, stats.totalTrainingDurationMs)

        val csv = AnalyticsExporter.buildCsvContent(listOf(s1, s2))
        assertTrue(csv.contains("session_key"))
        assertTrue(csv.contains("k1"))
        assertTrue(csv.contains("k2"))
    }
}
