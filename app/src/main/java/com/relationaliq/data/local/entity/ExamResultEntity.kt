package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val examId: Int,
    val stageRangeStart: Int,
    val stageRangeEnd: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val passed: Boolean,
    val difficultyPathJson: String,
    val relationTypeScoresJson: String,
    val startTime: Long,
    val endTime: Long,
    val xpEarned: Int
)
