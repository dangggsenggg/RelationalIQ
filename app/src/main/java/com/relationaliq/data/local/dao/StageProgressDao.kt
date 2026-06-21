package com.relationaliq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.relationaliq.data.local.entity.StageProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageProgressDao {

    @Query("SELECT * FROM stage_progress WHERE stageId = :stageId")
    suspend fun getProgress(stageId: Int): StageProgressEntity?

    @Query("SELECT * FROM stage_progress ORDER BY stageId")
    fun observeAllProgress(): Flow<List<StageProgressEntity>>

    @Query("SELECT * FROM stage_progress WHERE isCompleted = 1")
    fun observeCompletedStages(): Flow<List<StageProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: StageProgressEntity)

    @Query("UPDATE stage_progress SET isUnlocked = 1 WHERE stageId = :stageId")
    suspend fun unlockStage(stageId: Int)

    @Query("UPDATE stage_progress SET isCompleted = 1, completedAt = :timestamp, bestAccuracy = CASE WHEN :accuracy > bestAccuracy THEN :accuracy ELSE bestAccuracy END WHERE stageId = :stageId")
    suspend fun markCompleted(stageId: Int, accuracy: Float, timestamp: Long = System.currentTimeMillis())

    @Query("INSERT OR IGNORE INTO stage_progress (stageId, isUnlocked, isCompleted, bestAccuracy, bestTimeMs, attempts) VALUES (:stageId, 0, 0, 0, 0, 0)")
    suspend fun insertIfAbsent(stageId: Int)

    @Query("UPDATE stage_progress SET attempts = attempts + 1 WHERE stageId = :stageId")
    suspend fun incrementAttemptsQuery(stageId: Int)

    suspend fun incrementAttempts(stageId: Int) {
        insertIfAbsent(stageId)
        incrementAttemptsQuery(stageId)
    }

    @Query("SELECT COUNT(*) FROM stage_progress WHERE isCompleted = 1")
    fun observeCompletedCount(): Flow<Int>
}
