package com.relationaliq.domain.repository

import com.relationaliq.data.local.entity.StageProgressEntity
import com.relationaliq.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeAllStageProgress(): Flow<List<StageProgressEntity>>
    fun observeCompletedStagesCount(): Flow<Int>
    suspend fun getStageProgress(stageId: Int): StageProgressEntity?
    suspend fun unlockStage(stageId: Int)
    suspend fun markStageCompleted(stageId: Int, accuracy: Float)
    suspend fun incrementAttempts(stageId: Int)
    fun observeAchievements(): Flow<List<Achievement>>
    fun observeUnlockedAchievements(): Flow<List<Achievement>>
    suspend fun unlockAchievement(id: String)
    suspend fun initializeAchievements()
    fun observeAverageAccuracy(): Flow<Float?>
    fun observeAverageResponseTime(): Flow<Long?>
    fun observeTotalTrials(): Flow<Int>
    fun observeTotalCorrect(): Flow<Int>
}
