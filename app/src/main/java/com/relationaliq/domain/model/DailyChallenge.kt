package com.relationaliq.domain.model

data class DailyChallenge(
    val date: Long,
    val trials: List<Trial>,
    val isCompleted: Boolean = false,
    val score: Float = 0f,
    val xpReward: Int = 50
)
