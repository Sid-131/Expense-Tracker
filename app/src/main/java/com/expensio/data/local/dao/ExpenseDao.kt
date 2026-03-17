package com.expensio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensio.data.local.entity.ExpenseEntity

@Dao
interface ExpenseDao {
    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getByGroup(groupId: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}
