package com.relationaliq.presentation.screens.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.AdaptiveState
import com.relationaliq.domain.model.Difficulty
import com.relationaliq.domain.model.Exam
import com.relationaliq.domain.model.ExamResult
import com.relationaliq.domain.model.ExamTrialState
import com.relationaliq.domain.model.RelationScore
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.usecase.AdaptiveExamEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExamPhase {
    INTRO, QUESTION, FEEDBACK, RESULTS
}

data class ExamUiState(
    val phase: ExamPhase = ExamPhase.INTRO,
    val exam: Exam? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTrial: ExamTrialState? = null,
    val questionsAnswered: Int = 0,
    val totalQuestions: Int = 15,
    val currentDifficulty: Difficulty = Difficulty.MEDIUM,
    val timeRemainingSeconds: Int = 35,
    val showFeedback: Boolean = false,
    val feedbackCorrect: Boolean = false,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val examResult: ExamResult? = null,
    val relationTypeScores: Map<RelationType, RelationScore> = emptyMap()
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adaptiveExamEngine: AdaptiveExamEngine
) : ViewModel() {

    private val examId: Int = savedStateHandle["examId"] ?: 1

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadExam()
    }

    private fun loadExam() {
        viewModelScope.launch {
            val stageCheckpoint = examId * AdaptiveExamEngine.STAGES_PER_EXAM
            val exam = adaptiveExamEngine.getExamForStageCheckpoint(stageCheckpoint)
            if (exam == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Exam not found"
                )
                return@launch
            }
            val initialized = adaptiveExamEngine.initializeExam(exam)
            if (!initialized) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not load exam stages"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                exam = exam,
                totalQuestions = exam.totalQuestions,
                phase = ExamPhase.INTRO
            )
        }
    }

    fun startExam() {
        viewModelScope.launch {
            val trial = adaptiveExamEngine.generateNextTrial()
            if (trial == null) {
                _uiState.value = _uiState.value.copy(error = "No trials available")
                return@launch
            }
            val state = adaptiveExamEngine.getCurrentState()
            _uiState.value = _uiState.value.copy(
                phase = ExamPhase.QUESTION,
                currentTrial = trial,
                currentDifficulty = state.currentDifficulty,
                timeRemainingSeconds = AdaptiveExamEngine.timeLimitForDifficulty(state.currentDifficulty)
            )
            startTimer()
        }
    }

    fun submitAnswer(answer: Boolean) {
        timerJob?.cancel()
        val trialState = _uiState.value.currentTrial ?: return

        viewModelScope.launch {
            val isCorrect = adaptiveExamEngine.submitAnswer(trialState, answer)
            val adaptiveState = adaptiveExamEngine.getCurrentState()

            _uiState.value = _uiState.value.copy(
                phase = ExamPhase.FEEDBACK,
                showFeedback = true,
                feedbackCorrect = isCorrect,
                questionsAnswered = adaptiveState.questionsAnswered,
                currentDifficulty = adaptiveState.currentDifficulty,
                consecutiveCorrect = adaptiveState.consecutiveCorrect,
                consecutiveWrong = adaptiveState.consecutiveWrong,
                relationTypeScores = adaptiveState.relationTypeScores.toMap()
            )

            delay(1500)

            if (adaptiveExamEngine.isExamComplete()) {
                completeExam()
            } else {
                val nextTrial = adaptiveExamEngine.generateNextTrial()
                if (nextTrial == null) {
                    completeExam()
                    return@launch
                }
                val freshState = adaptiveExamEngine.getCurrentState()
                _uiState.value = _uiState.value.copy(
                    phase = ExamPhase.QUESTION,
                    currentTrial = nextTrial,
                    showFeedback = false,
                    currentDifficulty = freshState.currentDifficulty,
                    timeRemainingSeconds = AdaptiveExamEngine.timeLimitForDifficulty(freshState.currentDifficulty)
                )
                startTimer()
            }
        }
    }

    private suspend fun completeExam() {
        val result = adaptiveExamEngine.completeExam()
        _uiState.value = _uiState.value.copy(
            phase = ExamPhase.RESULTS,
            showFeedback = false,
            examResult = result
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        val timeLimit = AdaptiveExamEngine.timeLimitForDifficulty(
            _uiState.value.currentDifficulty
        )
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
