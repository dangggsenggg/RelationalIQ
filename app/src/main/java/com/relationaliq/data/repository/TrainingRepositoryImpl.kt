package com.relationaliq.data.repository

import com.relationaliq.data.datasource.StageDataSource
import com.relationaliq.data.local.dao.TrainingSessionDao
import com.relationaliq.data.local.dao.TrialResultDao
import com.relationaliq.data.local.entity.TrainingSessionEntity
import com.relationaliq.data.local.entity.TrialResultEntity
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.TrainingSession
import com.relationaliq.domain.model.TrialResult
import com.relationaliq.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    private val stageDataSource: StageDataSource,
    private val trainingSessionDao: TrainingSessionDao,
    private val trialResultDao: TrialResultDao
) : TrainingRepository {

    override suspend fun getStage(stageId: Int): Stage? =
        stageDataSource.getStage(stageId)

    override suspend fun getAllStages(): List<Stage> =
        stageDataSource.getAllStages()

    override suspend fun createSession(session: TrainingSession): Long =
        trainingSessionDao.insert(TrainingSessionEntity.fromDomain(session))

    override suspend fun updateSession(session: TrainingSession) =
        trainingSessionDao.update(TrainingSessionEntity.fromDomain(session))

    override suspend fun saveTrialResult(result: TrialResult): Long =
        trialResultDao.insert(TrialResultEntity.fromDomain(result))

    override fun observeSessionsForStage(stageId: Int): Flow<List<TrainingSession>> =
        trainingSessionDao.observeSessionsForStage(stageId).map { list ->
            list.map { it.toDomain() }
        }

    override fun observeAllSessions(): Flow<List<TrainingSession>> =
        trainingSessionDao.observeAllSessions().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getRecentSessions(limit: Int): List<TrainingSession> =
        trainingSessionDao.getRecentSessions(limit).map { it.toDomain() }

    override suspend fun getSessionById(sessionId: Long): TrainingSession? =
        trainingSessionDao.getById(sessionId)?.toDomain()
}
