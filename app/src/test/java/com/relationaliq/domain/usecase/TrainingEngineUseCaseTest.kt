package com.relationaliq.domain.usecase

import com.relationaliq.data.local.entity.StageProgressEntity
import com.relationaliq.domain.model.Achievement
import com.relationaliq.domain.model.BlockType
import com.relationaliq.domain.model.Difficulty
import com.relationaliq.domain.model.Premise
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.TrainingSession
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.model.TrialResult
import com.relationaliq.domain.model.UserProfile
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.TrainingRepository
import com.relationaliq.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrainingEngineUseCaseTest {

    private lateinit var trainingRepository: FakeTrainingRepository
    private lateinit var progressRepository: FakeProgressRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var useCase: TrainingEngineUseCase

    private val sampleStage = Stage(
        id = 1,
        title = "Test Stage",
        description = "A test stage",
        relationTypes = listOf(RelationType.SAME),
        premiseCount = 2,
        difficulty = Difficulty.BEGINNER,
        trainingTrials = listOf(
            Trial("t1", listOf(Premise("A", RelationType.SAME, "B"), Premise("B", RelationType.SAME, "C")),
                "A", RelationType.SAME, "C", true, explanation = "A=B=C"),
            Trial("t2", listOf(Premise("D", RelationType.SAME, "E"), Premise("E", RelationType.SAME, "F")),
                "D", RelationType.DIFFERENT, "F", false, explanation = "D=E=F, not different")
        ),
        testTrials = listOf(
            Trial("x1", listOf(Premise("G", RelationType.SAME, "H"), Premise("H", RelationType.SAME, "I")),
                "G", RelationType.SAME, "I", true)
        ),
        masteryThreshold = 0.85f,
        xpReward = 100
    )

    @Before
    fun setup() {
        trainingRepository = FakeTrainingRepository()
        progressRepository = FakeProgressRepository()
        userRepository = FakeUserRepository()
        useCase = TrainingEngineUseCase(trainingRepository, progressRepository, userRepository)

        trainingRepository.stages[1] = sampleStage
    }

    @Test
    fun `loadStage returns stage when exists`() = runTest {
        val stage = useCase.loadStage(1)
        assertNotNull(stage)
        assertEquals("Test Stage", stage!!.title)
    }

    @Test
    fun `loadStage returns null for nonexistent stage`() = runTest {
        val stage = useCase.loadStage(999)
        assertEquals(null, stage)
    }

    @Test
    fun `startSession creates session and increments attempts`() = runTest {
        val sessionId = useCase.startSession(1, BlockType.TRAINING)
        assertTrue(sessionId > 0)
        assertEquals(1, progressRepository.attemptCounts[1])
    }

    @Test
    fun `submitAnswer returns correct result for correct answer`() = runTest {
        val sessionId = useCase.startSession(1, BlockType.TRAINING)
        val trial = sampleStage.trainingTrials[0]
        val result = useCase.submitAnswer(sessionId, trial, true, 2500)

        assertTrue(result.isCorrect)
        assertEquals(true, result.userAnswer)
        assertEquals(true, result.correctAnswer)
        assertEquals(2500L, result.responseTimeMs)
    }

    @Test
    fun `submitAnswer returns incorrect result for wrong answer`() = runTest {
        val sessionId = useCase.startSession(1, BlockType.TRAINING)
        val trial = sampleStage.trainingTrials[0]
        val result = useCase.submitAnswer(sessionId, trial, false, 3000)

        assertFalse(result.isCorrect)
        assertEquals(false, result.userAnswer)
        assertEquals(true, result.correctAnswer)
    }

    @Test
    fun `completeSession calculates accuracy correctly`() = runTest {
        val results = listOf(
            TrialResult(sessionId = 1, trialId = "t1", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t2", userAnswer = true, correctAnswer = false, isCorrect = false, responseTimeMs = 3000)
        )
        val sessionResult = useCase.completeSession(1, 1, BlockType.TRAINING, results, System.currentTimeMillis())

        assertEquals(0.5f, sessionResult.accuracy)
        assertEquals(1, sessionResult.correctCount)
        assertEquals(2, sessionResult.totalTrials)
    }

    @Test
    fun `completeSession marks passed when accuracy meets threshold`() = runTest {
        val results = listOf(
            TrialResult(sessionId = 1, trialId = "t1", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t2", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t3", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t4", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t5", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000)
        )
        val sessionResult = useCase.completeSession(1, 1, BlockType.TEST, results, System.currentTimeMillis())

        assertEquals(1.0f, sessionResult.accuracy)
        assertTrue(sessionResult.passed)
    }

    @Test
    fun `completeSession marks failed when accuracy below threshold`() = runTest {
        val results = listOf(
            TrialResult(sessionId = 1, trialId = "t1", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t2", userAnswer = false, correctAnswer = true, isCorrect = false, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t3", userAnswer = false, correctAnswer = true, isCorrect = false, responseTimeMs = 2000)
        )
        val sessionResult = useCase.completeSession(1, 1, BlockType.TEST, results, System.currentTimeMillis())

        assertEquals(1f / 3f, sessionResult.accuracy, 0.01f)
        assertFalse(sessionResult.passed)
    }

    @Test
    fun `completeSession unlocks next stage on passed TEST block`() = runTest {
        userRepository.profile = UserProfile(id = 1, currentStageId = 1)
        val results = List(5) {
            TrialResult(sessionId = 1, trialId = "t$it", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000)
        }
        useCase.completeSession(1, 1, BlockType.TEST, results, System.currentTimeMillis())

        assertTrue(progressRepository.completedStages.contains(1))
        assertTrue(progressRepository.unlockedStages.contains(2))
    }

    @Test
    fun `completeSession does NOT unlock next stage on failed TEST block`() = runTest {
        userRepository.profile = UserProfile(id = 1, currentStageId = 1)
        val results = listOf(
            TrialResult(sessionId = 1, trialId = "t1", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t2", userAnswer = false, correctAnswer = true, isCorrect = false, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t3", userAnswer = false, correctAnswer = true, isCorrect = false, responseTimeMs = 2000)
        )
        useCase.completeSession(1, 1, BlockType.TEST, results, System.currentTimeMillis())

        assertFalse(progressRepository.completedStages.contains(1))
        assertFalse(progressRepository.unlockedStages.contains(2))
    }

    @Test
    fun `completeSession does NOT unlock next stage on TRAINING block even if passed`() = runTest {
        userRepository.profile = UserProfile(id = 1, currentStageId = 1)
        val results = List(5) {
            TrialResult(sessionId = 1, trialId = "t$it", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000)
        }
        useCase.completeSession(1, 1, BlockType.TRAINING, results, System.currentTimeMillis())

        assertFalse(progressRepository.completedStages.contains(1))
        assertFalse(progressRepository.unlockedStages.contains(2))
    }

    @Test
    fun `completeSession awards XP on passed TEST block`() = runTest {
        userRepository.profile = UserProfile(id = 1, currentStageId = 1, totalXp = 0)
        val results = List(5) {
            TrialResult(sessionId = 1, trialId = "t$it", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000)
        }
        val sessionResult = useCase.completeSession(1, 1, BlockType.TEST, results, System.currentTimeMillis())

        assertTrue(sessionResult.xpEarned > 0)
        assertTrue(userRepository.xpAdded > 0)
    }

    @Test
    fun `XP calculation gives speed bonus for fast responses`() = runTest {
        val fastResults = List(5) {
            TrialResult(sessionId = 1, trialId = "t$it", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000)
        }
        val slowResults = List(5) {
            TrialResult(sessionId = 1, trialId = "t$it", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 15000)
        }

        userRepository.profile = UserProfile(id = 1)
        val fastSession = useCase.completeSession(1, 1, BlockType.TEST, fastResults, System.currentTimeMillis())

        userRepository.profile = UserProfile(id = 1)
        userRepository.xpAdded = 0
        val slowSession = useCase.completeSession(2, 1, BlockType.TEST, slowResults, System.currentTimeMillis())

        assertTrue(fastSession.xpEarned > slowSession.xpEarned)
    }

    @Test
    fun `completeSession with empty results returns zero accuracy`() = runTest {
        val sessionResult = useCase.completeSession(1, 1, BlockType.TRAINING, emptyList(), System.currentTimeMillis())
        assertEquals(0f, sessionResult.accuracy)
        assertEquals(0, sessionResult.correctCount)
        assertEquals(0, sessionResult.totalTrials)
    }

    @Test
    fun `completeSession calculates average response time`() = runTest {
        val results = listOf(
            TrialResult(sessionId = 1, trialId = "t1", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 2000),
            TrialResult(sessionId = 1, trialId = "t2", userAnswer = true, correctAnswer = true, isCorrect = true, responseTimeMs = 4000)
        )
        val sessionResult = useCase.completeSession(1, 1, BlockType.TRAINING, results, System.currentTimeMillis())
        assertEquals(3000L, sessionResult.averageResponseTimeMs)
    }
}

// Fake implementations for testing

class FakeTrainingRepository : TrainingRepository {
    val stages = mutableMapOf<Int, Stage>()
    private val sessions = mutableMapOf<Long, TrainingSession>()
    private val trialResults = mutableListOf<TrialResult>()
    private var nextSessionId = 1L

    override suspend fun getStage(stageId: Int): Stage? = stages[stageId]
    override suspend fun getAllStages(): List<Stage> = stages.values.toList()
    override suspend fun createSession(session: TrainingSession): Long {
        val id = nextSessionId++
        sessions[id] = session.copy(id = id)
        return id
    }
    override suspend fun updateSession(session: TrainingSession) {
        sessions[session.id] = session
    }
    override suspend fun saveTrialResult(result: TrialResult): Long {
        trialResults.add(result)
        return trialResults.size.toLong()
    }
    override fun observeSessionsForStage(stageId: Int): Flow<List<TrainingSession>> =
        flowOf(sessions.values.filter { it.stageId == stageId })
    override fun observeAllSessions(): Flow<List<TrainingSession>> = flowOf(sessions.values.toList())
    override suspend fun getRecentSessions(limit: Int): List<TrainingSession> =
        sessions.values.toList().takeLast(limit)
}

class FakeProgressRepository : ProgressRepository {
    val completedStages = mutableSetOf<Int>()
    val unlockedStages = mutableSetOf<Int>()
    val attemptCounts = mutableMapOf<Int, Int>()

    override fun observeAllStageProgress(): Flow<List<StageProgressEntity>> = flowOf(emptyList())
    override fun observeCompletedStagesCount(): Flow<Int> = flowOf(completedStages.size)
    override suspend fun getStageProgress(stageId: Int): StageProgressEntity? = null
    override suspend fun unlockStage(stageId: Int) { unlockedStages.add(stageId) }
    override suspend fun markStageCompleted(stageId: Int, accuracy: Float) { completedStages.add(stageId) }
    override suspend fun incrementAttempts(stageId: Int) {
        attemptCounts[stageId] = (attemptCounts[stageId] ?: 0) + 1
    }
    override fun observeAchievements(): Flow<List<Achievement>> = flowOf(emptyList())
    override fun observeUnlockedAchievements(): Flow<List<Achievement>> = flowOf(emptyList())
    override suspend fun unlockAchievement(id: String) {}
    override suspend fun initializeAchievements() {}
    override fun observeAverageAccuracy(): Flow<Float?> = flowOf(null)
    override fun observeAverageResponseTime(): Flow<Long?> = flowOf(null)
    override fun observeTotalTrials(): Flow<Int> = flowOf(0)
    override fun observeTotalCorrect(): Flow<Int> = flowOf(0)
}

class FakeUserRepository : UserRepository {
    var profile: UserProfile? = null
    var xpAdded = 0

    override fun observeProfile(): Flow<UserProfile?> = flowOf(profile)
    override suspend fun getProfile(): UserProfile? = profile
    override suspend fun createProfile(profile: UserProfile): Long { this.profile = profile; return profile.id }
    override suspend fun updateProfile(profile: UserProfile) { this.profile = profile }
    override suspend fun markOnboardingComplete(userId: Long) {}
    override suspend fun savePreAssessmentScore(userId: Long, score: Float) {}
    override suspend fun savePostAssessmentScore(userId: Long, score: Float) {}
    override suspend fun addXp(userId: Long, xp: Int) { xpAdded += xp }
    override suspend fun updateStreak(userId: Long, streak: Int, date: Long) {}
    override suspend fun updateCurrentStage(userId: Long, stageId: Int) {}
}
