package com.relationaliq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.relationaliq.data.local.entity.TrialResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrialResultDao {

    @Insert
    suspend fun insert(result: TrialResultEntity): Long

    @Insert
    suspend fun insertAll(results: List<TrialResultEntity>)

    @Query("SELECT * FROM trial_results WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getResultsForSession(sessionId: Long): List<TrialResultEntity>

    @Query("SELECT * FROM trial_results WHERE sessionId = :sessionId ORDER BY timestamp")
    fun observeResultsForSession(sessionId: Long): Flow<List<TrialResultEntity>>

    @Query("SELECT COUNT(*) FROM trial_results WHERE isCorrect = 1")
    fun observeTotalCorrect(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trial_results")
    fun observeTotalTrials(): Flow<Int>
}
