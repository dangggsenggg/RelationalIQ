package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_progress")
data class StageProgressEntity(
    @PrimaryKey
    val stageId: Int,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestAccuracy: Float = 0f,
    val bestTimeMs: Long = 0,
    val attempts: Int = 0,
    val completedAt: Long? = null
)
