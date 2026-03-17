package com.expensio.di

import android.content.Context
import androidx.room.Room
import com.expensio.data.local.dao.BalanceDao
import com.expensio.data.local.dao.ExpenseDao
import com.expensio.data.local.dao.GroupDao
import com.expensio.data.local.dao.PendingExpenseDao
import com.expensio.data.local.dao.SettlementDao
import com.expensio.data.local.dao.UserDao
import com.expensio.data.local.db.AppDatabase
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides @Singleton fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()
    @Provides @Singleton fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides @Singleton fun provideBalanceDao(db: AppDatabase): BalanceDao = db.balanceDao()
    @Provides @Singleton fun provideSettlementDao(db: AppDatabase): SettlementDao = db.settlementDao()
    @Provides @Singleton fun providePendingExpenseDao(db: AppDatabase): PendingExpenseDao = db.pendingExpenseDao()
}
