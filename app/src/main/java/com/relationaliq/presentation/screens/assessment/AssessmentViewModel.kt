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

data class SubscaleScore(
    val name: String,
    val correct: Int = 0,
    val total: Int = 0
) {
    val accuracy: Float get() = if (total > 0) correct.toFloat() / total else 0f
}

data class AssessmentUiState(
    val showIntro: Boolean = true,
    val showResult: Boolean = false,
    val currentIndex: Int = 0,
    val totalTrials: Int = 0,
    val currentTrial: Trial? = null,
    val score: Float = 0f,
    val correctCount: Int = 0,
    val trials: List<Trial> = emptyList(),
    val subscaleScores: Map<String, SubscaleScore> = emptyMap(),
    val totalResponseTimeMs: Long = 0,
    val fluencyScore: Float = 0f
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    private var trialStartTime: Long = 0
    private val trialResponseTimes = mutableListOf<Long>()
    private val trialResults = mutableListOf<Boolean>()

    fun startAssessment(isPreAssessment: Boolean) {
        val trials = generateAssessmentTrials()
        trialResponseTimes.clear()
        trialResults.clear()
        _uiState.value = AssessmentUiState(
            showIntro = false,
            currentIndex = 0,
            totalTrials = trials.size,
            currentTrial = trials.firstOrNull(),
            trials = trials
        )
        trialStartTime = System.currentTimeMillis()
    }

    fun submitAnswer(answer: Boolean) {
        val state = _uiState.value
        val trial = state.currentTrial ?: return
        val responseTime = System.currentTimeMillis() - trialStartTime
        val isCorrect = answer == trial.correctAnswer
        val newCorrectCount = state.correctCount + if (isCorrect) 1 else 0
        val nextIndex = state.currentIndex + 1

        trialResponseTimes.add(responseTime)
        trialResults.add(isCorrect)

        if (nextIndex >= state.trials.size) {
            val score = newCorrectCount.toFloat() / state.trials.size
            val subscales = computeSubscaleScores(state.trials, trialResults)
            val avgCorrectTime = computeAverageCorrectTime()
            _uiState.value = state.copy(
                showResult = true,
                score = score,
                correctCount = newCorrectCount,
                subscaleScores = subscales,
                totalResponseTimeMs = trialResponseTimes.sum(),
                fluencyScore = avgCorrectTime
            )
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                currentTrial = state.trials[nextIndex],
                correctCount = newCorrectCount
            )
            trialStartTime = System.currentTimeMillis()
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

    private fun computeSubscaleScores(
        trials: List<Trial>,
        results: List<Boolean>
    ): Map<String, SubscaleScore> {
        val subscaleMap = mutableMapOf<String, MutableList<Boolean>>()
        for ((i, trial) in trials.withIndex()) {
            val subscale = getTrialSubscale(trial.id)
            subscaleMap.getOrPut(subscale) { mutableListOf() }.add(results[i])
        }
        return subscaleMap.mapValues { (name, resultList) ->
            SubscaleScore(
                name = name,
                correct = resultList.count { it },
                total = resultList.size
            )
        }
    }

    private fun computeAverageCorrectTime(): Float {
        val correctTimes = trialResponseTimes.zip(trialResults)
            .filter { it.second }
            .map { it.first }
        return if (correctTimes.isNotEmpty()) correctTimes.average().toFloat() else 0f
    }

    private fun getTrialSubscale(trialId: String): String {
        return when {
            trialId.startsWith("coord_") -> "Coordination"
            trialId.startsWith("comp_") -> "Comparison"
            trialId.startsWith("opp_") -> "Opposition"
            trialId.startsWith("temp_") -> "Temporal"
            trialId.startsWith("cont_") -> "Containment"
            trialId.startsWith("mix_") -> "Mixed"
            trialId.startsWith("anal_") -> "Analogy"
            else -> "Other"
        }
    }

    private fun generateAssessmentTrials(): List<Trial> {
        val trials = mutableListOf<Trial>()

        // ============================================================
        // SUBSCALE 1: COORDINATION (Same/Different) - 10 items
        // ============================================================

        // coord_1: SAME symmetry (1 premise, easy)
        trials.add(Trial(
            id = "coord_1",
            premises = listOf(Premise("GYQ", RelationType.SAME, "FYW")),
            probeStimA = "FYW", probeRelation = RelationType.SAME, probeStimB = "GYQ",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "If GYQ is the same as FYW, then FYW is the same as GYQ (symmetry)."
        ))
        // coord_2: SAME transitivity (2 premises)
        trials.add(Trial(
            id = "coord_2",
            premises = listOf(
                Premise("KES", RelationType.SAME, "DAX"),
                Premise("DAX", RelationType.SAME, "PIR")
            ),
            probeStimA = "KES", probeRelation = RelationType.SAME, probeStimB = "PIR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "KES = DAX = PIR, so KES is the same as PIR."
        ))
        // coord_3: SAME chain wrong probe (2 premises)
        trials.add(Trial(
            id = "coord_3",
            premises = listOf(
                Premise("VOP", RelationType.SAME, "CUB"),
                Premise("CUB", RelationType.SAME, "MOQ")
            ),
            probeStimA = "VOP", probeRelation = RelationType.DIFFERENT, probeStimB = "MOQ",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "VOP = CUB = MOQ, so VOP is the SAME as MOQ, not different."
        ))
        // coord_4: DIFFERENT symmetry
        trials.add(Trial(
            id = "coord_4",
            premises = listOf(Premise("RUF", RelationType.DIFFERENT, "WEX")),
            probeStimA = "WEX", probeRelation = RelationType.DIFFERENT, probeStimB = "RUF",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "If RUF is different from WEX, then WEX is different from RUF."
        ))
        // coord_5: SAME 3-premise chain
        trials.add(Trial(
            id = "coord_5",
            premises = listOf(
                Premise("NAV", RelationType.SAME, "QIP"),
                Premise("QIP", RelationType.SAME, "DUK"),
                Premise("DUK", RelationType.SAME, "FEZ")
            ),
            probeStimA = "NAV", probeRelation = RelationType.SAME, probeStimB = "FEZ",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "NAV = QIP = DUK = FEZ, so NAV is the same as FEZ."
        ))
        // coord_6: SAME 3-premise wrong
        trials.add(Trial(
            id = "coord_6",
            premises = listOf(
                Premise("LYR", RelationType.SAME, "BOK"),
                Premise("BOK", RelationType.SAME, "TEL"),
                Premise("TEL", RelationType.SAME, "ZUB")
            ),
            probeStimA = "LYR", probeRelation = RelationType.DIFFERENT, probeStimB = "ZUB",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "LYR = BOK = TEL = ZUB, so LYR is the SAME as ZUB, not different."
        ))
        // coord_7: SAME 2-premise reversed probe
        trials.add(Trial(
            id = "coord_7",
            premises = listOf(
                Premise("HEW", RelationType.SAME, "JOT"),
                Premise("JOT", RelationType.SAME, "BGW")
            ),
            probeStimA = "BGW", probeRelation = RelationType.SAME, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "HEW = JOT = BGW. Sameness is symmetric, so BGW = HEW."
        ))
        // coord_8: DIFFERENT wrong probe
        trials.add(Trial(
            id = "coord_8",
            premises = listOf(Premise("AWX", RelationType.DIFFERENT, "EGC")),
            probeStimA = "AWX", probeRelation = RelationType.SAME, probeStimB = "EGC",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "AWX is DIFFERENT from EGC, not the same."
        ))
        // coord_9: SAME 4-premise chain
        trials.add(Trial(
            id = "coord_9",
            premises = listOf(
                Premise("OPA", RelationType.SAME, "FYW"),
                Premise("FYW", RelationType.SAME, "CUB"),
                Premise("CUB", RelationType.SAME, "DAX"),
                Premise("DAX", RelationType.SAME, "PIR")
            ),
            probeStimA = "OPA", probeRelation = RelationType.SAME, probeStimB = "PIR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "OPA = FYW = CUB = DAX = PIR, so OPA is the same as PIR."
        ))
        // coord_10: SAME 4-premise wrong
        trials.add(Trial(
            id = "coord_10",
            premises = listOf(
                Premise("WEX", RelationType.SAME, "NAV"),
                Premise("NAV", RelationType.SAME, "QIP"),
                Premise("QIP", RelationType.SAME, "DUK"),
                Premise("DUK", RelationType.SAME, "FEZ")
            ),
            probeStimA = "WEX", probeRelation = RelationType.DIFFERENT, probeStimB = "FEZ",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "WEX = NAV = QIP = DUK = FEZ, so WEX is the SAME as FEZ, not different."
        ))

        // ============================================================
        // SUBSCALE 2: COMPARISON (More/Less) - 12 items
        // ============================================================

        // comp_1: MORE_THAN 2-premise
        trials.add(Trial(
            id = "comp_1",
            premises = listOf(
                Premise("AWX", RelationType.MORE_THAN, "EGC"),
                Premise("EGC", RelationType.MORE_THAN, "OPA")
            ),
            probeStimA = "AWX", probeRelation = RelationType.MORE_THAN, probeStimB = "OPA",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX > EGC > OPA, so AWX > OPA."
        ))
        // comp_2: MORE_THAN reversed
        trials.add(Trial(
            id = "comp_2",
            premises = listOf(
                Premise("BGW", RelationType.MORE_THAN, "MOQ"),
                Premise("MOQ", RelationType.MORE_THAN, "TEL")
            ),
            probeStimA = "TEL", probeRelation = RelationType.MORE_THAN, probeStimB = "BGW",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "BGW > MOQ > TEL, so TEL is LESS than BGW, not more."
        ))
        // comp_3: LESS_THAN 2-premise
        trials.add(Trial(
            id = "comp_3",
            premises = listOf(
                Premise("ZUB", RelationType.LESS_THAN, "HEW"),
                Premise("HEW", RelationType.LESS_THAN, "JOT")
            ),
            probeStimA = "ZUB", probeRelation = RelationType.LESS_THAN, probeStimB = "JOT",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "ZUB < HEW < JOT, so ZUB < JOT."
        ))
        // comp_4: SAME + MORE_THAN
        trials.add(Trial(
            id = "comp_4",
            premises = listOf(
                Premise("RUF", RelationType.SAME, "WEX"),
                Premise("WEX", RelationType.MORE_THAN, "NAV")
            ),
            probeStimA = "RUF", probeRelation = RelationType.MORE_THAN, probeStimB = "NAV",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "RUF = WEX and WEX > NAV, so RUF > NAV."
        ))
        // comp_5: SAME + MORE wrong probe
        trials.add(Trial(
            id = "comp_5",
            premises = listOf(
                Premise("QIP", RelationType.SAME, "DUK"),
                Premise("DUK", RelationType.MORE_THAN, "FEZ")
            ),
            probeStimA = "QIP", probeRelation = RelationType.LESS_THAN, probeStimB = "FEZ",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "QIP = DUK and DUK > FEZ, so QIP > FEZ, not less than."
        ))
        // comp_6: 3-step MORE chain
        trials.add(Trial(
            id = "comp_6",
            premises = listOf(
                Premise("BGW", RelationType.MORE_THAN, "MOQ"),
                Premise("MOQ", RelationType.MORE_THAN, "TEL"),
                Premise("TEL", RelationType.MORE_THAN, "ZUB")
            ),
            probeStimA = "BGW", probeRelation = RelationType.MORE_THAN, probeStimB = "ZUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "BGW > MOQ > TEL > ZUB, so BGW > ZUB."
        ))
        // comp_7: 3-step MORE reversed
        trials.add(Trial(
            id = "comp_7",
            premises = listOf(
                Premise("HEW", RelationType.MORE_THAN, "JOT"),
                Premise("JOT", RelationType.MORE_THAN, "RUF"),
                Premise("RUF", RelationType.MORE_THAN, "WEX")
            ),
            probeStimA = "WEX", probeRelation = RelationType.MORE_THAN, probeStimB = "HEW",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "HEW > JOT > RUF > WEX, so WEX is LESS than HEW, not more."
        ))
        // comp_8: SAME + 2-step MORE
        trials.add(Trial(
            id = "comp_8",
            premises = listOf(
                Premise("NAV", RelationType.SAME, "QIP"),
                Premise("QIP", RelationType.MORE_THAN, "DUK"),
                Premise("DUK", RelationType.MORE_THAN, "FEZ")
            ),
            probeStimA = "NAV", probeRelation = RelationType.MORE_THAN, probeStimB = "FEZ",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "NAV = QIP > DUK > FEZ, so NAV > FEZ."
        ))
        // comp_9: 3-step LESS chain
        trials.add(Trial(
            id = "comp_9",
            premises = listOf(
                Premise("KES", RelationType.LESS_THAN, "DAX"),
                Premise("DAX", RelationType.LESS_THAN, "PIR"),
                Premise("PIR", RelationType.LESS_THAN, "AWX")
            ),
            probeStimA = "KES", probeRelation = RelationType.LESS_THAN, probeStimB = "AWX",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "KES < DAX < PIR < AWX, so KES < AWX."
        ))
        // comp_10: SAME + LESS chain
        trials.add(Trial(
            id = "comp_10",
            premises = listOf(
                Premise("TEL", RelationType.SAME, "ZUB"),
                Premise("ZUB", RelationType.LESS_THAN, "HEW"),
                Premise("HEW", RelationType.LESS_THAN, "JOT")
            ),
            probeStimA = "JOT", probeRelation = RelationType.MORE_THAN, probeStimB = "TEL",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "TEL = ZUB < HEW < JOT, so JOT > TEL."
        ))
        // comp_11: 4-step MORE chain
        trials.add(Trial(
            id = "comp_11",
            premises = listOf(
                Premise("AWX", RelationType.MORE_THAN, "EGC"),
                Premise("EGC", RelationType.MORE_THAN, "OPA"),
                Premise("OPA", RelationType.MORE_THAN, "BGW"),
                Premise("BGW", RelationType.MORE_THAN, "DUK")
            ),
            probeStimA = "AWX", probeRelation = RelationType.MORE_THAN, probeStimB = "DUK",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX > EGC > OPA > BGW > DUK, so AWX > DUK."
        ))
        // comp_12: 4-premise mixed SAME + MORE
        trials.add(Trial(
            id = "comp_12",
            premises = listOf(
                Premise("FEZ", RelationType.SAME, "LYR"),
                Premise("LYR", RelationType.MORE_THAN, "BOK"),
                Premise("BOK", RelationType.SAME, "CUB"),
                Premise("CUB", RelationType.MORE_THAN, "GYQ")
            ),
            probeStimA = "FEZ", probeRelation = RelationType.MORE_THAN, probeStimB = "GYQ",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "FEZ = LYR > BOK = CUB > GYQ, so FEZ > GYQ."
        ))

        // ============================================================
        // SUBSCALE 3: OPPOSITION (Opposite) - 10 items
        // ============================================================

        // opp_1: OPPOSITE symmetry
        trials.add(Trial(
            id = "opp_1",
            premises = listOf(Premise("LYR", RelationType.OPPOSITE, "BOK")),
            probeStimA = "BOK", probeRelation = RelationType.OPPOSITE, probeStimB = "LYR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "If LYR is opposite BOK, then BOK is opposite LYR (symmetry)."
        ))
        // opp_2: Double opposite = SAME
        trials.add(Trial(
            id = "opp_2",
            premises = listOf(
                Premise("LYR", RelationType.OPPOSITE, "BOK"),
                Premise("BOK", RelationType.OPPOSITE, "CUB")
            ),
            probeStimA = "LYR", probeRelation = RelationType.SAME, probeStimB = "CUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "LYR is opposite BOK, BOK is opposite CUB. Opposite of opposite = same, so LYR = CUB."
        ))
        // opp_3: SAME + OPPOSITE
        trials.add(Trial(
            id = "opp_3",
            premises = listOf(
                Premise("KES", RelationType.SAME, "DAX"),
                Premise("DAX", RelationType.OPPOSITE, "PIR")
            ),
            probeStimA = "KES", probeRelation = RelationType.OPPOSITE, probeStimB = "PIR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "KES = DAX and DAX is opposite PIR, so KES is opposite PIR."
        ))
        // opp_4: Double opposite, wrong probe
        trials.add(Trial(
            id = "opp_4",
            premises = listOf(
                Premise("GYQ", RelationType.OPPOSITE, "FYW"),
                Premise("FYW", RelationType.OPPOSITE, "VOP")
            ),
            probeStimA = "GYQ", probeRelation = RelationType.OPPOSITE, probeStimB = "VOP",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "Two opposites cancel out: GYQ is the SAME as VOP, not opposite."
        ))
        // opp_5: Triple opposite = opposite
        trials.add(Trial(
            id = "opp_5",
            premises = listOf(
                Premise("RUF", RelationType.OPPOSITE, "WEX"),
                Premise("WEX", RelationType.OPPOSITE, "NAV"),
                Premise("NAV", RelationType.OPPOSITE, "QIP")
            ),
            probeStimA = "RUF", probeRelation = RelationType.OPPOSITE, probeStimB = "QIP",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "Three opposites: odd number means RUF is opposite to QIP."
        ))
        // opp_6: Triple opposite wrong
        trials.add(Trial(
            id = "opp_6",
            premises = listOf(
                Premise("DUK", RelationType.OPPOSITE, "FEZ"),
                Premise("FEZ", RelationType.OPPOSITE, "HEW"),
                Premise("HEW", RelationType.OPPOSITE, "JOT")
            ),
            probeStimA = "DUK", probeRelation = RelationType.SAME, probeStimB = "JOT",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "Three opposites (odd): DUK is OPPOSITE to JOT, not same."
        ))
        // opp_7: OPPOSITE + SAME + MORE (cross-frame)
        trials.add(Trial(
            id = "opp_7",
            premises = listOf(
                Premise("LYR", RelationType.OPPOSITE, "BOK"),
                Premise("BOK", RelationType.SAME, "CUB"),
                Premise("CUB", RelationType.MORE_THAN, "FYW")
            ),
            probeStimA = "LYR", probeRelation = RelationType.LESS_THAN, probeStimB = "FYW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "BOK = CUB > FYW, so BOK > FYW. LYR is opposite BOK, so LYR < FYW."
        ))
        // opp_8: OPPOSITE + SAME + MORE wrong
        trials.add(Trial(
            id = "opp_8",
            premises = listOf(
                Premise("RUF", RelationType.OPPOSITE, "WEX"),
                Premise("WEX", RelationType.SAME, "NAV"),
                Premise("NAV", RelationType.MORE_THAN, "QIP")
            ),
            probeStimA = "RUF", probeRelation = RelationType.MORE_THAN, probeStimB = "QIP",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "WEX = NAV > QIP, so WEX > QIP. RUF is opposite WEX, so RUF < QIP, not more."
        ))
        // opp_9: Quadruple opposite = SAME
        trials.add(Trial(
            id = "opp_9",
            premises = listOf(
                Premise("AWX", RelationType.OPPOSITE, "EGC"),
                Premise("EGC", RelationType.OPPOSITE, "OPA"),
                Premise("OPA", RelationType.OPPOSITE, "BGW"),
                Premise("BGW", RelationType.OPPOSITE, "MOQ")
            ),
            probeStimA = "AWX", probeRelation = RelationType.SAME, probeStimB = "MOQ",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "Four opposites (even number): AWX is the SAME as MOQ."
        ))
        // opp_10: Quadruple opposite wrong
        trials.add(Trial(
            id = "opp_10",
            premises = listOf(
                Premise("TEL", RelationType.OPPOSITE, "ZUB"),
                Premise("ZUB", RelationType.OPPOSITE, "HEW"),
                Premise("HEW", RelationType.OPPOSITE, "JOT"),
                Premise("JOT", RelationType.OPPOSITE, "KES")
            ),
            probeStimA = "TEL", probeRelation = RelationType.OPPOSITE, probeStimB = "KES",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "Four opposites (even): TEL is the SAME as KES, not opposite."
        ))

        // ============================================================
        // SUBSCALE 4: TEMPORAL (Before/After) - 10 items
        // ============================================================

        // temp_1: BEFORE 2-premise
        trials.add(Trial(
            id = "temp_1",
            premises = listOf(
                Premise("AWX", RelationType.BEFORE, "EGC"),
                Premise("EGC", RelationType.BEFORE, "OPA")
            ),
            probeStimA = "AWX", probeRelation = RelationType.BEFORE, probeStimB = "OPA",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX before EGC, EGC before OPA, so AWX before OPA."
        ))
        // temp_2: BEFORE reversed
        trials.add(Trial(
            id = "temp_2",
            premises = listOf(
                Premise("BGW", RelationType.BEFORE, "MOQ"),
                Premise("MOQ", RelationType.BEFORE, "TEL")
            ),
            probeStimA = "TEL", probeRelation = RelationType.BEFORE, probeStimB = "BGW",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "BGW before MOQ before TEL, so TEL is AFTER BGW, not before."
        ))
        // temp_3: AFTER 2-premise
        trials.add(Trial(
            id = "temp_3",
            premises = listOf(
                Premise("ZUB", RelationType.AFTER, "HEW"),
                Premise("HEW", RelationType.AFTER, "JOT")
            ),
            probeStimA = "ZUB", probeRelation = RelationType.AFTER, probeStimB = "JOT",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "ZUB after HEW, HEW after JOT, so ZUB after JOT."
        ))
        // temp_4: BEFORE inverse (B before A -> A after B)
        trials.add(Trial(
            id = "temp_4",
            premises = listOf(
                Premise("RUF", RelationType.BEFORE, "WEX"),
                Premise("WEX", RelationType.BEFORE, "NAV")
            ),
            probeStimA = "NAV", probeRelation = RelationType.AFTER, probeStimB = "RUF",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "RUF before WEX before NAV, so NAV is after RUF."
        ))
        // temp_5: 3-step BEFORE
        trials.add(Trial(
            id = "temp_5",
            premises = listOf(
                Premise("QIP", RelationType.BEFORE, "DUK"),
                Premise("DUK", RelationType.BEFORE, "FEZ"),
                Premise("FEZ", RelationType.BEFORE, "LYR")
            ),
            probeStimA = "QIP", probeRelation = RelationType.BEFORE, probeStimB = "LYR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "QIP before DUK before FEZ before LYR, so QIP before LYR."
        ))
        // temp_6: 3-step BEFORE reversed
        trials.add(Trial(
            id = "temp_6",
            premises = listOf(
                Premise("EGC", RelationType.BEFORE, "OPA"),
                Premise("OPA", RelationType.BEFORE, "BGW"),
                Premise("BGW", RelationType.BEFORE, "MOQ")
            ),
            probeStimA = "MOQ", probeRelation = RelationType.BEFORE, probeStimB = "EGC",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "EGC before OPA before BGW before MOQ, so MOQ is AFTER EGC, not before."
        ))
        // temp_7: SAME + BEFORE
        trials.add(Trial(
            id = "temp_7",
            premises = listOf(
                Premise("GYQ", RelationType.SAME, "FYW"),
                Premise("FYW", RelationType.BEFORE, "VOP")
            ),
            probeStimA = "GYQ", probeRelation = RelationType.BEFORE, probeStimB = "VOP",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "GYQ = FYW and FYW before VOP, so GYQ before VOP."
        ))
        // temp_8: SAME + AFTER
        trials.add(Trial(
            id = "temp_8",
            premises = listOf(
                Premise("CUB", RelationType.SAME, "DAX"),
                Premise("DAX", RelationType.AFTER, "PIR")
            ),
            probeStimA = "CUB", probeRelation = RelationType.AFTER, probeStimB = "PIR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "CUB = DAX and DAX after PIR, so CUB after PIR."
        ))
        // temp_9: 4-step BEFORE
        trials.add(Trial(
            id = "temp_9",
            premises = listOf(
                Premise("BOK", RelationType.BEFORE, "CUB"),
                Premise("CUB", RelationType.BEFORE, "TEL"),
                Premise("TEL", RelationType.BEFORE, "ZUB"),
                Premise("ZUB", RelationType.BEFORE, "HEW")
            ),
            probeStimA = "BOK", probeRelation = RelationType.BEFORE, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "BOK before CUB before TEL before ZUB before HEW, so BOK before HEW."
        ))
        // temp_10: 4-step AFTER reversed
        trials.add(Trial(
            id = "temp_10",
            premises = listOf(
                Premise("JOT", RelationType.AFTER, "RUF"),
                Premise("RUF", RelationType.AFTER, "WEX"),
                Premise("WEX", RelationType.AFTER, "NAV"),
                Premise("NAV", RelationType.AFTER, "QIP")
            ),
            probeStimA = "QIP", probeRelation = RelationType.AFTER, probeStimB = "JOT",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "JOT after RUF after WEX after NAV after QIP. QIP is BEFORE JOT, not after."
        ))

        // ============================================================
        // SUBSCALE 5: CONTAINMENT (Contains/Within) - 8 items
        // ============================================================

        // cont_1: Basic containment
        trials.add(Trial(
            id = "cont_1",
            premises = listOf(Premise("AWX", RelationType.CONTAINS, "EGC")),
            probeStimA = "EGC", probeRelation = RelationType.WITHIN, probeStimB = "AWX",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX contains EGC, so EGC is within AWX."
        ))
        // cont_2: Containment wrong direction
        trials.add(Trial(
            id = "cont_2",
            premises = listOf(Premise("BGW", RelationType.CONTAINS, "MOQ")),
            probeStimA = "MOQ", probeRelation = RelationType.CONTAINS, probeStimB = "BGW",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "BGW contains MOQ, not the other way around."
        ))
        // cont_3: 2-premise containment
        trials.add(Trial(
            id = "cont_3",
            premises = listOf(
                Premise("TEL", RelationType.CONTAINS, "ZUB"),
                Premise("ZUB", RelationType.CONTAINS, "HEW")
            ),
            probeStimA = "TEL", probeRelation = RelationType.CONTAINS, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "TEL contains ZUB, ZUB contains HEW, so TEL contains HEW (transitive)."
        ))
        // cont_4: 2-premise containment reversed
        trials.add(Trial(
            id = "cont_4",
            premises = listOf(
                Premise("JOT", RelationType.CONTAINS, "RUF"),
                Premise("RUF", RelationType.CONTAINS, "WEX")
            ),
            probeStimA = "WEX", probeRelation = RelationType.CONTAINS, probeStimB = "JOT",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "JOT contains RUF contains WEX. WEX is WITHIN JOT, not contains."
        ))
        // cont_5: SAME + containment
        trials.add(Trial(
            id = "cont_5",
            premises = listOf(
                Premise("NAV", RelationType.SAME, "QIP"),
                Premise("QIP", RelationType.CONTAINS, "DUK")
            ),
            probeStimA = "NAV", probeRelation = RelationType.CONTAINS, probeStimB = "DUK",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "NAV = QIP and QIP contains DUK, so NAV contains DUK."
        ))
        // cont_6: 3-premise containment
        trials.add(Trial(
            id = "cont_6",
            premises = listOf(
                Premise("FEZ", RelationType.CONTAINS, "LYR"),
                Premise("LYR", RelationType.CONTAINS, "BOK"),
                Premise("BOK", RelationType.CONTAINS, "CUB")
            ),
            probeStimA = "FEZ", probeRelation = RelationType.CONTAINS, probeStimB = "CUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "FEZ contains LYR contains BOK contains CUB, so FEZ contains CUB."
        ))
        // cont_7: 3-premise within
        trials.add(Trial(
            id = "cont_7",
            premises = listOf(
                Premise("GYQ", RelationType.WITHIN, "FYW"),
                Premise("FYW", RelationType.WITHIN, "VOP"),
                Premise("VOP", RelationType.WITHIN, "KES")
            ),
            probeStimA = "GYQ", probeRelation = RelationType.WITHIN, probeStimB = "KES",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "GYQ within FYW within VOP within KES, so GYQ is within KES."
        ))
        // cont_8: 3-premise containment wrong
        trials.add(Trial(
            id = "cont_8",
            premises = listOf(
                Premise("DAX", RelationType.CONTAINS, "PIR"),
                Premise("PIR", RelationType.CONTAINS, "OPA"),
                Premise("OPA", RelationType.CONTAINS, "BGW")
            ),
            probeStimA = "BGW", probeRelation = RelationType.CONTAINS, probeStimB = "DAX",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "DAX contains PIR contains OPA contains BGW. BGW is WITHIN DAX, not contains."
        ))

        // ============================================================
        // SUBSCALE 6: MIXED / COMBINED - 12 items
        // ============================================================

        // mix_1: SAME + MORE_THAN
        trials.add(Trial(
            id = "mix_1",
            premises = listOf(
                Premise("GYQ", RelationType.SAME, "FYW"),
                Premise("FYW", RelationType.MORE_THAN, "VOP")
            ),
            probeStimA = "GYQ", probeRelation = RelationType.MORE_THAN, probeStimB = "VOP",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "GYQ = FYW > VOP, so GYQ > VOP."
        ))
        // mix_2: SAME + LESS_THAN
        trials.add(Trial(
            id = "mix_2",
            premises = listOf(
                Premise("KES", RelationType.SAME, "DAX"),
                Premise("DAX", RelationType.LESS_THAN, "PIR")
            ),
            probeStimA = "KES", probeRelation = RelationType.LESS_THAN, probeStimB = "PIR",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "KES = DAX < PIR, so KES < PIR."
        ))
        // mix_3: SAME + BEFORE
        trials.add(Trial(
            id = "mix_3",
            premises = listOf(
                Premise("MOQ", RelationType.SAME, "TEL"),
                Premise("TEL", RelationType.BEFORE, "ZUB")
            ),
            probeStimA = "MOQ", probeRelation = RelationType.BEFORE, probeStimB = "ZUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "MOQ = TEL and TEL before ZUB, so MOQ before ZUB."
        ))
        // mix_4: SAME + CONTAINS
        trials.add(Trial(
            id = "mix_4",
            premises = listOf(
                Premise("HEW", RelationType.SAME, "JOT"),
                Premise("JOT", RelationType.CONTAINS, "RUF")
            ),
            probeStimA = "HEW", probeRelation = RelationType.CONTAINS, probeStimB = "RUF",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "HEW = JOT and JOT contains RUF, so HEW contains RUF."
        ))
        // mix_5: OPPOSITE + MORE (cross-frame 3 premise)
        trials.add(Trial(
            id = "mix_5",
            premises = listOf(
                Premise("WEX", RelationType.OPPOSITE, "NAV"),
                Premise("NAV", RelationType.SAME, "QIP"),
                Premise("QIP", RelationType.MORE_THAN, "DUK")
            ),
            probeStimA = "WEX", probeRelation = RelationType.MORE_THAN, probeStimB = "DUK",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "NAV = QIP > DUK, so NAV > DUK. WEX is opposite NAV, so WEX < DUK, not more."
        ))
        // mix_6: SAME + MORE + SAME (3 premise)
        trials.add(Trial(
            id = "mix_6",
            premises = listOf(
                Premise("FEZ", RelationType.SAME, "LYR"),
                Premise("LYR", RelationType.MORE_THAN, "BOK"),
                Premise("BOK", RelationType.SAME, "CUB")
            ),
            probeStimA = "FEZ", probeRelation = RelationType.MORE_THAN, probeStimB = "CUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "FEZ = LYR > BOK = CUB, so FEZ > CUB."
        ))
        // mix_7: BEFORE + SAME + BEFORE (3 premise)
        trials.add(Trial(
            id = "mix_7",
            premises = listOf(
                Premise("AWX", RelationType.BEFORE, "EGC"),
                Premise("EGC", RelationType.SAME, "OPA"),
                Premise("OPA", RelationType.BEFORE, "BGW")
            ),
            probeStimA = "AWX", probeRelation = RelationType.BEFORE, probeStimB = "BGW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX before EGC = OPA before BGW, so AWX before BGW."
        ))
        // mix_8: CONTAINS + SAME + CONTAINS (3 premise)
        trials.add(Trial(
            id = "mix_8",
            premises = listOf(
                Premise("MOQ", RelationType.CONTAINS, "TEL"),
                Premise("TEL", RelationType.SAME, "ZUB"),
                Premise("ZUB", RelationType.CONTAINS, "HEW")
            ),
            probeStimA = "MOQ", probeRelation = RelationType.CONTAINS, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "MOQ contains TEL = ZUB, ZUB contains HEW, so MOQ contains HEW."
        ))
        // mix_9: 4-premise SAME + MORE + SAME + MORE
        trials.add(Trial(
            id = "mix_9",
            premises = listOf(
                Premise("JOT", RelationType.SAME, "RUF"),
                Premise("RUF", RelationType.MORE_THAN, "WEX"),
                Premise("WEX", RelationType.SAME, "NAV"),
                Premise("NAV", RelationType.MORE_THAN, "QIP")
            ),
            probeStimA = "JOT", probeRelation = RelationType.MORE_THAN, probeStimB = "QIP",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "JOT = RUF > WEX = NAV > QIP, so JOT > QIP."
        ))
        // mix_10: 4-premise BEFORE chain with SAME
        trials.add(Trial(
            id = "mix_10",
            premises = listOf(
                Premise("DUK", RelationType.BEFORE, "FEZ"),
                Premise("FEZ", RelationType.SAME, "LYR"),
                Premise("LYR", RelationType.BEFORE, "BOK"),
                Premise("BOK", RelationType.SAME, "CUB")
            ),
            probeStimA = "DUK", probeRelation = RelationType.BEFORE, probeStimB = "CUB",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "DUK before FEZ = LYR before BOK = CUB, so DUK before CUB."
        ))
        // mix_11: 3-premise cross-frame wrong
        trials.add(Trial(
            id = "mix_11",
            premises = listOf(
                Premise("GYQ", RelationType.SAME, "FYW"),
                Premise("FYW", RelationType.MORE_THAN, "VOP"),
                Premise("VOP", RelationType.MORE_THAN, "KES")
            ),
            probeStimA = "KES", probeRelation = RelationType.MORE_THAN, probeStimB = "GYQ",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "GYQ = FYW > VOP > KES, so KES is LESS than GYQ, not more."
        ))
        // mix_12: 3-premise temporal reversed
        trials.add(Trial(
            id = "mix_12",
            premises = listOf(
                Premise("DAX", RelationType.SAME, "PIR"),
                Premise("PIR", RelationType.BEFORE, "OPA"),
                Premise("OPA", RelationType.BEFORE, "BGW")
            ),
            probeStimA = "BGW", probeRelation = RelationType.BEFORE, probeStimB = "DAX",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "DAX = PIR before OPA before BGW. BGW is AFTER DAX, not before."
        ))

        // ============================================================
        // SUBSCALE 7: ANALOGY / ADVANCED - 8 items
        // ============================================================

        // anal_1: SAME + OPPOSITE + SAME
        trials.add(Trial(
            id = "anal_1",
            premises = listOf(
                Premise("AWX", RelationType.SAME, "EGC"),
                Premise("EGC", RelationType.OPPOSITE, "OPA"),
                Premise("OPA", RelationType.SAME, "BGW")
            ),
            probeStimA = "AWX", probeRelation = RelationType.OPPOSITE, probeStimB = "BGW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "AWX = EGC opposite OPA = BGW. AWX is opposite BGW."
        ))
        // anal_2: OPPOSITE + SAME + OPPOSITE
        trials.add(Trial(
            id = "anal_2",
            premises = listOf(
                Premise("MOQ", RelationType.OPPOSITE, "TEL"),
                Premise("TEL", RelationType.SAME, "ZUB"),
                Premise("ZUB", RelationType.OPPOSITE, "HEW")
            ),
            probeStimA = "MOQ", probeRelation = RelationType.SAME, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "MOQ opposite TEL = ZUB opposite HEW. Two opposites = same, so MOQ = HEW."
        ))
        // anal_3: OPPOSITE + OPPOSITE + SAME wrong
        trials.add(Trial(
            id = "anal_3",
            premises = listOf(
                Premise("JOT", RelationType.OPPOSITE, "RUF"),
                Premise("RUF", RelationType.OPPOSITE, "WEX"),
                Premise("WEX", RelationType.SAME, "NAV")
            ),
            probeStimA = "JOT", probeRelation = RelationType.OPPOSITE, probeStimB = "NAV",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "JOT opp RUF opp WEX = NAV. Two opposites cancel: JOT is the SAME as NAV, not opposite."
        ))
        // anal_4: SAME + OPPOSITE + MORE
        trials.add(Trial(
            id = "anal_4",
            premises = listOf(
                Premise("QIP", RelationType.SAME, "DUK"),
                Premise("DUK", RelationType.OPPOSITE, "FEZ"),
                Premise("FEZ", RelationType.MORE_THAN, "LYR")
            ),
            probeStimA = "QIP", probeRelation = RelationType.MORE_THAN, probeStimB = "LYR",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "QIP = DUK, DUK opposite FEZ. FEZ > LYR but QIP is opposite to FEZ, so QIP < LYR."
        ))
        // anal_5: MORE + SAME + OPPOSITE
        trials.add(Trial(
            id = "anal_5",
            premises = listOf(
                Premise("BOK", RelationType.MORE_THAN, "CUB"),
                Premise("CUB", RelationType.SAME, "GYQ"),
                Premise("GYQ", RelationType.OPPOSITE, "FYW")
            ),
            probeStimA = "BOK", probeRelation = RelationType.LESS_THAN, probeStimB = "FYW",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "BOK > CUB = GYQ, GYQ opposite FYW. BOK > GYQ and GYQ opposite FYW, so BOK < FYW is incorrect."
        ))
        // anal_6: 4-premise SAME + OPP + SAME + MORE
        trials.add(Trial(
            id = "anal_6",
            premises = listOf(
                Premise("VOP", RelationType.SAME, "KES"),
                Premise("KES", RelationType.OPPOSITE, "DAX"),
                Premise("DAX", RelationType.SAME, "PIR"),
                Premise("PIR", RelationType.MORE_THAN, "OPA")
            ),
            probeStimA = "VOP", probeRelation = RelationType.LESS_THAN, probeStimB = "OPA",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "VOP = KES opposite DAX = PIR > OPA. VOP is opposite to PIR, so VOP < OPA."
        ))
        // anal_7: 4-premise complex
        trials.add(Trial(
            id = "anal_7",
            premises = listOf(
                Premise("BGW", RelationType.OPPOSITE, "MOQ"),
                Premise("MOQ", RelationType.SAME, "TEL"),
                Premise("TEL", RelationType.OPPOSITE, "ZUB"),
                Premise("ZUB", RelationType.SAME, "HEW")
            ),
            probeStimA = "BGW", probeRelation = RelationType.SAME, probeStimB = "HEW",
            correctAnswer = true, timeLimitSeconds = 30,
            explanation = "BGW opp MOQ = TEL opp ZUB = HEW. Two opposites cancel: BGW = HEW."
        ))
        // anal_8: 4-premise complex wrong
        trials.add(Trial(
            id = "anal_8",
            premises = listOf(
                Premise("JOT", RelationType.OPPOSITE, "RUF"),
                Premise("RUF", RelationType.SAME, "WEX"),
                Premise("WEX", RelationType.OPPOSITE, "NAV"),
                Premise("NAV", RelationType.SAME, "QIP")
            ),
            probeStimA = "JOT", probeRelation = RelationType.OPPOSITE, probeStimB = "QIP",
            correctAnswer = false, timeLimitSeconds = 30,
            explanation = "JOT opp RUF = WEX opp NAV = QIP. Two opposites cancel: JOT = QIP, not opposite."
        ))

        return trials
    }
}
