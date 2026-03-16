package com.expensio.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensio.data.local.dao.UserDao
import com.expensio.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "expensio_db"
    }
}
