package com.relationaliq.presentation.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.BlockType
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.model.TrialResult
import com.relationaliq.domain.usecase.SessionResult
import com.relationaliq.domain.usecase.TrainingEngineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingUiState(
    val isLoading: Boolean = true,
    val stage: Stage? = null,
    val currentBlockType: BlockType = BlockType.TRAINING,
    val currentTrialIndex: Int = 0,
    val currentTrial: Trial? = null,
    val totalTrials: Int = 0,
    val timeRemainingSeconds: Int = 30,
    val lastResult: TrialResult? = null,
    val showFeedback: Boolean = false,
    val feedbackCorrect: Boolean = false,
    val results: List<TrialResult> = emptyList(),
    val sessionComplete: Boolean = false,
    val sessionResult: SessionResult? = null,
    val sessionId: Long = 0,
    val error: String? = null
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainingEngine: TrainingEngineUseCase
) : ViewModel() {

    private val stageId: Int = savedStateHandle["stageId"] ?: 1

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var trialStartTime: Long = 0
    private var sessionStartTime: Long = 0

    init {
        loadStage()
    }

    private fun loadStage() {
        viewModelScope.launch {
            val stage = trainingEngine.loadStage(stageId)
            if (stage == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Stage not found")
                return@launch
            }
            val sessionId = trainingEngine.startSession(stageId, BlockType.TRAINING)
            sessionStartTime = System.currentTimeMillis()
            val trials = stage.trainingTrials
            _uiState.value = TrainingUiState(
                isLoading = false,
                stage = stage,
                currentBlockType = BlockType.TRAINING,
                currentTrialIndex = 0,
                currentTrial = trials.firstOrNull(),
                totalTrials = trials.size,
                timeRemainingSeconds = stage.timeLimitSeconds,
                sessionId = sessionId
            )
            startTimer()
        }
    }

    fun submitAnswer(answer: Boolean) {
        val state = _uiState.value
        val trial = state.currentTrial ?: return
        val responseTimeMs = System.currentTimeMillis() - trialStartTime
        timerJob?.cancel()

        viewModelScope.launch {
            val result = trainingEngine.submitAnswer(state.sessionId, trial, answer, responseTimeMs)
            val updatedResults = state.results + result

            _uiState.value = state.copy(
                lastResult = result,
                showFeedback = true,
                feedbackCorrect = result.isCorrect,
                results = updatedResults
            )

            delay(1500)

            val trials = getCurrentTrials()
            val nextIndex = state.currentTrialIndex + 1

            if (nextIndex >= trials.size) {
                if (state.currentBlockType == BlockType.TRAINING) {
                    switchToTestBlock(updatedResults)
                } else {
                    completeSession(updatedResults)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    currentTrialIndex = nextIndex,
                    currentTrial = trials[nextIndex],
                    showFeedback = false,
                    timeRemainingSeconds = state.stage?.timeLimitSeconds ?: 30
                )
                startTimer()
            }
        }
    }

    private fun switchToTestBlock(trainingResults: List<TrialResult>) {
        viewModelScope.launch {
            val stage = _uiState.value.stage ?: return@launch
            val sessionId = trainingEngine.startSession(stageId, BlockType.TEST)
            val testTrials = stage.testTrials

            _uiState.value = _uiState.value.copy(
                currentBlockType = BlockType.TEST,
                currentTrialIndex = 0,
                currentTrial = testTrials.firstOrNull(),
                totalTrials = testTrials.size,
                showFeedback = false,
                results = emptyList(),
                sessionId = sessionId,
                timeRemainingSeconds = stage.timeLimitSeconds
            )
            sessionStartTime = System.currentTimeMillis()
            startTimer()
        }
    }

    private fun completeSession(results: List<TrialResult>) {
        viewModelScope.launch {
            val state = _uiState.value
            val sessionResult = trainingEngine.completeSession(
                sessionId = state.sessionId,
                stageId = stageId,
                blockType = state.currentBlockType,
                results = results,
                startTime = sessionStartTime
            )
            _uiState.value = state.copy(
                sessionComplete = true,
                sessionResult = sessionResult,
                showFeedback = false
            )
        }
    }

    private fun getCurrentTrials(): List<Trial> {
        val state = _uiState.value
        val stage = state.stage ?: return emptyList()
        return if (state.currentBlockType == BlockType.TRAINING) stage.trainingTrials else stage.testTrials
    }

    private fun startTimer() {
        timerJob?.cancel()
        trialStartTime = System.currentTimeMillis()
        val timeLimit = _uiState.value.stage?.timeLimitSeconds ?: 30
        _uiState.value = _uiState.value.copy(timeRemainingSeconds = timeLimit)

        timerJob = viewModelScope.launch {
            for (i in timeLimit downTo 0) {
                _uiState.value = _uiState.value.copy(timeRemainingSeconds = i)
                if (i == 0) {
                    submitAnswer(false)
                    break
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
