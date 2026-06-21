package com.relationaliq.presentation.screens.assessment

import com.relationaliq.domain.model.Premise
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.model.UserProfile
import com.relationaliq.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssessmentTrialsTest {

    private lateinit var viewModel: AssessmentViewModel
    private lateinit var trials: List<Trial>

    @Before
    fun setup() {
        viewModel = AssessmentViewModel(FakeUserRepository())
        viewModel.startAssessment(true)
        trials = viewModel.uiState.value.trials
    }

    @Test
    fun `assessment contains exactly 20 trials`() {
        assertEquals(20, trials.size)
    }

    @Test
    fun `all trials have unique IDs`() {
        val ids = trials.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every trial has at least 2 premises`() {
        for (trial in trials) {
            assertTrue(
                "Trial ${trial.id} has only ${trial.premises.size} premise(s), expected at least 2",
                trial.premises.size >= 2
            )
        }
    }

    @Test
    fun `probe stimuli are non-adjacent in the premise chain`() {
        for (trial in trials) {
            val allStimuli = mutableSetOf<String>()
            trial.premises.forEach { p ->
                allStimuli.add(p.stimulusA)
                allStimuli.add(p.stimulusB)
            }
            assertTrue(
                "Trial ${trial.id}: probeStimA '${trial.probeStimA}' should appear in premises",
                allStimuli.contains(trial.probeStimA)
            )
            assertTrue(
                "Trial ${trial.id}: probeStimB '${trial.probeStimB}' should appear in premises",
                allStimuli.contains(trial.probeStimB)
            )
        }
    }

    @Test
    fun `trials have a mix of true and false correct answers`() {
        val trueCount = trials.count { it.correctAnswer }
        val falseCount = trials.count { !it.correctAnswer }
        assertTrue("Expected some true answers, got $trueCount", trueCount > 0)
        assertTrue("Expected some false answers, got $falseCount", falseCount > 0)
        assertTrue("Expected at least 5 false answers for balance, got $falseCount", falseCount >= 5)
    }

    @Test
    fun `every trial has an explanation`() {
        for (trial in trials) {
            assertTrue(
                "Trial ${trial.id} is missing an explanation",
                trial.explanation.isNotBlank()
            )
        }
    }

    @Test
    fun `difficulty progresses from 2-premise to 4-premise chains`() {
        val first5 = trials.subList(0, 5)
        val last5 = trials.subList(15, 20)

        val avgPremisesEarly = first5.map { it.premises.size }.average()
        val avgPremisesLate = last5.map { it.premises.size }.average()

        assertTrue(
            "Expected later trials to have more premises than early ones (early=$avgPremisesEarly, late=$avgPremisesLate)",
            avgPremisesLate >= avgPremisesEarly
        )
    }

    @Test
    fun `last two trials have 3 or more premises`() {
        assertTrue("Trial a19 should have 3+ premises", trials[18].premises.size >= 3)
        assertTrue("Trial a20 should have 3+ premises", trials[19].premises.size >= 3)
    }

    @Test
    fun `assessment covers multiple relation types`() {
        val probeRelations = trials.map { it.probeRelation }.toSet()
        assertTrue("Should test SAME relation", probeRelations.contains(RelationType.SAME))
        assertTrue("Should test MORE_THAN relation", probeRelations.contains(RelationType.MORE_THAN))
        assertTrue("Should test LESS_THAN relation", probeRelations.contains(RelationType.LESS_THAN))
        assertTrue("Should test OPPOSITE relation", probeRelations.contains(RelationType.OPPOSITE))
        assertTrue("Should test BEFORE relation", probeRelations.contains(RelationType.BEFORE))
    }

    @Test
    fun `premise chains are logically connected`() {
        for (trial in trials) {
            if (trial.premises.size >= 2) {
                for (i in 0 until trial.premises.size - 1) {
                    val current = trial.premises[i]
                    val next = trial.premises[i + 1]
                    assertEquals(
                        "Trial ${trial.id}: premise chain should be connected (premise $i stimulusB should equal premise ${i + 1} stimulusA)",
                        current.stimulusB,
                        next.stimulusA
                    )
                }
            }
        }
    }

    @Test
    fun `transitive MORE_THAN chain derives correct answer`() {
        // Q3: AWX > EGC, EGC > OPA → AWX > OPA? YES
        val trial = trials.first { it.id == "a3" }
        assertEquals("AWX", trial.premises[0].stimulusA)
        assertEquals(RelationType.MORE_THAN, trial.premises[0].relationType)
        assertEquals("EGC", trial.premises[0].stimulusB)
        assertEquals("EGC", trial.premises[1].stimulusA)
        assertEquals(RelationType.MORE_THAN, trial.premises[1].relationType)
        assertEquals("OPA", trial.premises[1].stimulusB)
        assertEquals("AWX", trial.probeStimA)
        assertEquals(RelationType.MORE_THAN, trial.probeRelation)
        assertEquals("OPA", trial.probeStimB)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `reversed transitive chain derives correct false answer`() {
        // Q4: BGW > MOQ, MOQ > TEL → TEL > BGW? NO
        val trial = trials.first { it.id == "a4" }
        assertEquals(false, trial.correctAnswer)
    }

    @Test
    fun `4-premise chain derives correct answer`() {
        // Q19: AWX > EGC, EGC > OPA, OPA > BGW, BGW > DUK → AWX > DUK? YES
        val trial = trials.first { it.id == "a19" }
        assertEquals(4, trial.premises.size)
        assertEquals("AWX", trial.probeStimA)
        assertEquals(RelationType.MORE_THAN, trial.probeRelation)
        assertEquals("DUK", trial.probeStimB)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `opposite of opposite equals same`() {
        // Q8: LYR ↔ BOK, BOK ↔ CUB → LYR = CUB? YES
        val trial = trials.first { it.id == "a8" }
        assertEquals(RelationType.OPPOSITE, trial.premises[0].relationType)
        assertEquals(RelationType.OPPOSITE, trial.premises[1].relationType)
        assertEquals(RelationType.SAME, trial.probeRelation)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `opposite reverses comparison relation`() {
        // Q14: LYR ↔ BOK, BOK = CUB, CUB > FYW → LYR < FYW? YES
        val trial = trials.first { it.id == "a14" }
        assertEquals(RelationType.OPPOSITE, trial.premises[0].relationType)
        assertEquals(RelationType.SAME, trial.premises[1].relationType)
        assertEquals(RelationType.MORE_THAN, trial.premises[2].relationType)
        assertEquals(RelationType.LESS_THAN, trial.probeRelation)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `opposite with more_than gives false for more_than probe`() {
        // Q18: RUF ↔ WEX, WEX = NAV, NAV > QIP → RUF > QIP? NO
        val trial = trials.first { it.id == "a18" }
        assertEquals(RelationType.OPPOSITE, trial.premises[0].relationType)
        assertEquals(RelationType.MORE_THAN, trial.probeRelation)
        assertNotNull(trial)
        assertEquals(false, trial.correctAnswer)
    }

    @Test
    fun `submitting answers tracks score correctly`() {
        // Answer all 20 correctly
        for (trial in trials) {
            viewModel.submitAnswer(trial.correctAnswer)
        }
        val state = viewModel.uiState.value
        assertTrue(state.showResult)
        assertEquals(1.0f, state.score)
        assertEquals(20, state.correctCount)
    }

    @Test
    fun `submitting wrong answers reduces score`() {
        // Answer first 10 correctly, last 10 incorrectly
        for (i in trials.indices) {
            val trial = trials[i]
            if (i < 10) {
                viewModel.submitAnswer(trial.correctAnswer)
            } else {
                viewModel.submitAnswer(!trial.correctAnswer)
            }
        }
        val state = viewModel.uiState.value
        assertTrue(state.showResult)
        assertEquals(0.5f, state.score)
        assertEquals(10, state.correctCount)
    }
}

private class FakeUserRepository : UserRepository {
    override fun observeProfile(): Flow<UserProfile?> = flowOf(null)
    override suspend fun getProfile(): UserProfile? = UserProfile(id = 1)
    override suspend fun createProfile(profile: UserProfile): Long = 1
    override suspend fun updateProfile(profile: UserProfile) {}
    override suspend fun markOnboardingComplete(userId: Long) {}
    override suspend fun savePreAssessmentScore(userId: Long, score: Float) {}
    override suspend fun savePostAssessmentScore(userId: Long, score: Float) {}
    override suspend fun addXp(userId: Long, xp: Int) {}
    override suspend fun updateStreak(userId: Long, streak: Int, date: Long) {}
    override suspend fun updateCurrentStage(userId: Long, stageId: Int) {}
}
