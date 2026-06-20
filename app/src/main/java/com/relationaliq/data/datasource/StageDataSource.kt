package com.relationaliq.data.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.relationaliq.domain.model.Difficulty
import com.relationaliq.domain.model.Premise
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.Trial
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cachedStages: List<Stage>? = null

    suspend fun getAllStages(): List<Stage> {
        cachedStages?.let { return it }
        val stages = loadStagesFromAssets()
        cachedStages = stages
        return stages
    }

    suspend fun getStage(stageId: Int): Stage? =
        getAllStages().find { it.id == stageId }

    private fun loadStagesFromAssets(): List<Stage> {
        return try {
            val json = context.assets.open("stages.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<StageJson>>() {}.type
            val stageJsonList: List<StageJson> = gson.fromJson(json, type)
            stageJsonList.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class StageJson(
    val id: Int,
    val title: String,
    val description: String,
    val relationTypes: List<String>,
    val premiseCount: Int,
    val difficulty: String,
    val masteryThreshold: Float = 0.85f,
    val timeLimitSeconds: Int = 30,
    val xpReward: Int = 100,
    val trainingTrials: List<TrialJson>,
    val testTrials: List<TrialJson>
) {
    fun toDomain(): Stage = Stage(
        id = id,
        title = title,
        description = description,
        relationTypes = relationTypes.map { RelationType.valueOf(it) },
        premiseCount = premiseCount,
        difficulty = Difficulty.valueOf(difficulty),
        trainingTrials = trainingTrials.map { it.toDomain() },
        testTrials = testTrials.map { it.toDomain() },
        masteryThreshold = masteryThreshold,
        timeLimitSeconds = timeLimitSeconds,
        xpReward = xpReward
    )
}

data class TrialJson(
    val id: String,
    val premises: List<PremiseJson>,
    val probeStimA: String,
    val probeRelation: String,
    val probeStimB: String,
    val correctAnswer: Boolean,
    val explanation: String = "",
    val timeLimitSeconds: Int = 30
) {
    fun toDomain(): Trial = Trial(
        id = id,
        premises = premises.map { it.toDomain() },
        probeStimA = probeStimA,
        probeRelation = RelationType.valueOf(probeRelation),
        probeStimB = probeStimB,
        correctAnswer = correctAnswer,
        timeLimitSeconds = timeLimitSeconds,
        explanation = explanation
    )
}

data class PremiseJson(
    val stimulusA: String,
    val relationType: String,
    val stimulusB: String
) {
    fun toDomain(): Premise = Premise(
        stimulusA = stimulusA,
        relationType = RelationType.valueOf(relationType),
        stimulusB = stimulusB
    )
}
