package com.expensio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.expensio.data.local.entity.PendingExpenseEntity

@Dao
interface PendingExpenseDao {
    @Insert
    suspend fun insert(entity: PendingExpenseEntity)

    @Query("SELECT * FROM pending_expenses ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingExpenseEntity>

    @Query("DELETE FROM pending_expenses WHERE localId = :localId")
    suspend fun deleteById(localId: Long)

    @Query("SELECT COUNT(*) FROM pending_expenses")
    suspend fun count(): Int
}
