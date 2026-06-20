package com.relationaliq.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val currentStageId: Int = 1,
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val stagesCompleted: Int = 0,
    val averageAccuracy: Float? = null,
    val totalTrials: Int = 0,
    val totalCorrect: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    currentStageId = profile?.currentStageId ?: 1,
                    currentStreak = profile?.currentStreak ?: 0,
                    totalXp = profile?.totalXp ?: 0,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            progressRepository.observeCompletedStagesCount().collect { count ->
                _uiState.value = _uiState.value.copy(stagesCompleted = count)
            }
        }
        viewModelScope.launch {
            progressRepository.observeAverageAccuracy().collect { accuracy ->
                _uiState.value = _uiState.value.copy(averageAccuracy = accuracy)
            }
        }
        viewModelScope.launch {
            progressRepository.observeTotalTrials().collect { total ->
                _uiState.value = _uiState.value.copy(totalTrials = total)
            }
        }
        viewModelScope.launch {
            progressRepository.observeTotalCorrect().collect { correct ->
                _uiState.value = _uiState.value.copy(totalCorrect = correct)
            }
        }
    }
}
