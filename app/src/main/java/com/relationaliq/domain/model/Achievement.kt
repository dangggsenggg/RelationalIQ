package com.relationaliq.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val category: AchievementCategory,
    val requirement: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

enum class AchievementCategory {
    STREAK,
    ACCURACY,
    SPEED,
    COMPLETION,
    MASTERY
}

object Achievements {
    val all = listOf(
        Achievement("streak_7", "Week Warrior", "Maintain a 7-day training streak", "local_fire_department", AchievementCategory.STREAK, 7),
        Achievement("streak_30", "Monthly Master", "Maintain a 30-day training streak", "whatshot", AchievementCategory.STREAK, 30),
        Achievement("accuracy_90", "Sharp Mind", "Achieve 90%+ accuracy in a session", "psychology", AchievementCategory.ACCURACY, 90),
        Achievement("accuracy_100", "Perfect Deriver", "Achieve 100% accuracy in a session", "stars", AchievementCategory.ACCURACY, 100),
        Achievement("speed_demon", "Speed Thinker", "Average response time under 5 seconds", "bolt", AchievementCategory.SPEED, 5),
        Achievement("stages_10", "Progressing", "Complete 10 stages", "trending_up", AchievementCategory.COMPLETION, 10),
        Achievement("stages_25", "Halfway There", "Complete 25 stages", "flag", AchievementCategory.COMPLETION, 25),
        Achievement("stages_50", "Master Trainer", "Complete all 50 stages", "emoji_events", AchievementCategory.COMPLETION, 50),
        Achievement("same_master", "Coordinator", "Master all Same/Different stages", "compare_arrows", AchievementCategory.MASTERY, 1),
        Achievement("opposite_master", "Opposition Expert", "Master all Opposite stages", "swap_horiz", AchievementCategory.MASTERY, 1),
        Achievement("comparison_master", "Comparator", "Master all More/Less stages", "sort", AchievementCategory.MASTERY, 1),
        Achievement("temporal_master", "Time Lord", "Master all Before/After stages", "schedule", AchievementCategory.MASTERY, 1),
        Achievement("fluent_deriver", "Fluent Deriver", "Complete 100 trials with 85%+ accuracy", "auto_awesome", AchievementCategory.MASTERY, 100)
    )
}
