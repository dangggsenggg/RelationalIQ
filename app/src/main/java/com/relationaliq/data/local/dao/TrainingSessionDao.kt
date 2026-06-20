package com.relationaliq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.relationaliq.data.local.entity.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insert(session: TrainingSessionEntity): Long

    @Update
    suspend fun update(session: TrainingSessionEntity)

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: Long): TrainingSessionEntity?

    @Query("SELECT * FROM training_sessions WHERE stageId = :stageId ORDER BY startTime DESC")
    fun observeSessionsForStage(stageId: Int): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_sessions ORDER BY startTime DESC")
    fun observeAllSessions(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT COUNT(*) FROM training_sessions WHERE passed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT AVG(accuracy) FROM training_sessions WHERE passed = 1")
    fun observeAverageAccuracy(): Flow<Float?>

    @Query("SELECT AVG(averageResponseTimeMs) FROM training_sessions")
    fun observeAverageResponseTime(): Flow<Long?>
}
