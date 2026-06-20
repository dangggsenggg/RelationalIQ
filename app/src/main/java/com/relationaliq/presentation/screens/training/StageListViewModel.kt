package com.relationaliq.presentation.screens.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StageListUiState(
    val stages: List<Stage> = emptyList(),
    val unlockedStageIds: Set<Int> = emptySet(),
    val completedStageIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StageListViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StageListUiState())
    val uiState: StateFlow<StageListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stages = trainingRepository.getAllStages()
            _uiState.value = _uiState.value.copy(stages = stages, isLoading = false)
        }
        viewModelScope.launch {
            progressRepository.observeAllStageProgress().collect { progressList ->
                _uiState.value = _uiState.value.copy(
                    unlockedStageIds = progressList.filter { it.isUnlocked }.map { it.stageId }.toSet(),
                    completedStageIds = progressList.filter { it.isCompleted }.map { it.stageId }.toSet()
                )
            }
        }
    }
}
