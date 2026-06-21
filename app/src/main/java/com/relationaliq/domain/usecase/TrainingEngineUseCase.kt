package com.relationaliq.domain.usecase

import com.relationaliq.domain.model.BlockType
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.TrainingSession
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.model.TrialResult
import com.relationaliq.domain.model.UserProfile
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.TrainingRepository
import com.relationaliq.domain.repository.UserRepository
import javax.inject.Inject

class TrainingEngineUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository
) {
    suspend fun loadStage(stageId: Int): Stage? =
        trainingRepository.getStage(stageId)

    suspend fun startSession(stageId: Int, blockType: BlockType): Long {
        val session = TrainingSession(
            stageId = stageId,
            blockType = blockType
        )
        progressRepository.incrementAttempts(stageId)
        return trainingRepository.createSession(session)
    }

    suspend fun submitAnswer(
        sessionId: Long,
        trial: Trial,
        userAnswer: Boolean,
        responseTimeMs: Long
    ): TrialResult {
        val result = TrialResult(
            sessionId = sessionId,
            trialId = trial.id,
            userAnswer = userAnswer,
            correctAnswer = trial.correctAnswer,
            isCorrect = userAnswer == trial.correctAnswer,
            responseTimeMs = responseTimeMs
        )
        trainingRepository.saveTrialResult(result)
        return result
    }

    suspend fun completeSession(
        sessionId: Long,
        stageId: Int,
        blockType: BlockType,
        results: List<TrialResult>,
        startTime: Long
    ): SessionResult {
        val correctCount = results.count { it.isCorrect }
        val accuracy = if (results.isNotEmpty()) correctCount.toFloat() / results.size else 0f
        val avgResponseTime = if (results.isNotEmpty()) results.map { it.responseTimeMs }.average().toLong() else 0L
        val stage = trainingRepository.getStage(stageId)
        val passed = accuracy >= (stage?.masteryThreshold ?: 0.85f)
        val xpEarned = calculateXp(accuracy, avgResponseTime, stage?.xpReward ?: 100)

        val session = TrainingSession(
            id = sessionId,
            stageId = stageId,
            blockType = blockType,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            totalTrials = results.size,
            correctAnswers = correctCount,
            averageResponseTimeMs = avgResponseTime,
            accuracy = accuracy,
            passed = passed,
            xpEarned = xpEarned
        )
        trainingRepository.updateSession(session)

        if (passed && blockType == BlockType.TEST) {
            progressRepository.markStageCompleted(stageId, accuracy)
            progressRepository.unlockStage(stageId + 1)
            val profile = userRepository.getProfile()
            if (profile != null) {
                userRepository.addXp(profile.id, xpEarned)
                userRepository.updateCurrentStage(profile.id, stageId + 1)
                updateStreak(profile)
            }
        }

        return SessionResult(
            accuracy = accuracy,
            correctCount = correctCount,
            totalTrials = results.size,
            averageResponseTimeMs = avgResponseTime,
            passed = passed,
            xpEarned = xpEarned
        )
    }

    private suspend fun updateStreak(profile: UserProfile) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val lastDate = profile.lastTrainingDate ?: 0L
        val elapsed = now - lastDate
        val newStreak = when {
            elapsed < oneDayMs -> profile.currentStreak
            elapsed < oneDayMs * 2 -> profile.currentStreak + 1
            else -> 1
        }
        userRepository.updateStreak(profile.id, newStreak, now)
    }

    private fun calculateXp(accuracy: Float, avgResponseTimeMs: Long, baseXp: Int): Int {
        val accuracyMultiplier = accuracy
        val speedBonus = when {
            avgResponseTimeMs < 3000 -> 1.5f
            avgResponseTimeMs < 5000 -> 1.2f
            avgResponseTimeMs < 10000 -> 1.0f
            else -> 0.8f
        }
        return (baseXp * accuracyMultiplier * speedBonus).toInt()
    }
}

data class SessionResult(
    val accuracy: Float,
    val correctCount: Int,
    val totalTrials: Int,
    val averageResponseTimeMs: Long,
    val passed: Boolean,
    val xpEarned: Int
)
