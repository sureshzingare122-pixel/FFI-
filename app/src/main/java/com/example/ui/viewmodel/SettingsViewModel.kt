package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AppPreferences
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.model.CrosshairStyle
import com.example.data.model.SensitivitySettings
import com.example.data.model.UserProfile
import com.example.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val profiles: List<UserProfile> = emptyList(),
    val sensitivitySettings: SensitivitySettings = SensitivitySettings(),
    val statusBanner: String? = null
)

class SettingsViewModel(
    private val trainingRepository: TrainingRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferencesFlow,
                trainingRepository.profilesFlow,
                trainingRepository.sensitivitySettingsFlow
            ) { prefs, profiles, sens ->
                Triple(prefs, profiles, sens)
            }.collect { (prefs, profiles, sens) ->
                _uiState.update {
                    it.copy(
                        preferences = prefs,
                        profiles = profiles,
                        sensitivitySettings = sens
                    )
                }
            }
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticFeedbackEnabled(enabled) }
    }

    fun setSoundEffects(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEffectsEnabled(enabled) }
    }

    fun setPracticeDuration(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setPracticeDurationSeconds(seconds) }
    }

    fun setCrosshairStyle(style: CrosshairStyle) {
        viewModelScope.launch { preferencesRepository.setCrosshairStyle(style) }
    }

    fun setShowHudGrid(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowHudGrid(show) }
    }

    fun setActiveProfile(profileId: String) {
        viewModelScope.launch { preferencesRepository.setActiveMovementProfileId(profileId) }
    }

    fun resetAllPreferencesAndProfiles() {
        viewModelScope.launch {
            preferencesRepository.resetDefaults()
            trainingRepository.resetProfilesToDefaults()
            _uiState.update { it.copy(statusBanner = "Simulator preferences and movement profiles restored to defaults.") }
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }
}
