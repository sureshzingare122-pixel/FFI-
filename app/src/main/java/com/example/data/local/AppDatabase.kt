package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SensitivitySettingsDao
import com.example.data.local.dao.TrainingSessionDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.model.SensitivityPreset
import com.example.data.model.SensitivitySettings
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile

@Database(
    entities = [
        TrainingSession::class,
        UserProfile::class,
        SensitivitySettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun sensitivitySettingsDao(): SensitivitySettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun defaultProfiles(): List<UserProfile> = listOf(
            UserProfile(
                profileId = "RUSH",
                displayName = "RUSH",
                description = "High-tempo entry movement with low dead zone and snappy turn response.",
                joystickDeadZone = 0.06f,
                joystickSensitivity = 1.65f,
                movementAcceleration = 1.55f,
                responseCurveExponent = 1.15f,
                dragAimSensitivity = 1.55f
            ),
            UserProfile(
                profileId = "DEFENSIVE",
                displayName = "DEFENSIVE",
                description = "Balanced strafe control and steady micro-tracking stability.",
                joystickDeadZone = 0.12f,
                joystickSensitivity = 1.15f,
                movementAcceleration = 1.05f,
                responseCurveExponent = 1.40f,
                dragAimSensitivity = 1.10f
            ),
            UserProfile(
                profileId = "SNIPER",
                displayName = "SNIPER",
                description = "Precision micro-adjustment curve with controlled dead zone for long-range holds.",
                joystickDeadZone = 0.16f,
                joystickSensitivity = 0.85f,
                movementAcceleration = 0.85f,
                responseCurveExponent = 1.85f,
                dragAimSensitivity = 0.80f
            )
        )

        fun defaultSensitivitySettings(): SensitivitySettings = SensitivitySettings(
            id = 1,
            activePreset = SensitivityPreset.MEDIUM.name,
            crosshairSensitivity = 1.15f,
            joystickResponse = 1.10f
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ffi_trainer_pro.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
