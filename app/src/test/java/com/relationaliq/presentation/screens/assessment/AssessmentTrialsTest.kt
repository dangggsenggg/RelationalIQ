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
    fun `assessment contains exactly 70 trials`() {
        assertEquals(70, trials.size)
    }

    @Test
    fun `all trials have unique IDs`() {
        val ids = trials.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every trial has at least 1 premise`() {
        for (trial in trials) {
            assertTrue(
                "Trial ${trial.id} has ${trial.premises.size} premise(s), expected at least 1",
                trial.premises.size >= 1
            )
        }
    }

    @Test
    fun `probe stimuli appear in the premise chain`() {
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
        assertTrue("Expected at least 15 false answers for balance, got $falseCount", falseCount >= 15)
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
    fun `assessment covers 7 subscales`() {
        val prefixes = trials.map { it.id.substringBefore("_") }.toSet()
        assertTrue("Should have coord subscale", prefixes.contains("coord"))
        assertTrue("Should have comp subscale", prefixes.contains("comp"))
        assertTrue("Should have opp subscale", prefixes.contains("opp"))
        assertTrue("Should have temp subscale", prefixes.contains("temp"))
        assertTrue("Should have cont subscale", prefixes.contains("cont"))
        assertTrue("Should have mix subscale", prefixes.contains("mix"))
        assertTrue("Should have anal subscale", prefixes.contains("anal"))
    }

    @Test
    fun `coordination subscale has 10 items`() {
        val coordTrials = trials.filter { it.id.startsWith("coord_") }
        assertEquals(10, coordTrials.size)
    }

    @Test
    fun `comparison subscale has 12 items`() {
        val compTrials = trials.filter { it.id.startsWith("comp_") }
        assertEquals(12, compTrials.size)
    }

    @Test
    fun `opposition subscale has 10 items`() {
        val oppTrials = trials.filter { it.id.startsWith("opp_") }
        assertEquals(10, oppTrials.size)
    }

    @Test
    fun `temporal subscale has 10 items`() {
        val tempTrials = trials.filter { it.id.startsWith("temp_") }
        assertEquals(10, tempTrials.size)
    }

    @Test
    fun `containment subscale has 8 items`() {
        val contTrials = trials.filter { it.id.startsWith("cont_") }
        assertEquals(8, contTrials.size)
    }

    @Test
    fun `mixed subscale has 12 items`() {
        val mixTrials = trials.filter { it.id.startsWith("mix_") }
        assertEquals(12, mixTrials.size)
    }

    @Test
    fun `analogy subscale has 8 items`() {
        val analTrials = trials.filter { it.id.startsWith("anal_") }
        assertEquals(8, analTrials.size)
    }

    @Test
    fun `assessment covers multiple relation types`() {
        val probeRelations = trials.map { it.probeRelation }.toSet()
        assertTrue("Should test SAME relation", probeRelations.contains(RelationType.SAME))
        assertTrue("Should test MORE_THAN relation", probeRelations.contains(RelationType.MORE_THAN))
        assertTrue("Should test LESS_THAN relation", probeRelations.contains(RelationType.LESS_THAN))
        assertTrue("Should test OPPOSITE relation", probeRelations.contains(RelationType.OPPOSITE))
        assertTrue("Should test BEFORE relation", probeRelations.contains(RelationType.BEFORE))
        assertTrue("Should test CONTAINS relation", probeRelations.contains(RelationType.CONTAINS))
        assertTrue("Should test WITHIN relation", probeRelations.contains(RelationType.WITHIN))
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
        val trial = trials.first { it.id == "comp_1" }
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
        val trial = trials.first { it.id == "comp_2" }
        assertEquals(false, trial.correctAnswer)
    }

    @Test
    fun `opposite of opposite equals same`() {
        val trial = trials.first { it.id == "opp_2" }
        assertEquals(RelationType.OPPOSITE, trial.premises[0].relationType)
        assertEquals(RelationType.OPPOSITE, trial.premises[1].relationType)
        assertEquals(RelationType.SAME, trial.probeRelation)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `containment transitivity works`() {
        val trial = trials.first { it.id == "cont_3" }
        assertEquals(RelationType.CONTAINS, trial.premises[0].relationType)
        assertEquals(RelationType.CONTAINS, trial.premises[1].relationType)
        assertEquals(RelationType.CONTAINS, trial.probeRelation)
        assertTrue(trial.correctAnswer)
    }

    @Test
    fun `submitting answers tracks score correctly`() {
        for (trial in trials) {
            viewModel.submitAnswer(trial.correctAnswer)
        }
        val state = viewModel.uiState.value
        assertTrue(state.showResult)
        assertEquals(1.0f, state.score)
        assertEquals(70, state.correctCount)
    }

    @Test
    fun `submitting wrong answers reduces score`() {
        for (i in trials.indices) {
            val trial = trials[i]
            if (i < 35) {
                viewModel.submitAnswer(trial.correctAnswer)
            } else {
                viewModel.submitAnswer(!trial.correctAnswer)
            }
        }
        val state = viewModel.uiState.value
        assertTrue(state.showResult)
        assertEquals(0.5f, state.score)
        assertEquals(35, state.correctCount)
    }

    @Test
    fun `subscale scores are computed on completion`() {
        for (trial in trials) {
            viewModel.submitAnswer(trial.correctAnswer)
        }
        val state = viewModel.uiState.value
        assertTrue(state.subscaleScores.isNotEmpty())
        assertTrue(state.subscaleScores.containsKey("Coordination"))
        assertTrue(state.subscaleScores.containsKey("Comparison"))
        assertTrue(state.subscaleScores.containsKey("Opposition"))
        assertTrue(state.subscaleScores.containsKey("Temporal"))
        assertTrue(state.subscaleScores.containsKey("Containment"))
        assertTrue(state.subscaleScores.containsKey("Mixed"))
        assertTrue(state.subscaleScores.containsKey("Analogy"))

        for ((_, subscale) in state.subscaleScores) {
            assertEquals(1.0f, subscale.accuracy)
        }
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
