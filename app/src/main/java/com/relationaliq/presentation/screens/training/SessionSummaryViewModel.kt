package com.relationaliq.presentation.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.TrainingSession
import com.relationaliq.domain.repository.TrainingRepository
import com.relationaliq.domain.usecase.AdaptiveExamEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSummaryUiState(
    val session: TrainingSession? = null,
    val isLoading: Boolean = true,
    val examAvailable: Boolean = false,
    val examId: Int? = null
)

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainingRepository: TrainingRepository,
    private val adaptiveExamEngine: AdaptiveExamEngine
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = trainingRepository.getSessionById(sessionId)
            val stageId = session?.stageId ?: 0
            val exam = adaptiveExamEngine.getExamForStageCheckpoint(stageId)
            _uiState.value = SessionSummaryUiState(
                session = session,
                isLoading = false,
                examAvailable = exam != null && session?.passed == true,
                examId = exam?.id
            )
        }
    }
}
