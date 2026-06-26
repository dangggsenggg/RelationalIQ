package com.relationaliq.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.relationaliq.data.local.dao.ExamResultDao
import com.relationaliq.data.local.entity.ExamResultEntity
import com.relationaliq.domain.model.Difficulty
import com.relationaliq.domain.model.ExamResult
import com.relationaliq.domain.model.RelationScore
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExamRepositoryImpl @Inject constructor(
    private val examResultDao: ExamResultDao,
    private val gson: Gson
) : ExamRepository {

    override suspend fun saveExamResult(result: ExamResult): Long {
        val entity = result.toEntity()
        return examResultDao.insert(entity)
    }

    override suspend fun getExamResult(examId: Int): ExamResult? {
        return examResultDao.getLatestResult(examId)?.toDomain()
    }

    override suspend fun hasPassedExam(examId: Int): Boolean {
        return examResultDao.hasPassedExam(examId)
    }

    override fun observeAllExamResults(): Flow<List<ExamResult>> {
        return examResultDao.observeAll().map { entities ->
            entities.mapNotNull { it.toDomain() }
        }
    }

    private fun ExamResult.toEntity(): ExamResultEntity {
        val difficultyNames = difficultyPath.map { it.name }
        val scoresMap = relationTypeScores.map { (k, v) ->
            k.name to mapOf("correct" to v.correct, "total" to v.total)
        }.toMap()

        return ExamResultEntity(
            id = id,
            examId = examId,
            stageRangeStart = stageRangeStart,
            stageRangeEnd = stageRangeEnd,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            accuracy = accuracy,
            passed = passed,
            difficultyPathJson = gson.toJson(difficultyNames),
            relationTypeScoresJson = gson.toJson(scoresMap),
            startTime = startTime,
            endTime = endTime ?: System.currentTimeMillis(),
            xpEarned = xpEarned
        )
    }

    private fun ExamResultEntity.toDomain(): ExamResult? {
        return try {
            val difficultyListType = object : TypeToken<List<String>>() {}.type
            val difficultyNames: List<String> = gson.fromJson(difficultyPathJson, difficultyListType)
            val difficulties = difficultyNames.map { Difficulty.valueOf(it) }

            val scoresMapType = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
            val scoresRaw: Map<String, Map<String, Int>> = gson.fromJson(relationTypeScoresJson, scoresMapType)
            val scores = scoresRaw.mapNotNull { (key, value) ->
                try {
                    val relType = RelationType.valueOf(key)
                    val score = RelationScore(
                        correct = value["correct"] ?: 0,
                        total = value["total"] ?: 0
                    )
                    relType to score
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toMap()

            ExamResult(
                id = id,
                examId = examId,
                stageRangeStart = stageRangeStart,
                stageRangeEnd = stageRangeEnd,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                accuracy = accuracy,
                passed = passed,
                difficultyPath = difficulties,
                relationTypeScores = scores,
                startTime = startTime,
                endTime = endTime,
                xpEarned = xpEarned
            )
        } catch (e: Exception) {
            null
        }
    }
}
