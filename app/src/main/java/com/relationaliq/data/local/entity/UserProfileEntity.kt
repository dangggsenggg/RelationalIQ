package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.relationaliq.domain.model.AgeGroup
import com.relationaliq.domain.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ageGroup: String = AgeGroup.ADULT.name,
    val goals: String = "",
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
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        ageGroup = AgeGroup.valueOf(ageGroup),
        goals = if (goals.isBlank()) emptyList() else goals.split(","),
        hasCompletedOnboarding = hasCompletedOnboarding,
        hasCompletedPreAssessment = hasCompletedPreAssessment,
        preAssessmentScore = preAssessmentScore,
        postAssessmentScore = postAssessmentScore,
        currentStageId = currentStageId,
        totalXp = totalXp,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastTrainingDate = lastTrainingDate,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity = UserProfileEntity(
            id = profile.id,
            ageGroup = profile.ageGroup.name,
            goals = profile.goals.joinToString(","),
            hasCompletedOnboarding = profile.hasCompletedOnboarding,
            hasCompletedPreAssessment = profile.hasCompletedPreAssessment,
            preAssessmentScore = profile.preAssessmentScore,
            postAssessmentScore = profile.postAssessmentScore,
            currentStageId = profile.currentStageId,
            totalXp = profile.totalXp,
            currentStreak = profile.currentStreak,
            longestStreak = profile.longestStreak,
            lastTrainingDate = profile.lastTrainingDate,
            createdAt = profile.createdAt
        )
    }
}
