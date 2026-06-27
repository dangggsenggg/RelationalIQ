package com.relationaliq.di

import com.relationaliq.data.repository.ExamRepositoryImpl
import com.relationaliq.data.repository.ProgressRepositoryImpl
import com.relationaliq.data.repository.TrainingRepositoryImpl
import com.relationaliq.data.repository.UserRepositoryImpl
import com.relationaliq.domain.repository.ExamRepository
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.TrainingRepository
import com.relationaliq.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(impl: TrainingRepositoryImpl): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository
}
