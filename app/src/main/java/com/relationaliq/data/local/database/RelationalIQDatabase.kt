package com.relationaliq.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.relationaliq.data.local.dao.AchievementDao
import com.relationaliq.data.local.dao.ExamResultDao
import com.relationaliq.data.local.dao.StageProgressDao
import com.relationaliq.data.local.dao.TrainingSessionDao
import com.relationaliq.data.local.dao.TrialResultDao
import com.relationaliq.data.local.dao.UserProfileDao
import com.relationaliq.data.local.entity.AchievementEntity
import com.relationaliq.data.local.entity.ExamResultEntity
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
        AchievementEntity::class,
        ExamResultEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class RelationalIQDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun trialResultDao(): TrialResultDao
    abstract fun stageProgressDao(): StageProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun examResultDao(): ExamResultDao

    companion object {
        const val DATABASE_NAME = "relationaliq_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exam_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        examId INTEGER NOT NULL,
                        stageRangeStart INTEGER NOT NULL,
                        stageRangeEnd INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        correctAnswers INTEGER NOT NULL,
                        accuracy REAL NOT NULL,
                        passed INTEGER NOT NULL,
                        difficultyPathJson TEXT NOT NULL,
                        relationTypeScoresJson TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        xpEarned INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
