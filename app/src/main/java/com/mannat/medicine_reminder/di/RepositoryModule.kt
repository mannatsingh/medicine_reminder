package com.mannat.medicine_reminder.di

import com.mannat.medicine_reminder.data.repository.BackupRepositoryImpl
import com.mannat.medicine_reminder.data.repository.DoseLogRepositoryImpl
import com.mannat.medicine_reminder.data.repository.MedicineRepositoryImpl
import com.mannat.medicine_reminder.domain.repository.BackupRepository
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
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
    abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindDoseLogRepository(impl: DoseLogRepositoryImpl): DoseLogRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
