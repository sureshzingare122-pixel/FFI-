package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TrainingSession
import com.example.data.model.TrainingStatistics
import com.example.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data object Empty : StatisticsUiState
    data class Content(
        val statistics: TrainingStatistics,
        val sessions: List<TrainingSession>
    ) : StatisticsUiState
}

class StatisticsViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.statisticsFlow,
                repository.allSessionsDesc
            ) { stats, sessions ->
                stats to sessions
            }.collect { (stats, sessions) ->
                _uiState.value = if (sessions.isEmpty()) {
                    StatisticsUiState.Empty
                } else {
                    StatisticsUiState.Content(
                        statistics = stats,
                        sessions = sessions
                    )
                }
            }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
            _statusMessage.update { "Session deleted from local log." }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllSessions()
            _statusMessage.update { "All local training records cleared." }
        }
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
