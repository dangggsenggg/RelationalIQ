package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.relationaliq.domain.model.BlockType
import com.relationaliq.domain.model.TrainingSession

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stageId: Int,
    val blockType: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalTrials: Int = 0,
    val correctAnswers: Int = 0,
    val averageResponseTimeMs: Long = 0,
    val accuracy: Float = 0f,
    val passed: Boolean = false,
    val xpEarned: Int = 0
) {
    fun toDomain(): TrainingSession = TrainingSession(
        id = id,
        stageId = stageId,
        blockType = BlockType.valueOf(blockType),
        startTime = startTime,
        endTime = endTime,
        totalTrials = totalTrials,
        correctAnswers = correctAnswers,
        averageResponseTimeMs = averageResponseTimeMs,
        accuracy = accuracy,
        passed = passed,
        xpEarned = xpEarned
    )

    companion object {
        fun fromDomain(session: TrainingSession): TrainingSessionEntity = TrainingSessionEntity(
            id = session.id,
            stageId = session.stageId,
            blockType = session.blockType.name,
            startTime = session.startTime,
            endTime = session.endTime,
            totalTrials = session.totalTrials,
            correctAnswers = session.correctAnswers,
            averageResponseTimeMs = session.averageResponseTimeMs,
            accuracy = session.accuracy,
            passed = session.passed,
            xpEarned = session.xpEarned
        )
    }
}
