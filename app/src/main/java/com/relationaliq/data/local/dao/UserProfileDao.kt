package com.relationaliq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.relationaliq.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity): Long

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET currentStageId = :stageId WHERE id = :userId")
    suspend fun updateCurrentStage(userId: Long, stageId: Int)

    @Query("UPDATE user_profiles SET totalXp = totalXp + :xp WHERE id = :userId")
    suspend fun addXp(userId: Long, xp: Int)

    @Query("UPDATE user_profiles SET currentStreak = :streak, longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END, lastTrainingDate = :date WHERE id = :userId")
    suspend fun updateStreak(userId: Long, streak: Int, date: Long)

    @Query("UPDATE user_profiles SET hasCompletedOnboarding = 1 WHERE id = :userId")
    suspend fun markOnboardingComplete(userId: Long)

    @Query("UPDATE user_profiles SET hasCompletedPreAssessment = 1, preAssessmentScore = :score WHERE id = :userId")
    suspend fun savePreAssessmentScore(userId: Long, score: Float)

    @Query("UPDATE user_profiles SET postAssessmentScore = :score WHERE id = :userId")
    suspend fun savePostAssessmentScore(userId: Long, score: Float)
}
