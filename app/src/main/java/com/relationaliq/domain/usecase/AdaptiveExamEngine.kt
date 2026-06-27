package com.relationaliq.domain.usecase

import com.relationaliq.domain.model.AdaptiveState
import com.relationaliq.domain.model.Difficulty
import com.relationaliq.domain.model.Exam
import com.relationaliq.domain.model.ExamResult
import com.relationaliq.domain.model.ExamTrialState
import com.relationaliq.domain.model.Premise
import com.relationaliq.domain.model.RelationScore
import com.relationaliq.domain.model.RelationType
import com.relationaliq.domain.model.Stage
import com.relationaliq.domain.model.Trial
import com.relationaliq.domain.repository.ExamRepository
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.TrainingRepository
import com.relationaliq.domain.repository.UserRepository
import javax.inject.Inject

class AdaptiveExamEngine @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val examRepository: ExamRepository,
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository
) {
    private var adaptiveState = AdaptiveState()
    private var coveredStages: List<Stage> = emptyList()
    private var currentExam: Exam? = null
    private val usedTrialIds = mutableSetOf<String>()

    fun getExamForStageCheckpoint(completedStageId: Int): Exam? {
        val examNumber = completedStageId / STAGES_PER_EXAM
        if (completedStageId % STAGES_PER_EXAM != 0) return null
        val start = (examNumber - 1) * STAGES_PER_EXAM + 1
        val end = examNumber * STAGES_PER_EXAM
        return Exam(id = examNumber, stageRangeStart = start, stageRangeEnd = end)
    }

    suspend fun initializeExam(exam: Exam): Boolean {
        val stages = mutableListOf<Stage>()
        for (id in exam.stageRangeStart..exam.stageRangeEnd) {
            val stage = trainingRepository.getStage(id) ?: continue
            stages.add(stage)
        }
        if (stages.isEmpty()) return false

        coveredStages = stages
        currentExam = exam
        adaptiveState = AdaptiveState()
        usedTrialIds.clear()
        return true
    }

    fun generateNextTrial(): ExamTrialState? {
        val exam = currentExam ?: return null
        if (adaptiveState.questionsAnswered >= exam.totalQuestions) return null

        val difficulty = adaptiveState.currentDifficulty
        val weakRelations = findWeakRelationTypes()
        val trial = selectOrGenerateTrial(difficulty, weakRelations)
            ?: return null

        return ExamTrialState(
            trial = trial,
            difficultyAtPresentation = difficulty,
            sourceStageId = findSourceStage(trial)
        )
    }

    fun submitAnswer(trialState: ExamTrialState, userAnswer: Boolean): Boolean {
        val isCorrect = userAnswer == trialState.trial.correctAnswer

        val relationType = trialState.trial.probeRelation
        val currentScore = adaptiveState.relationTypeScores.getOrDefault(
            relationType, RelationScore(0, 0)
        )
        adaptiveState.relationTypeScores[relationType] = RelationScore(
            correct = currentScore.correct + if (isCorrect) 1 else 0,
            total = currentScore.total + 1
        )

        adaptiveState = adaptiveState.copy(
            questionsAnswered = adaptiveState.questionsAnswered + 1,
            correctCount = adaptiveState.correctCount + if (isCorrect) 1 else 0
        )

        updateDifficulty(isCorrect)
        return isCorrect
    }

    fun isExamComplete(): Boolean {
        val exam = currentExam ?: return true
        return adaptiveState.questionsAnswered >= exam.totalQuestions
    }

    suspend fun completeExam(): ExamResult? {
        val exam = currentExam ?: return null

        val accuracy = if (adaptiveState.questionsAnswered > 0)
            adaptiveState.correctCount.toFloat() / adaptiveState.questionsAnswered
        else 0f

        val passed = accuracy >= exam.passingThreshold
        val xpEarned = calculateExamXp(accuracy, adaptiveState.difficultyPath)

        val result = ExamResult(
            examId = exam.id,
            stageRangeStart = exam.stageRangeStart,
            stageRangeEnd = exam.stageRangeEnd,
            totalQuestions = adaptiveState.questionsAnswered,
            correctAnswers = adaptiveState.correctCount,
            accuracy = accuracy,
            passed = passed,
            difficultyPath = adaptiveState.difficultyPath.toList(),
            relationTypeScores = adaptiveState.relationTypeScores.toMap(),
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            xpEarned = xpEarned
        )

        examRepository.saveExamResult(result)

        if (passed) {
            val nextStageStart = exam.stageRangeEnd + 1
            progressRepository.unlockStage(nextStageStart)
            val profile = userRepository.getProfile()
            if (profile != null) {
                userRepository.addXp(profile.id, xpEarned)
            }
        }

        return result
    }

    fun getCurrentState(): AdaptiveState = adaptiveState

    private fun updateDifficulty(isCorrect: Boolean) {
        val difficulties = Difficulty.entries.sortedBy { it.ordinal_level }
        if (isCorrect) {
            adaptiveState = adaptiveState.copy(
                consecutiveCorrect = adaptiveState.consecutiveCorrect + 1,
                consecutiveWrong = 0
            )
            if (adaptiveState.consecutiveCorrect >= STREAK_THRESHOLD) {
                val currentIdx = difficulties.indexOf(adaptiveState.currentDifficulty)
                val newDifficulty = if (currentIdx < difficulties.lastIndex)
                    difficulties[currentIdx + 1] else adaptiveState.currentDifficulty
                adaptiveState = adaptiveState.copy(
                    currentDifficulty = newDifficulty,
                    consecutiveCorrect = 0
                )
                adaptiveState.difficultyPath.add(newDifficulty)
            }
        } else {
            adaptiveState = adaptiveState.copy(
                consecutiveWrong = adaptiveState.consecutiveWrong + 1,
                consecutiveCorrect = 0
            )
            if (adaptiveState.consecutiveWrong >= STREAK_THRESHOLD) {
                val currentIdx = difficulties.indexOf(adaptiveState.currentDifficulty)
                val newDifficulty = if (currentIdx > 0)
                    difficulties[currentIdx - 1] else adaptiveState.currentDifficulty
                adaptiveState = adaptiveState.copy(
                    currentDifficulty = newDifficulty,
                    consecutiveWrong = 0
                )
                adaptiveState.difficultyPath.add(newDifficulty)
            }
        }
    }

    private fun findWeakRelationTypes(): List<RelationType> {
        return adaptiveState.relationTypeScores.entries
            .filter { it.value.total >= 2 && it.value.accuracy < 0.5f }
            .map { it.key }
    }

    private fun selectOrGenerateTrial(
        difficulty: Difficulty,
        weakRelations: List<RelationType>
    ): Trial? {
        val candidateStages = coveredStages.sortedBy {
            Math.abs(it.difficulty.ordinal_level - difficulty.ordinal_level)
        }

        if (weakRelations.isNotEmpty()) {
            for (stage in candidateStages) {
                val trials = (stage.trainingTrials + stage.testTrials)
                    .filter { trial ->
                        trial.id !in usedTrialIds &&
                            trial.probeRelation in weakRelations
                    }
                if (trials.isNotEmpty()) {
                    val selected = trials.random()
                    usedTrialIds.add(selected.id)
                    return adaptTrialDifficulty(selected, difficulty)
                }
            }
        }

        for (stage in candidateStages) {
            val trials = (stage.trainingTrials + stage.testTrials)
                .filter { it.id !in usedTrialIds }
            if (trials.isNotEmpty()) {
                val selected = trials.random()
                usedTrialIds.add(selected.id)
                return adaptTrialDifficulty(selected, difficulty)
            }
        }

        return generateTrial(difficulty, weakRelations)
    }

    private fun adaptTrialDifficulty(trial: Trial, targetDifficulty: Difficulty): Trial {
        val targetPremiseCount = premiseCountForDifficulty(targetDifficulty)
        if (trial.premises.size == targetPremiseCount) return trial
        if (trial.premises.size > targetPremiseCount) return trial

        if (targetPremiseCount > trial.premises.size && trial.premises.isNotEmpty()) {
            val extendedPremises = trial.premises.toMutableList()
            val lastPremise = extendedPremises.last()
            var currentStim = lastPremise.stimulusB

            while (extendedPremises.size < targetPremiseCount) {
                val nextStim = generateStimulus()
                extendedPremises.add(
                    Premise(
                        stimulusA = currentStim,
                        relationType = trial.probeRelation,
                        stimulusB = nextStim
                    )
                )
                currentStim = nextStim
            }

            return trial.copy(
                id = "${trial.id}_adapted",
                premises = extendedPremises,
                probeStimB = currentStim
            )
        }

        return trial
    }

    private fun generateTrial(
        difficulty: Difficulty,
        preferredRelations: List<RelationType>
    ): Trial? {
        val availableRelations = if (preferredRelations.isNotEmpty()) {
            preferredRelations
        } else {
            coveredStages.flatMap { it.relationTypes }.distinct()
        }
        if (availableRelations.isEmpty()) return null

        val relationType = availableRelations.random()
        val premiseCount = premiseCountForDifficulty(difficulty)
        val stimuli = (0..premiseCount).map { generateStimulus() }

        val premises = (0 until premiseCount).map { i ->
            Premise(
                stimulusA = stimuli[i],
                relationType = relationType,
                stimulusB = stimuli[i + 1]
            )
        }

        val correctAnswer = shouldDerivedRelationHold(relationType, premiseCount)

        val probeRelation = if (correctAnswer) relationType else relationType
        val trialId = "exam_gen_${adaptiveState.questionsAnswered}_${System.nanoTime()}"

        return Trial(
            id = trialId,
            premises = premises,
            probeStimA = stimuli.first(),
            probeRelation = probeRelation,
            probeStimB = stimuli.last(),
            correctAnswer = correctAnswer,
            timeLimitSeconds = timeLimitForDifficulty(difficulty)
        )
    }

    private fun shouldDerivedRelationHold(
        relationType: RelationType,
        premiseCount: Int
    ): Boolean {
        return when (relationType) {
            RelationType.SAME -> true
            RelationType.MORE_THAN, RelationType.LESS_THAN,
            RelationType.BEFORE, RelationType.AFTER,
            RelationType.CONTAINS, RelationType.WITHIN -> true
            RelationType.OPPOSITE -> premiseCount % 2 == 0
            RelationType.DIFFERENT -> premiseCount == 1
        }
    }

    private fun findSourceStage(trial: Trial): Int {
        for (stage in coveredStages) {
            if (stage.trainingTrials.any { it.id == trial.id } ||
                stage.testTrials.any { it.id == trial.id }
            ) {
                return stage.id
            }
        }
        return coveredStages.firstOrNull()?.id ?: 0
    }

    private fun calculateExamXp(accuracy: Float, difficultyPath: List<Difficulty>): Int {
        val baseXp = 250
        val accuracyMultiplier = accuracy
        val avgDifficulty = if (difficultyPath.isNotEmpty()) {
            difficultyPath.map { it.ordinal_level }.average().toFloat()
        } else 3f
        val difficultyBonus = avgDifficulty / 3f
        return (baseXp * accuracyMultiplier * difficultyBonus).toInt()
    }

    companion object {
        const val STAGES_PER_EXAM = 10
        private const val STREAK_THRESHOLD = 2

        private val CONSONANTS = "BCDFGHJKLMNPQRSTVWXYZ".toList()
        private val VOWELS = "AEIOU".toList()

        fun generateStimulus(): String {
            val c1 = CONSONANTS.random()
            val v = VOWELS.random()
            val c2 = CONSONANTS.random()
            return "$c1$v$c2"
        }

        fun premiseCountForDifficulty(difficulty: Difficulty): Int = when (difficulty) {
            Difficulty.BEGINNER -> 1
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
            Difficulty.ADVANCED -> 3
            Difficulty.EXPERT -> 4
        }

        fun timeLimitForDifficulty(difficulty: Difficulty): Int = when (difficulty) {
            Difficulty.BEGINNER -> 45
            Difficulty.EASY -> 40
            Difficulty.MEDIUM -> 35
            Difficulty.HARD -> 30
            Difficulty.ADVANCED -> 25
            Difficulty.EXPERT -> 20
        }
    }
}
