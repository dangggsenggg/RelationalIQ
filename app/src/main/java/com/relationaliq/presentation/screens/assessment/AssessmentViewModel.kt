package com.relationaliq.presentation.screens.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.Premise
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssessmentUiState(
    val showIntro: Boolean = true,
    val showResult: Boolean = false,
    val currentIndex: Int = 0,
    val totalTrials: Int = 0,
    val currentTrial: Trial? = null,
    val score: Float = 0f,
    val correctCount: Int = 0,
    val trials: List<Trial> = emptyList()
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    fun startAssessment(isPreAssessment: Boolean) {
        val trials = generateAssessmentTrials()
        _uiState.value = AssessmentUiState(
            showIntro = false,
            currentIndex = 0,
            totalTrials = trials.size,
            currentTrial = trials.firstOrNull(),
            trials = trials
        )
    }

    fun submitAnswer(answer: Boolean) {
        val state = _uiState.value
        val trial = state.currentTrial ?: return
        val isCorrect = answer == trial.correctAnswer
        val newCorrectCount = state.correctCount + if (isCorrect) 1 else 0
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.trials.size) {
            val score = newCorrectCount.toFloat() / state.trials.size
            _uiState.value = state.copy(
                showResult = true,
                score = score,
                correctCount = newCorrectCount
            )
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                currentTrial = state.trials[nextIndex],
                correctCount = newCorrectCount
            )
        }
    }

    fun saveResult(isPreAssessment: Boolean) {
        viewModelScope.launch {
            val profile = userRepository.getProfile() ?: return@launch
            val score = _uiState.value.score
            if (isPreAssessment) {
                userRepository.savePreAssessmentScore(profile.id, score)
            } else {
                userRepository.savePostAssessmentScore(profile.id, score)
            }
        }
    }

    private fun generateAssessmentTrials(): List<Trial> {
        val stimuli = listOf("GYQ", "FYW", "VOP", "CUB", "KES", "DAX", "PIR", "MOQ", "ZUB", "TEL")
        val trials = mutableListOf<Trial>()

        // Same relations (easy)
        trials.add(createTrial("a1", stimuli[0], RelationType.SAME, stimuli[1], stimuli[0], RelationType.SAME, stimuli[1], true))
        trials.add(createTrial("a2", stimuli[2], RelationType.SAME, stimuli[3], stimuli[3], RelationType.SAME, stimuli[2], true))
        trials.add(createTrial("a3", stimuli[0], RelationType.SAME, stimuli[1], stimuli[0], RelationType.DIFFERENT, stimuli[1], false))

        // Derived same relations
        trials.add(createDerivedTrial("a4",
            listOf(Premise(stimuli[0], RelationType.SAME, stimuli[1]), Premise(stimuli[1], RelationType.SAME, stimuli[2])),
            stimuli[0], RelationType.SAME, stimuli[2], true))

        // Opposite relations
        trials.add(createTrial("a5", stimuli[4], RelationType.OPPOSITE, stimuli[5], stimuli[5], RelationType.OPPOSITE, stimuli[4], true))
        trials.add(createTrial("a6", stimuli[4], RelationType.OPPOSITE, stimuli[5], stimuli[4], RelationType.SAME, stimuli[5], false))

        // More/Less relations
        trials.add(createTrial("a7", stimuli[6], RelationType.MORE_THAN, stimuli[7], stimuli[7], RelationType.LESS_THAN, stimuli[6], true))
        trials.add(createTrial("a8", stimuli[6], RelationType.MORE_THAN, stimuli[7], stimuli[6], RelationType.LESS_THAN, stimuli[7], false))

        // Derived comparison
        trials.add(createDerivedTrial("a9",
            listOf(Premise(stimuli[0], RelationType.MORE_THAN, stimuli[1]), Premise(stimuli[1], RelationType.MORE_THAN, stimuli[2])),
            stimuli[0], RelationType.MORE_THAN, stimuli[2], true))
        trials.add(createDerivedTrial("a10",
            listOf(Premise(stimuli[0], RelationType.MORE_THAN, stimuli[1]), Premise(stimuli[1], RelationType.MORE_THAN, stimuli[2])),
            stimuli[2], RelationType.MORE_THAN, stimuli[0], false))

        // Before/After
        trials.add(createTrial("a11", stimuli[8], RelationType.BEFORE, stimuli[9], stimuli[9], RelationType.AFTER, stimuli[8], true))

        // Complex derived
        trials.add(createDerivedTrial("a12",
            listOf(Premise(stimuli[3], RelationType.SAME, stimuli[4]), Premise(stimuli[4], RelationType.MORE_THAN, stimuli[5])),
            stimuli[3], RelationType.MORE_THAN, stimuli[5], true))

        trials.add(createDerivedTrial("a13",
            listOf(Premise(stimuli[6], RelationType.OPPOSITE, stimuli[7]), Premise(stimuli[7], RelationType.MORE_THAN, stimuli[8])),
            stimuli[6], RelationType.LESS_THAN, stimuli[8], true))

        // Multi-premise chains
        trials.add(createDerivedTrial("a14",
            listOf(Premise(stimuli[0], RelationType.SAME, stimuli[1]), Premise(stimuli[1], RelationType.MORE_THAN, stimuli[2]), Premise(stimuli[2], RelationType.MORE_THAN, stimuli[3])),
            stimuli[0], RelationType.MORE_THAN, stimuli[3], true))

        trials.add(createDerivedTrial("a15",
            listOf(Premise(stimuli[4], RelationType.BEFORE, stimuli[5]), Premise(stimuli[5], RelationType.BEFORE, stimuli[6])),
            stimuli[4], RelationType.BEFORE, stimuli[6], true))

        trials.add(createDerivedTrial("a16",
            listOf(Premise(stimuli[7], RelationType.SAME, stimuli[8]), Premise(stimuli[8], RelationType.OPPOSITE, stimuli[9])),
            stimuli[7], RelationType.OPPOSITE, stimuli[9], true))

        trials.add(createDerivedTrial("a17",
            listOf(Premise(stimuli[0], RelationType.MORE_THAN, stimuli[1]), Premise(stimuli[1], RelationType.SAME, stimuli[2])),
            stimuli[2], RelationType.MORE_THAN, stimuli[0], false))

        trials.add(createDerivedTrial("a18",
            listOf(Premise(stimuli[3], RelationType.LESS_THAN, stimuli[4]), Premise(stimuli[4], RelationType.LESS_THAN, stimuli[5])),
            stimuli[3], RelationType.LESS_THAN, stimuli[5], true))

        trials.add(createDerivedTrial("a19",
            listOf(Premise(stimuli[6], RelationType.AFTER, stimuli[7]), Premise(stimuli[7], RelationType.AFTER, stimuli[8])),
            stimuli[8], RelationType.BEFORE, stimuli[6], true))

        trials.add(createDerivedTrial("a20",
            listOf(Premise(stimuli[0], RelationType.SAME, stimuli[1]), Premise(stimuli[1], RelationType.OPPOSITE, stimuli[2]), Premise(stimuli[2], RelationType.MORE_THAN, stimuli[3])),
            stimuli[0], RelationType.LESS_THAN, stimuli[3], true))

        return trials
    }

    private fun createTrial(
        id: String,
        stimA: String, relation: RelationType, stimB: String,
        probeA: String, probeRel: RelationType, probeB: String,
        correct: Boolean
    ): Trial = Trial(
        id = id,
        premises = listOf(Premise(stimA, relation, stimB)),
        probeStimA = probeA,
        probeRelation = probeRel,
        probeStimB = probeB,
        correctAnswer = correct
    )

    private fun createDerivedTrial(
        id: String,
        premises: List<Premise>,
        probeA: String, probeRel: RelationType, probeB: String,
        correct: Boolean
    ): Trial = Trial(
        id = id,
        premises = premises,
        probeStimA = probeA,
        probeRelation = probeRel,
        probeStimB = probeB,
        correctAnswer = correct
    )
}
