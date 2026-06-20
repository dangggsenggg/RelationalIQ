package com.relationaliq.domain.repository

import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.TrainingSession
import com.relationaliq.domain.model.TrialResult
import kotlinx.coroutines.flow.Flow

interface TrainingRepository {
    suspend fun getStage(stageId: Int): Stage?
    suspend fun getAllStages(): List<Stage>
    suspend fun createSession(session: TrainingSession): Long
    suspend fun updateSession(session: TrainingSession)
    suspend fun saveTrialResult(result: TrialResult): Long
    fun observeSessionsForStage(stageId: Int): Flow<List<TrainingSession>>
    fun observeAllSessions(): Flow<List<TrainingSession>>
    suspend fun getRecentSessions(limit: Int = 10): List<TrainingSession>
}
