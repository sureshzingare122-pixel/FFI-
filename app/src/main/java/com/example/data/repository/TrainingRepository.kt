package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.SensitivitySettingsDao
import com.example.data.local.dao.TrainingSessionDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import com.example.data.model.SessionMetricPoint
import com.example.data.model.TrainingSession
import com.example.data.model.TrainingStatistics
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainingRepository(
    private val sessionDao: TrainingSessionDao,
    private val profileDao: UserProfileDao,
    private val sensitivityDao: SensitivitySettingsDao
) {
    val allSessionsDesc: Flow<List<TrainingSession>> = sessionDao.observeAllSessions()

    val profilesFlow: Flow<List<UserProfile>> = profileDao.observeProfiles()

    val sensitivitySettingsFlow: Flow<SensitivitySettings> =
        sensitivityDao.observeSettings().map { it ?: AppDatabase.defaultSensitivitySettings() }

    val statisticsFlow: Flow<TrainingStatistics> =
        sessionDao.observeSessionsChronological().map { sessions ->
            computeStatistics(sessions)
        }

    suspend fun ensureSeedData() {
        profileDao.insertProfilesIgnore(AppDatabase.defaultProfiles())
        sensitivityDao.insertDefaultIgnore(AppDatabase.defaultSensitivitySettings())
    }

    suspend fun recordSession(session: TrainingSession): Boolean {
        val rowId = sessionDao.insertSession(session)
        return rowId != -1L
    }

    suspend fun deleteSession(id: Long) {
        sessionDao.deleteSessionById(id)
    }

    suspend fun clearAllSessions() {
        sessionDao.clearAllSessions()
    }

    suspend fun updateProfile(profile: UserProfile) {
        profileDao.upsertProfile(profile.copy(updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun resetProfilesToDefaults() {
        AppDatabase.defaultProfiles().forEach { defaultProfile ->
            profileDao.upsertProfile(defaultProfile)
        }
    }

    suspend fun updateSensitivitySettings(settings: SensitivitySettings) {
        sensitivityDao.upsertSettings(settings)
    }

    suspend fun applyPreset(preset: SensitivityPreset) {
        val current = sensitivityDao.getSettingsOnce() ?: AppDatabase.defaultSensitivitySettings()
        val updated = when (preset) {
            SensitivityPreset.LOW -> current.copy(
                activePreset = preset.name,
                crosshairSensitivity = 0.75f,
                joystickResponse = 0.85f
            )
            SensitivityPreset.MEDIUM -> current.copy(
                activePreset = preset.name,
                crosshairSensitivity = 1.15f,
                joystickResponse = 1.10f
            )
            SensitivityPreset.HIGH -> current.copy(
                activePreset = preset.name,
                crosshairSensitivity = 1.75f,
                joystickResponse = 1.55f
            )
            SensitivityPreset.CUSTOM -> current.copy(activePreset = preset.name)
        }
        sensitivityDao.upsertSettings(updated)
    }

    companion object {
        fun computeStatistics(sessions: List<TrainingSession>): TrainingStatistics {
            if (sessions.isEmpty()) {
                return TrainingStatistics()
            }

            val totalSessions = sessions.size
            val bestScore = sessions.maxOf { it.score }
            val averageScore = sessions.map { it.score }.average().toFloat()
            val totalDurationMs = sessions.sumOf { it.durationMs }

            val totalAttempts = sessions.sumOf { it.totalAttempts }
            val totalHits = sessions.sumOf { it.successfulHits }
            val overallAccuracy = if (totalAttempts > 0) {
                (totalHits.toFloat() / totalAttempts.toFloat()) * 100f
            } else {
                sessions.map { it.accuracyPercent }.average().toFloat()
            }

            val centerZoneAvg = sessions.map { it.centerZoneAccuracyPercent }.average().toFloat()

            val reactionSessions = sessions.filter { it.avgReactionTimeMs > 0f }
            val avgReactionMs = if (reactionSessions.isNotEmpty()) {
                reactionSessions.map { it.avgReactionTimeMs }.average().toFloat()
            } else {
                0f
            }

            val avgRatio = sessions.map { it.performanceRatio }.average().toFloat()

            val accuracyHistory = sessions.mapIndexed { idx, session ->
                SessionMetricPoint(
                    sessionIndex = idx + 1,
                    timestampMs = session.timestampMs,
                    value = session.accuracyPercent,
                    label = session.subMode
                )
            }

            val reactionHistory = sessions.mapIndexed { idx, session ->
                SessionMetricPoint(
                    sessionIndex = idx + 1,
                    timestampMs = session.timestampMs,
                    value = session.avgReactionTimeMs,
                    label = session.subMode
                )
            }

            return TrainingStatistics(
                totalSessions = totalSessions,
                bestScore = bestScore,
                averageScore = averageScore,
                overallAccuracyPercent = overallAccuracy,
                centerZoneAccuracyPercent = centerZoneAvg,
                averageReactionTimeMs = avgReactionMs,
                totalTrainingDurationMs = totalDurationMs,
                simulatedPerformanceRatio = avgRatio,
                accuracyHistory = accuracyHistory,
                reactionTimeHistory = reactionHistory
            )
        }
    }
}
