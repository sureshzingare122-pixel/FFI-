package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CrosshairStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ffi_trainer_preferences"
)

data class AppPreferences(
    val hapticFeedbackEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val activeMovementProfileId: String = "RUSH",
    val practiceDurationSeconds: Int = 45,
    val crosshairStyle: CrosshairStyle = CrosshairStyle.TACTICAL_DOT,
    val showHudGrid: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val PRACTICE_DURATION_SEC = intPreferencesKey("practice_duration_sec")
        val CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        val SHOW_HUD_GRID = booleanPreferencesKey("show_hud_grid")
    }

    val preferencesFlow: Flow<AppPreferences> = context.userPrefsDataStore.data.map { prefs ->
        val styleName = prefs[Keys.CROSSHAIR_STYLE] ?: CrosshairStyle.TACTICAL_DOT.name
        val style = CrosshairStyle.entries.firstOrNull { it.name == styleName }
            ?: CrosshairStyle.TACTICAL_DOT
        AppPreferences(
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            soundEffectsEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            activeMovementProfileId = prefs[Keys.ACTIVE_PROFILE_ID] ?: "RUSH",
            practiceDurationSeconds = prefs[Keys.PRACTICE_DURATION_SEC] ?: 45,
            crosshairStyle = style,
            showHudGrid = prefs[Keys.SHOW_HUD_GRID] ?: true
        )
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setActiveMovementProfileId(profileId: String) {
        context.userPrefsDataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }

    suspend fun setPracticeDurationSeconds(seconds: Int) {
        context.userPrefsDataStore.edit { it[Keys.PRACTICE_DURATION_SEC] = seconds.coerceIn(15, 120) }
    }

    suspend fun setCrosshairStyle(style: CrosshairStyle) {
        context.userPrefsDataStore.edit { it[Keys.CROSSHAIR_STYLE] = style.name }
    }

    suspend fun setShowHudGrid(show: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.SHOW_HUD_GRID] = show }
    }

    suspend fun resetDefaults() {
        context.userPrefsDataStore.edit { it.clear() }
    }
}
