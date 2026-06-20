package com.relationaliq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
