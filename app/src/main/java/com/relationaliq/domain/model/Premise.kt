package com.relationaliq.domain.model

data class Premise(
    val stimulusA: String,
    val relationType: RelationType,
    val stimulusB: String
) {
    fun toDisplayString(): String = "$stimulusA is ${relationType.displayName} $stimulusB"
}
