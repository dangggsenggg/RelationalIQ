package com.relationaliq.domain.model

data class Stage(
    val id: Int,
    val title: String,
    val description: String,
    val module: String = "",
    val frameType: String = "",
    val subFrame: String = "",
    val relationTypes: List<RelationType>,
    val premiseCount: Int,
    val derivationDepth: Int = 1,
    val difficulty: Difficulty,
    val trainingTrials: List<Trial>,
    val testTrials: List<Trial>,
    val masteryThreshold: Float = 0.85f,
    val timeLimitSeconds: Int = 30,
    val xpReward: Int = 100,
    val estimatedTimeMinutes: Int = 4
)

enum class Difficulty(val label: String, val ordinal_level: Int) {
    BEGINNER("Beginner", 1),
    EASY("Easy", 2),
    MEDIUM("Medium", 3),
    HARD("Hard", 4),
    ADVANCED("Advanced", 5),
    EXPERT("Expert", 6)
}

enum class BlockType {
    TRAINING,
    TEST
}
