package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.relationaliq.domain.model.TrialResult

@Entity(
    tableName = "trial_results",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TrialResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val trialId: String,
    val userAnswer: Boolean,
    val correctAnswer: Boolean,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): TrialResult = TrialResult(
        id = id,
        sessionId = sessionId,
        trialId = trialId,
        userAnswer = userAnswer,
        correctAnswer = correctAnswer,
        isCorrect = isCorrect,
        responseTimeMs = responseTimeMs,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(result: TrialResult): TrialResultEntity = TrialResultEntity(
            id = result.id,
            sessionId = result.sessionId,
            trialId = result.trialId,
            userAnswer = result.userAnswer,
            correctAnswer = result.correctAnswer,
            isCorrect = result.isCorrect,
            responseTimeMs = result.responseTimeMs,
            timestamp = result.timestamp
        )
    }
}
