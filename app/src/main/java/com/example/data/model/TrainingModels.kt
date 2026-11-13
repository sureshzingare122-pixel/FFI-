package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrainingMode {
    PRACTICE_ARENA,
    MOVEMENT_TRAINER,
    AIM_REACTION,
    SENSITIVITY_CALIBRATION
}

@Entity(
    tableName = "training_sessions",
    indices = [Index(value = ["sessionKey"], unique = true)]
)
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionKey: String,
    val mode: String,
    val subMode: String,
    val profileName: String,
    val timestampMs: Long,
    val durationMs: Long,
    val totalAttempts: Int,
    val successfulHits: Int,
    val missedTargets: Int,
    val accuracyPercent: Float,
    val centerZoneAccuracyPercent: Float,
    val avgReactionTimeMs: Float,
    val flickAccuracyPercent: Float,
    val score: Int,
    val performanceRatio: Float
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val profileId: String,
    val displayName: String,
    val description: String,
    val joystickDeadZone: Float,
    val joystickSensitivity: Float,
    val movementAcceleration: Float,
    val responseCurveExponent: Float,
    val dragAimSensitivity: Float,
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class SensitivityPreset {
    LOW,
    MEDIUM,
    HIGH,
    CUSTOM
}

@Entity(tableName = "sensitivity_settings")
data class SensitivitySettings(
    @PrimaryKey val id: Int = 1,
    val activePreset: String = SensitivityPreset.MEDIUM.name,
    val crosshairSensitivity: Float = 1.15f,
    val joystickResponse: Float = 1.10f,
    val recommendedCrosshairSensitivity: Float? = null,
    val recommendedJoystickResponse: Float? = null,
    val lastCalibrationScore: Int? = null,
    val lastCalibratedAtMs: Long? = null
)

data class TrainingStatistics(
    val totalSessions: Int = 0,
    val bestScore: Int = 0,
    val averageScore: Float = 0f,
    val overallAccuracyPercent: Float = 0f,
    val centerZoneAccuracyPercent: Float = 0f,
    val averageReactionTimeMs: Float = 0f,
    val totalTrainingDurationMs: Long = 0L,
    val simulatedPerformanceRatio: Float = 0f,
    val accuracyHistory: List<SessionMetricPoint> = emptyList(),
    val reactionTimeHistory: List<SessionMetricPoint> = emptyList()
)

data class SessionMetricPoint(
    val sessionIndex: Int,
    val timestampMs: Long,
    val value: Float,
    val label: String
)

enum class CrosshairStyle(val displayName: String) {
    TACTICAL_DOT("Tactical Dot"),
    CLASSIC_CROSS("Classic Cross"),
    OPEN_BRACKET("Open Bracket")
}
