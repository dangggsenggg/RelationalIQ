package com.relationaliq.data.repository

import com.relationaliq.data.local.dao.UserProfileDao
import com.relationaliq.data.local.entity.UserProfileEntity
import com.relationaliq.domain.model.UserProfile
import com.relationaliq.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        userProfileDao.observeProfile().map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? =
        userProfileDao.getProfile()?.toDomain()

    override suspend fun createProfile(profile: UserProfile): Long =
        userProfileDao.insert(UserProfileEntity.fromDomain(profile))

    override suspend fun updateProfile(profile: UserProfile) =
        userProfileDao.update(UserProfileEntity.fromDomain(profile))

    override suspend fun markOnboardingComplete(userId: Long) =
        userProfileDao.markOnboardingComplete(userId)

    override suspend fun savePreAssessmentScore(userId: Long, score: Float) =
        userProfileDao.savePreAssessmentScore(userId, score)

    override suspend fun savePostAssessmentScore(userId: Long, score: Float) =
        userProfileDao.savePostAssessmentScore(userId, score)

    override suspend fun addXp(userId: Long, xp: Int) =
        userProfileDao.addXp(userId, xp)

    override suspend fun updateStreak(userId: Long, streak: Int, date: Long) =
        userProfileDao.updateStreak(userId, streak, date)

    override suspend fun updateCurrentStage(userId: Long, stageId: Int) =
        userProfileDao.updateCurrentStage(userId, stageId)
}
