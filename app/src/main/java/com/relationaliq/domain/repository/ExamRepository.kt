package com.relationaliq.domain.repository

import com.relationaliq.domain.model.ExamResult
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    suspend fun saveExamResult(result: ExamResult): Long
    suspend fun getExamResult(examId: Int): ExamResult?
    suspend fun hasPassedExam(examId: Int): Boolean
    fun observeAllExamResults(): Flow<List<ExamResult>>
}
