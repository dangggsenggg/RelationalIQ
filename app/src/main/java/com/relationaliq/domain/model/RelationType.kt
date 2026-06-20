package com.relationaliq.domain.model

enum class RelationType(val displayName: String, val symbol: String) {
    SAME("Same As", "="),
    DIFFERENT("Different From", "≠"),
    OPPOSITE("Opposite Of", "↔"),
    MORE_THAN("More Than", ">"),
    LESS_THAN("Less Than", "<"),
    BEFORE("Before", "←"),
    AFTER("After", "→"),
    CONTAINS("Contains", "⊃"),
    WITHIN("Within", "⊂");

    fun inverse(): RelationType = when (this) {
        SAME -> SAME
        DIFFERENT -> DIFFERENT
        OPPOSITE -> OPPOSITE
        MORE_THAN -> LESS_THAN
        LESS_THAN -> MORE_THAN
        BEFORE -> AFTER
        AFTER -> BEFORE
        CONTAINS -> WITHIN
        WITHIN -> CONTAINS
    }

    fun isSymmetric(): Boolean = this in setOf(SAME, DIFFERENT, OPPOSITE)
}
