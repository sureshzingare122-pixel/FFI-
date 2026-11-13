package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SensitivitySettings
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {
    @Query("SELECT * FROM training_sessions ORDER BY timestampMs DESC")
    fun observeAllSessions(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions ORDER BY timestampMs ASC")
    fun observeSessionsChronological(): Flow<List<TrainingSession>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: TrainingSession): Long

    @Query("DELETE FROM training_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM training_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT COUNT(*) FROM training_sessions")
    suspend fun getSessionCount(): Int
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY CASE profileId WHEN 'RUSH' THEN 1 WHEN 'DEFENSIVE' THEN 2 WHEN 'SNIPER' THEN 3 ELSE 4 END")
    fun observeProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE profileId = :profileId LIMIT 1")
    fun observeProfile(profileId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfileOnce(profileId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfilesIgnore(profiles: List<UserProfile>)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)
}

@Dao
interface SensitivitySettingsDao {
    @Query("SELECT * FROM sensitivity_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SensitivitySettings?>

    @Query("SELECT * FROM sensitivity_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): SensitivitySettings?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultIgnore(settings: SensitivitySettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SensitivitySettings)
}
