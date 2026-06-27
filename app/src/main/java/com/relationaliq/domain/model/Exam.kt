package com.relationaliq.domain.model

data class Exam(
    val id: Int,
    val stageRangeStart: Int,
    val stageRangeEnd: Int,
    val totalQuestions: Int = 15,
    val passingThreshold: Float = 0.70f
) {
    val title: String
        get() = "Exam ${id}: Stages $stageRangeStart-$stageRangeEnd"

    val description: String
        get() = "Adaptive exam covering all content from stages $stageRangeStart through $stageRangeEnd"
}

data class ExamResult(
    val id: Long = 0,
    val examId: Int,
    val stageRangeStart: Int,
    val stageRangeEnd: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val passed: Boolean,
    val difficultyPath: List<Difficulty>,
    val relationTypeScores: Map<RelationType, RelationScore>,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val xpEarned: Int = 0
)

data class RelationScore(
    val correct: Int,
    val total: Int
) {
    val accuracy: Float
        get() = if (total > 0) correct.toFloat() / total else 0f
}

data class ExamTrialState(
    val trial: Trial,
    val difficultyAtPresentation: Difficulty,
    val sourceStageId: Int
)

data class AdaptiveState(
    val currentDifficulty: Difficulty = Difficulty.MEDIUM,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val relationTypeScores: MutableMap<RelationType, RelationScore> = mutableMapOf(),
    val difficultyPath: MutableList<Difficulty> = mutableListOf(Difficulty.MEDIUM),
    val questionsAnswered: Int = 0,
    val correctCount: Int = 0
)
