package com.relationaliq.di

import android.content.Context
import androidx.room.Room
import com.relationaliq.data.local.dao.AchievementDao
import com.relationaliq.data.local.dao.ExamResultDao
import com.relationaliq.data.local.dao.StageProgressDao
import com.relationaliq.data.local.dao.TrainingSessionDao
import com.relationaliq.data.local.dao.TrialResultDao
import com.relationaliq.data.local.dao.UserProfileDao
import com.relationaliq.data.local.database.RelationalIQDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RelationalIQDatabase =
        Room.databaseBuilder(
            context,
            RelationalIQDatabase::class.java,
            RelationalIQDatabase.DATABASE_NAME
        )
            .addMigrations(RelationalIQDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideUserProfileDao(db: RelationalIQDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideTrainingSessionDao(db: RelationalIQDatabase): TrainingSessionDao = db.trainingSessionDao()

    @Provides
    fun provideTrialResultDao(db: RelationalIQDatabase): TrialResultDao = db.trialResultDao()

    @Provides
    fun provideStageProgressDao(db: RelationalIQDatabase): StageProgressDao = db.stageProgressDao()

    @Provides
    fun provideAchievementDao(db: RelationalIQDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideExamResultDao(db: RelationalIQDatabase): ExamResultDao = db.examResultDao()
}
