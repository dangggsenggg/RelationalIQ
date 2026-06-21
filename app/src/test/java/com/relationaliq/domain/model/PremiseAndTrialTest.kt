package com.relationaliq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiseAndTrialTest {

    @Test
    fun `premise display string shows correct format`() {
        val premise = Premise("AWX", RelationType.MORE_THAN, "BGW")
        assertEquals("AWX is More Than BGW", premise.toDisplayString())
    }

    @Test
    fun `premise display string for SAME relation`() {
        val premise = Premise("GYQ", RelationType.SAME, "FYW")
        assertEquals("GYQ is Same As FYW", premise.toDisplayString())
    }

    @Test
    fun `premise display string for OPPOSITE relation`() {
        val premise = Premise("LYR", RelationType.OPPOSITE, "BOK")
        assertEquals("LYR is Opposite Of BOK", premise.toDisplayString())
    }

    @Test
    fun `trial probe display string shows question format`() {
        val trial = Trial(
            id = "test1",
            premises = listOf(Premise("AWX", RelationType.MORE_THAN, "BGW")),
            probeStimA = "AWX",
            probeRelation = RelationType.MORE_THAN,
            probeStimB = "BGW",
            correctAnswer = true
        )
        assertEquals("Is AWX More Than BGW?", trial.probeDisplayString())
    }

    @Test
    fun `trial with multiple premises preserves all premises`() {
        val premises = listOf(
            Premise("AWX", RelationType.MORE_THAN, "EGC"),
            Premise("EGC", RelationType.MORE_THAN, "OPA"),
            Premise("OPA", RelationType.MORE_THAN, "BGW")
        )
        val trial = Trial(
            id = "test2",
            premises = premises,
            probeStimA = "AWX",
            probeRelation = RelationType.MORE_THAN,
            probeStimB = "BGW",
            correctAnswer = true
        )
        assertEquals(3, trial.premises.size)
        assertEquals("AWX", trial.premises[0].stimulusA)
        assertEquals("BGW", trial.premises[2].stimulusB)
    }

    @Test
    fun `trial default time limit is 30 seconds`() {
        val trial = Trial(
            id = "test3",
            premises = listOf(Premise("A", RelationType.SAME, "B")),
            probeStimA = "A",
            probeRelation = RelationType.SAME,
            probeStimB = "B",
            correctAnswer = true
        )
        assertEquals(30, trial.timeLimitSeconds)
    }

    @Test
    fun `stage mastery threshold defaults to 85 percent`() {
        val stage = Stage(
            id = 1,
            title = "Test",
            description = "Test stage",
            relationTypes = listOf(RelationType.SAME),
            premiseCount = 2,
            difficulty = Difficulty.BEGINNER,
            trainingTrials = emptyList(),
            testTrials = emptyList()
        )
        assertEquals(0.85f, stage.masteryThreshold)
    }

    @Test
    fun `difficulty levels are ordered correctly`() {
        assertTrue(Difficulty.BEGINNER.ordinal_level < Difficulty.EASY.ordinal_level)
        assertTrue(Difficulty.EASY.ordinal_level < Difficulty.MEDIUM.ordinal_level)
        assertTrue(Difficulty.MEDIUM.ordinal_level < Difficulty.HARD.ordinal_level)
        assertTrue(Difficulty.HARD.ordinal_level < Difficulty.ADVANCED.ordinal_level)
        assertTrue(Difficulty.ADVANCED.ordinal_level < Difficulty.EXPERT.ordinal_level)
    }

    @Test
    fun `user profile default values are correct`() {
        val profile = UserProfile()
        assertEquals(AgeGroup.ADULT, profile.ageGroup)
        assertEquals(false, profile.hasCompletedOnboarding)
        assertEquals(false, profile.hasCompletedPreAssessment)
        assertEquals(0f, profile.preAssessmentScore)
        assertEquals(1, profile.currentStageId)
        assertEquals(0, profile.totalXp)
        assertEquals(0, profile.currentStreak)
    }

    @Test
    fun `training session defaults to not passed`() {
        val session = TrainingSession(stageId = 1, blockType = BlockType.TRAINING)
        assertEquals(false, session.passed)
        assertEquals(0, session.xpEarned)
        assertEquals(0f, session.accuracy)
    }

    @Test
    fun `trial result tracks correctness accurately`() {
        val result = TrialResult(
            sessionId = 1,
            trialId = "t1",
            userAnswer = true,
            correctAnswer = true,
            isCorrect = true,
            responseTimeMs = 2500
        )
        assertTrue(result.isCorrect)
        assertEquals(result.userAnswer, result.correctAnswer)
    }

    @Test
    fun `trial result tracks incorrect answer`() {
        val result = TrialResult(
            sessionId = 1,
            trialId = "t2",
            userAnswer = false,
            correctAnswer = true,
            isCorrect = false,
            responseTimeMs = 5000
        )
        assertEquals(false, result.isCorrect)
    }
}
