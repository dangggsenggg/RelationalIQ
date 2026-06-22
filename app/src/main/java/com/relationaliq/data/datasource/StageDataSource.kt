package com.relationaliq.data.datasource

import android.content.Context
import com.google.gson.Gson
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
    private var stageIndex: StageIndex? = null

    suspend fun getAllStages(): List<Stage> {
        cachedStages?.let { return it }
        val stages = loadAllStagesFromAssets()
        cachedStages = stages
        return stages
    }

    suspend fun getStage(stageId: Int): Stage? {
        cachedStages?.find { it.id == stageId }?.let { return it }
        return loadSingleStage(stageId)
    }

    suspend fun getStageCount(): Int {
        return getIndex().totalStages
    }

    private fun getIndex(): StageIndex {
        stageIndex?.let { return it }
        val index = loadIndex()
        stageIndex = index
        return index
    }

    private fun loadIndex(): StageIndex {
        return try {
            val json = context.assets.open("stages/index.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, StageIndex::class.java)
        } catch (e: Exception) {
            StageIndex(version = "1.0", totalStages = 0, stages = emptyList())
        }
    }

    private fun loadSingleStage(stageId: Int): Stage? {
        val index = getIndex()
        val entry = index.stages.find { it.stageId == stageId } ?: return null
        return try {
            val json = context.assets.open("stages/${entry.file}")
                .bufferedReader().use { it.readText() }
            val stageJson: StageJson = gson.fromJson(json, StageJson::class.java)
            stageJson.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    private fun loadAllStagesFromAssets(): List<Stage> {
        val index = getIndex()
        return index.stages.mapNotNull { entry ->
            try {
                val json = context.assets.open("stages/${entry.file}")
                    .bufferedReader().use { it.readText() }
                val stageJson: StageJson = gson.fromJson(json, StageJson::class.java)
                stageJson.toDomain()
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class StageIndex(
    val version: String,
    val totalStages: Int,
    val stages: List<StageIndexEntry>
)

data class StageIndexEntry(
    val stageId: Int,
    val level: Int,
    val title: String,
    val difficulty: String,
    val file: String
)

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
