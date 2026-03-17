package com.expensio.di

import com.expensio.data.repository.AnalyticsRepositoryImpl
import com.expensio.data.repository.AuthRepositoryImpl
import com.expensio.data.repository.ExpenseRepositoryImpl
import com.expensio.data.repository.GroupRepositoryImpl
import com.expensio.data.repository.RecurringExpenseRepositoryImpl
import com.expensio.data.repository.SettlementRepositoryImpl
import com.expensio.domain.repository.AnalyticsRepository
import com.expensio.domain.repository.AuthRepository
import com.expensio.domain.repository.ExpenseRepository
import com.expensio.domain.repository.GroupRepository
import com.expensio.domain.repository.RecurringExpenseRepository
import com.expensio.domain.repository.SettlementRepository
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSettlementRepository(
        settlementRepositoryImpl: SettlementRepositoryImpl
    ): SettlementRepository

    @Binds
    @Singleton
    abstract fun bindRecurringExpenseRepository(
        recurringExpenseRepositoryImpl: RecurringExpenseRepositoryImpl
    ): RecurringExpenseRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
}
