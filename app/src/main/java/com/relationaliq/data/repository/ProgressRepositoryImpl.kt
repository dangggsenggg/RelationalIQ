package com.relationaliq.data.repository

import com.relationaliq.data.local.dao.AchievementDao
import com.relationaliq.data.local.dao.StageProgressDao
import com.relationaliq.data.local.dao.TrainingSessionDao
import com.relationaliq.data.local.dao.TrialResultDao
import com.relationaliq.data.local.entity.AchievementEntity
import com.relationaliq.data.local.entity.StageProgressEntity
import com.relationaliq.domain.model.Achievement
import com.relationaliq.domain.model.Achievements
import com.relationaliq.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val stageProgressDao: StageProgressDao,
    private val achievementDao: AchievementDao,
    private val trainingSessionDao: TrainingSessionDao,
    private val trialResultDao: TrialResultDao
) : ProgressRepository {

    override fun observeAllStageProgress(): Flow<List<StageProgressEntity>> =
        stageProgressDao.observeAllProgress()

    override fun observeCompletedStagesCount(): Flow<Int> =
        stageProgressDao.observeCompletedCount()

    override suspend fun getStageProgress(stageId: Int): StageProgressEntity? =
        stageProgressDao.getProgress(stageId)

    override suspend fun unlockStage(stageId: Int) =
        stageProgressDao.insertOrUpdate(StageProgressEntity(stageId = stageId, isUnlocked = true))

    override suspend fun markStageCompleted(stageId: Int, accuracy: Float) =
        stageProgressDao.markCompleted(stageId, accuracy)

    override suspend fun incrementAttempts(stageId: Int) =
        stageProgressDao.incrementAttempts(stageId)

    override fun observeAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAll().map { entities ->
            Achievements.all.map { achievement ->
                val entity = entities.find { it.id == achievement.id }
                achievement.copy(
                    isUnlocked = entity?.isUnlocked == true,
                    unlockedAt = entity?.unlockedAt
                )
            }
        }

    override fun observeUnlockedAchievements(): Flow<List<Achievement>> =
        achievementDao.observeUnlocked().map { entities ->
            entities.mapNotNull { entity ->
                Achievements.all.find { it.id == entity.id }?.copy(
                    isUnlocked = true,
                    unlockedAt = entity.unlockedAt
                )
            }
        }

    override suspend fun unlockAchievement(id: String) =
        achievementDao.unlock(id)

    override suspend fun initializeAchievements() {
        val entities = Achievements.all.map { AchievementEntity(id = it.id) }
        achievementDao.insertAll(entities)
    }

    override fun observeAverageAccuracy(): Flow<Float?> =
        trainingSessionDao.observeAverageAccuracy()

    override fun observeAverageResponseTime(): Flow<Long?> =
        trainingSessionDao.observeAverageResponseTime()

    override fun observeTotalTrials(): Flow<Int> =
        trialResultDao.observeTotalTrials()

    override fun observeTotalCorrect(): Flow<Int> =
        trialResultDao.observeTotalCorrect()
}
