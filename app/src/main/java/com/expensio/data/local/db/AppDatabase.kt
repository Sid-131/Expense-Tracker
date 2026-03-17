package com.expensio.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensio.data.local.dao.BalanceDao
import com.expensio.data.local.dao.ExpenseDao
import com.expensio.data.local.dao.GroupDao
import com.expensio.data.local.dao.PendingExpenseDao
import com.expensio.data.local.dao.SettlementDao
import com.expensio.data.local.dao.UserDao
import com.expensio.data.local.entity.BalanceEntity
import com.expensio.data.local.entity.ExpenseEntity
import com.expensio.data.local.entity.GroupEntity
import com.expensio.data.local.entity.PendingExpenseEntity
import com.expensio.data.local.entity.SettlementEntity
import com.expensio.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        SettlementEntity::class,
        PendingExpenseEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun settlementDao(): SettlementDao
    abstract fun pendingExpenseDao(): PendingExpenseDao

    companion object {
        const val DATABASE_NAME = "expensio_db"
    }
}
