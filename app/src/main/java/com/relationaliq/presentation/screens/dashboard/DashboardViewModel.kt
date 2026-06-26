package com.relationaliq.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.repository.ExamRepository
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.UserRepository
import com.relationaliq.domain.usecase.AdaptiveExamEngine
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
    val isLoading: Boolean = true,
    val examAvailable: Boolean = false,
    val pendingExamId: Int? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val examRepository: ExamRepository,
    private val adaptiveExamEngine: AdaptiveExamEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeProfile().collect { profile ->
                val stageId = profile?.currentStageId ?: 1
                _uiState.value = _uiState.value.copy(
                    currentStageId = stageId,
                    currentStreak = profile?.currentStreak ?: 0,
                    totalXp = profile?.totalXp ?: 0,
                    isLoading = false
                )
                checkExamAvailability(stageId)
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

    private suspend fun checkExamAvailability(currentStageId: Int) {
        val completedStages = currentStageId - 1
        if (completedStages < AdaptiveExamEngine.STAGES_PER_EXAM) {
            _uiState.value = _uiState.value.copy(examAvailable = false, pendingExamId = null)
            return
        }
        val latestExamNumber = completedStages / AdaptiveExamEngine.STAGES_PER_EXAM
        val alreadyPassed = examRepository.hasPassedExam(latestExamNumber)
        if (!alreadyPassed) {
            _uiState.value = _uiState.value.copy(
                examAvailable = true,
                pendingExamId = latestExamNumber
            )
        } else {
            _uiState.value = _uiState.value.copy(examAvailable = false, pendingExamId = null)
        }
    }
}
