package com.relationaliq.data.datasource

import android.content.Context
import android.util.Log
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
            stageJsonList.mapNotNull { stageJson ->
                try {
                    stageJson.toDomain()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse stage ${stageJson.id}: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stages from assets: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "StageDataSource"
    }
}

data class StageJson(
    val id: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val module: String? = null,
    val frame_type: String? = null,
    val sub_frame: String? = null,
    val relationTypes: List<String>? = null,
    val premiseCount: Int? = null,
    val derivation_depth: Int? = null,
    val difficulty: String? = null,
    val masteryThreshold: Float? = null,
    val timeLimitSeconds: Int? = null,
    val xpReward: Int? = null,
    val estimated_time_minutes: Int? = null,
    val trainingTrials: List<TrialJson>? = null,
    val testTrials: List<TrialJson>? = null
) {
    fun toDomain(): Stage = Stage(
        id = id ?: 0,
        title = title ?: "",
        description = description ?: "",
        module = module ?: "",
        frameType = frame_type ?: "",
        subFrame = sub_frame ?: "",
        relationTypes = relationTypes?.map { RelationType.valueOf(it) } ?: emptyList(),
        premiseCount = premiseCount ?: 1,
        derivationDepth = derivation_depth ?: 1,
        difficulty = Difficulty.valueOf(difficulty ?: "BEGINNER"),
        trainingTrials = trainingTrials?.map { it.toDomain() } ?: emptyList(),
        testTrials = testTrials?.map { it.toDomain() } ?: emptyList(),
        masteryThreshold = masteryThreshold ?: 0.85f,
        timeLimitSeconds = timeLimitSeconds ?: 30,
        xpReward = xpReward ?: 100,
        estimatedTimeMinutes = estimated_time_minutes ?: 4
    )
}

data class TrialJson(
    val id: String? = null,
    val premises: List<PremiseJson>? = null,
    val probeStimA: String? = null,
    val probeRelation: String? = null,
    val probeStimB: String? = null,
    val correctAnswer: Boolean? = null,
    val explanation: String? = null,
    val timeLimitSeconds: Int? = null
) {
    fun toDomain(): Trial = Trial(
        id = id ?: "",
        premises = premises?.map { it.toDomain() } ?: emptyList(),
        probeStimA = probeStimA ?: "",
        probeRelation = RelationType.valueOf(probeRelation ?: "SAME"),
        probeStimB = probeStimB ?: "",
        correctAnswer = correctAnswer ?: false,
        timeLimitSeconds = timeLimitSeconds ?: 30,
        explanation = explanation ?: ""
    )
}

data class PremiseJson(
    val stimulusA: String? = null,
    val relationType: String? = null,
    val stimulusB: String? = null
) {
    fun toDomain(): Premise = Premise(
        stimulusA = stimulusA ?: "",
        relationType = RelationType.valueOf(relationType ?: "SAME"),
        stimulusB = stimulusB ?: ""
    )
}
