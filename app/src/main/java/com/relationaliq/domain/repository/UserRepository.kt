package com.relationaliq.domain.repository

import com.relationaliq.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun createProfile(profile: UserProfile): Long
    suspend fun updateProfile(profile: UserProfile)
    suspend fun markOnboardingComplete(userId: Long)
    suspend fun savePreAssessmentScore(userId: Long, score: Float)
    suspend fun savePostAssessmentScore(userId: Long, score: Float)
    suspend fun addXp(userId: Long, xp: Int)
    suspend fun updateStreak(userId: Long, streak: Int, date: Long)
    suspend fun updateCurrentStage(userId: Long, stageId: Int)
}
