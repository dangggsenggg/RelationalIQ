package com.relationaliq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.relationaliq.data.local.entity.ExamResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ExamResultEntity): Long

    @Query("SELECT * FROM exam_results WHERE examId = :examId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestResult(examId: Int): ExamResultEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM exam_results WHERE examId = :examId AND passed = 1)")
    suspend fun hasPassedExam(examId: Int): Boolean

    @Query("SELECT * FROM exam_results ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ExamResultEntity>>
}
