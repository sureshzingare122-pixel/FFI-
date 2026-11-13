package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.FFITrainerApplication

class FFIViewModelFactory(private val app: FFITrainerApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PracticeArenaViewModel::class.java) ->
                PracticeArenaViewModel(app.trainingRepository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(MovementTrainerViewModel::class.java) ->
                MovementTrainerViewModel(app.trainingRepository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(AimReactionViewModel::class.java) ->
                AimReactionViewModel(app.trainingRepository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(SensitivityCalibratorViewModel::class.java) ->
                SensitivityCalibratorViewModel(app.trainingRepository) as T
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) ->
                StatisticsViewModel(app.trainingRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.trainingRepository, app.preferencesRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
