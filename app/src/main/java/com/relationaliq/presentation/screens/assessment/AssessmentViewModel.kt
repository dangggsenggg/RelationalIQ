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
        // All assessment trials use multi-premise chains.
        // The user must derive a relation between non-adjacent stimuli.
        return listOf(
            // ---- EASY: 2-premise chains ----

            // Q1: SAME transitivity: GYQ=FYW, FYW=VOP → GYQ=VOP? YES
            Trial(
                id = "a1",
                premises = listOf(
                    Premise("GYQ", RelationType.SAME, "FYW"),
                    Premise("FYW", RelationType.SAME, "VOP")
                ),
                probeStimA = "GYQ", probeRelation = RelationType.SAME, probeStimB = "VOP",
                correctAnswer = true,
                explanation = "GYQ = FYW and FYW = VOP, so by transitivity GYQ = VOP."
            ),
            // Q2: SAME transitivity, wrong probe: KES=DAX, DAX=PIR → KES≠PIR? NO
            Trial(
                id = "a2",
                premises = listOf(
                    Premise("KES", RelationType.SAME, "DAX"),
                    Premise("DAX", RelationType.SAME, "PIR")
                ),
                probeStimA = "KES", probeRelation = RelationType.DIFFERENT, probeStimB = "PIR",
                correctAnswer = false,
                explanation = "KES = DAX = PIR, so KES is the SAME as PIR, not different."
            ),
            // Q3: MORE_THAN transitivity: AWX>EGC, EGC>OPA → AWX>OPA? YES
            Trial(
                id = "a3",
                premises = listOf(
                    Premise("AWX", RelationType.MORE_THAN, "EGC"),
                    Premise("EGC", RelationType.MORE_THAN, "OPA")
                ),
                probeStimA = "AWX", probeRelation = RelationType.MORE_THAN, probeStimB = "OPA",
                correctAnswer = true,
                explanation = "AWX > EGC > OPA, so AWX > OPA."
            ),
            // Q4: MORE_THAN reversed: BGW>MOQ, MOQ>TEL → TEL>BGW? NO
            Trial(
                id = "a4",
                premises = listOf(
                    Premise("BGW", RelationType.MORE_THAN, "MOQ"),
                    Premise("MOQ", RelationType.MORE_THAN, "TEL")
                ),
                probeStimA = "TEL", probeRelation = RelationType.MORE_THAN, probeStimB = "BGW",
                correctAnswer = false,
                explanation = "BGW > MOQ > TEL, so TEL is LESS than BGW, not more."
            ),
            // Q5: LESS_THAN transitivity: ZUB<HEW, HEW<JOT → ZUB<JOT? YES
            Trial(
                id = "a5",
                premises = listOf(
                    Premise("ZUB", RelationType.LESS_THAN, "HEW"),
                    Premise("HEW", RelationType.LESS_THAN, "JOT")
                ),
                probeStimA = "ZUB", probeRelation = RelationType.LESS_THAN, probeStimB = "JOT",
                correctAnswer = true,
                explanation = "ZUB < HEW < JOT, so ZUB < JOT."
            ),

            // ---- MEDIUM: 2-premise mixed-relation chains ----

            // Q6: SAME + MORE: RUF=WEX, WEX>NAV → RUF>NAV? YES
            Trial(
                id = "a6",
                premises = listOf(
                    Premise("RUF", RelationType.SAME, "WEX"),
                    Premise("WEX", RelationType.MORE_THAN, "NAV")
                ),
                probeStimA = "RUF", probeRelation = RelationType.MORE_THAN, probeStimB = "NAV",
                correctAnswer = true,
                explanation = "RUF = WEX and WEX > NAV, so RUF > NAV."
            ),
            // Q7: SAME + MORE, wrong probe: QIP=DUK, DUK>FEZ → QIP<FEZ? NO
            Trial(
                id = "a7",
                premises = listOf(
                    Premise("QIP", RelationType.SAME, "DUK"),
                    Premise("DUK", RelationType.MORE_THAN, "FEZ")
                ),
                probeStimA = "QIP", probeRelation = RelationType.LESS_THAN, probeStimB = "FEZ",
                correctAnswer = false,
                explanation = "QIP = DUK and DUK > FEZ, so QIP > FEZ, not less than."
            ),
            // Q8: OPPOSITE of OPPOSITE = SAME: LYR↔BOK, BOK↔CUB → LYR=CUB? YES
            Trial(
                id = "a8",
                premises = listOf(
                    Premise("LYR", RelationType.OPPOSITE, "BOK"),
                    Premise("BOK", RelationType.OPPOSITE, "CUB")
                ),
                probeStimA = "LYR", probeRelation = RelationType.SAME, probeStimB = "CUB",
                correctAnswer = true,
                explanation = "LYR is opposite BOK, and BOK is opposite CUB. Opposite of opposite = same, so LYR = CUB."
            ),
            // Q9: SAME + OPPOSITE: KES=DAX, DAX↔PIR → KES↔PIR? YES
            Trial(
                id = "a9",
                premises = listOf(
                    Premise("KES", RelationType.SAME, "DAX"),
                    Premise("DAX", RelationType.OPPOSITE, "PIR")
                ),
                probeStimA = "KES", probeRelation = RelationType.OPPOSITE, probeStimB = "PIR",
                correctAnswer = true,
                explanation = "KES = DAX and DAX is opposite PIR, so KES is opposite PIR."
            ),
            // Q10: BEFORE transitivity: AWX→EGC, EGC→OPA → AWX before OPA? YES
            Trial(
                id = "a10",
                premises = listOf(
                    Premise("AWX", RelationType.BEFORE, "EGC"),
                    Premise("EGC", RelationType.BEFORE, "OPA")
                ),
                probeStimA = "AWX", probeRelation = RelationType.BEFORE, probeStimB = "OPA",
                correctAnswer = true,
                explanation = "AWX comes before EGC, and EGC comes before OPA, so AWX comes before OPA."
            ),

            // ---- HARD: 3-premise chains ----

            // Q11: 3-step MORE chain: BGW>MOQ, MOQ>TEL, TEL>ZUB → BGW>ZUB? YES
            Trial(
                id = "a11",
                premises = listOf(
                    Premise("BGW", RelationType.MORE_THAN, "MOQ"),
                    Premise("MOQ", RelationType.MORE_THAN, "TEL"),
                    Premise("TEL", RelationType.MORE_THAN, "ZUB")
                ),
                probeStimA = "BGW", probeRelation = RelationType.MORE_THAN, probeStimB = "ZUB",
                correctAnswer = true,
                explanation = "BGW > MOQ > TEL > ZUB, so BGW > ZUB."
            ),
            // Q12: 3-step MORE reversed: HEW>JOT, JOT>RUF, RUF>WEX → WEX>HEW? NO
            Trial(
                id = "a12",
                premises = listOf(
                    Premise("HEW", RelationType.MORE_THAN, "JOT"),
                    Premise("JOT", RelationType.MORE_THAN, "RUF"),
                    Premise("RUF", RelationType.MORE_THAN, "WEX")
                ),
                probeStimA = "WEX", probeRelation = RelationType.MORE_THAN, probeStimB = "HEW",
                correctAnswer = false,
                explanation = "HEW > JOT > RUF > WEX, so WEX is LESS than HEW, not more."
            ),
            // Q13: SAME + 2-step MORE: NAV=QIP, QIP>DUK, DUK>FEZ → NAV>FEZ? YES
            Trial(
                id = "a13",
                premises = listOf(
                    Premise("NAV", RelationType.SAME, "QIP"),
                    Premise("QIP", RelationType.MORE_THAN, "DUK"),
                    Premise("DUK", RelationType.MORE_THAN, "FEZ")
                ),
                probeStimA = "NAV", probeRelation = RelationType.MORE_THAN, probeStimB = "FEZ",
                correctAnswer = true,
                explanation = "NAV = QIP > DUK > FEZ, so NAV > FEZ."
            ),
            // Q14: OPPOSITE + SAME + MORE: LYR↔BOK, BOK=CUB, CUB>FYW → LYR<FYW? YES
            Trial(
                id = "a14",
                premises = listOf(
                    Premise("LYR", RelationType.OPPOSITE, "BOK"),
                    Premise("BOK", RelationType.SAME, "CUB"),
                    Premise("CUB", RelationType.MORE_THAN, "FYW")
                ),
                probeStimA = "LYR", probeRelation = RelationType.LESS_THAN, probeStimB = "FYW",
                correctAnswer = true,
                explanation = "BOK = CUB > FYW, so BOK > FYW. LYR is opposite BOK, so LYR < FYW."
            ),
            // Q15: 3-step LESS chain: KES<DAX, DAX<PIR, PIR<AWX → KES<AWX? YES
            Trial(
                id = "a15",
                premises = listOf(
                    Premise("KES", RelationType.LESS_THAN, "DAX"),
                    Premise("DAX", RelationType.LESS_THAN, "PIR"),
                    Premise("PIR", RelationType.LESS_THAN, "AWX")
                ),
                probeStimA = "KES", probeRelation = RelationType.LESS_THAN, probeStimB = "AWX",
                correctAnswer = true,
                explanation = "KES < DAX < PIR < AWX, so KES < AWX."
            ),

            // ---- VERY HARD: 3-4 premise complex chains ----

            // Q16: 3-step BEFORE reversed: EGC→OPA, OPA→BGW, BGW→MOQ → MOQ before EGC? NO
            Trial(
                id = "a16",
                premises = listOf(
                    Premise("EGC", RelationType.BEFORE, "OPA"),
                    Premise("OPA", RelationType.BEFORE, "BGW"),
                    Premise("BGW", RelationType.BEFORE, "MOQ")
                ),
                probeStimA = "MOQ", probeRelation = RelationType.BEFORE, probeStimB = "EGC",
                correctAnswer = false,
                explanation = "EGC → OPA → BGW → MOQ, so MOQ comes AFTER EGC, not before."
            ),
            // Q17: SAME + LESS chain: TEL=ZUB, ZUB<HEW, HEW<JOT → JOT>TEL? YES
            Trial(
                id = "a17",
                premises = listOf(
                    Premise("TEL", RelationType.SAME, "ZUB"),
                    Premise("ZUB", RelationType.LESS_THAN, "HEW"),
                    Premise("HEW", RelationType.LESS_THAN, "JOT")
                ),
                probeStimA = "JOT", probeRelation = RelationType.MORE_THAN, probeStimB = "TEL",
                correctAnswer = true,
                explanation = "TEL = ZUB < HEW < JOT, so JOT > TEL."
            ),
            // Q18: OPPOSITE + SAME + MORE: RUF↔WEX, WEX=NAV, NAV>QIP → RUF>QIP? NO
            Trial(
                id = "a18",
                premises = listOf(
                    Premise("RUF", RelationType.OPPOSITE, "WEX"),
                    Premise("WEX", RelationType.SAME, "NAV"),
                    Premise("NAV", RelationType.MORE_THAN, "QIP")
                ),
                probeStimA = "RUF", probeRelation = RelationType.MORE_THAN, probeStimB = "QIP",
                correctAnswer = false,
                explanation = "WEX = NAV > QIP, so WEX > QIP. RUF is opposite WEX, so RUF < QIP, not more."
            ),
            // Q19: 4-step MORE chain: AWX>EGC, EGC>OPA, OPA>BGW, BGW>DUK → AWX>DUK? YES
            Trial(
                id = "a19",
                premises = listOf(
                    Premise("AWX", RelationType.MORE_THAN, "EGC"),
                    Premise("EGC", RelationType.MORE_THAN, "OPA"),
                    Premise("OPA", RelationType.MORE_THAN, "BGW"),
                    Premise("BGW", RelationType.MORE_THAN, "DUK")
                ),
                probeStimA = "AWX", probeRelation = RelationType.MORE_THAN, probeStimB = "DUK",
                correctAnswer = true,
                explanation = "AWX > EGC > OPA > BGW > DUK, so AWX > DUK."
            ),
            // Q20: 4-premise mixed: FEZ=LYR, LYR>BOK, BOK=CUB, CUB>GYQ → FEZ>GYQ? YES
            Trial(
                id = "a20",
                premises = listOf(
                    Premise("FEZ", RelationType.SAME, "LYR"),
                    Premise("LYR", RelationType.MORE_THAN, "BOK"),
                    Premise("BOK", RelationType.SAME, "CUB"),
                    Premise("CUB", RelationType.MORE_THAN, "GYQ")
                ),
                probeStimA = "FEZ", probeRelation = RelationType.MORE_THAN, probeStimB = "GYQ",
                correctAnswer = true,
                explanation = "FEZ = LYR > BOK = CUB > GYQ, so FEZ > GYQ."
            )
        )
    }
}
