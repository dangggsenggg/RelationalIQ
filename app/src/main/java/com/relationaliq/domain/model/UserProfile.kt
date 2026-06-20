package com.relationaliq.domain.model

data class UserProfile(
    val id: Long = 0,
    val ageGroup: AgeGroup = AgeGroup.ADULT,
    val goals: List<String> = emptyList(),
    val hasCompletedOnboarding: Boolean = false,
    val hasCompletedPreAssessment: Boolean = false,
    val preAssessmentScore: Float = 0f,
    val postAssessmentScore: Float? = null,
    val currentStageId: Int = 1,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastTrainingDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AgeGroup(val label: String) {
    CHILD("Child (8-12)"),
    TEEN("Teen (13-17)"),
    ADULT("Adult (18+)")
}
