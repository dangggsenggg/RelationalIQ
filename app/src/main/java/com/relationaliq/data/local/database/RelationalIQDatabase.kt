package com.relationaliq.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.relationaliq.data.local.dao.AchievementDao
import com.relationaliq.data.local.dao.StageProgressDao
import com.relationaliq.data.local.dao.TrainingSessionDao
import com.relationaliq.data.local.dao.TrialResultDao
import com.relationaliq.data.local.dao.UserProfileDao
import com.relationaliq.data.local.entity.AchievementEntity
import com.relationaliq.data.local.entity.StageProgressEntity
import com.relationaliq.data.local.entity.TrainingSessionEntity
import com.relationaliq.data.local.entity.TrialResultEntity
import com.relationaliq.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        TrainingSessionEntity::class,
        TrialResultEntity::class,
        StageProgressEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RelationalIQDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun trialResultDao(): TrialResultDao
    abstract fun stageProgressDao(): StageProgressDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val DATABASE_NAME = "relationaliq_db"
    }
}
