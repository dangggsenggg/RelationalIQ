package com.relationaliq.domain.model

data class Trial(
    val id: String,
    val premises: List<Premise>,
    val probeStimA: String,
    val probeRelation: RelationType,
    val probeStimB: String,
    val correctAnswer: Boolean,
    val timeLimitSeconds: Int = 30,
    val explanation: String = ""
) {
    fun probeDisplayString(): String =
        "Is $probeStimA ${probeRelation.displayName} $probeStimB?"
}
