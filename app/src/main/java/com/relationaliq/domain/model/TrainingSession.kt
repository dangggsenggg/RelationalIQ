package com.relationaliq.domain.model

data class TrainingSession(
    val id: Long = 0,
    val stageId: Int,
    val blockType: BlockType,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalTrials: Int = 0,
    val correctAnswers: Int = 0,
    val averageResponseTimeMs: Long = 0,
    val accuracy: Float = 0f,
    val passed: Boolean = false,
    val xpEarned: Int = 0
)

data class TrialResult(
    val id: Long = 0,
    val sessionId: Long,
    val trialId: String,
    val userAnswer: Boolean,
    val correctAnswer: Boolean,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
