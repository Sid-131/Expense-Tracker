package com.expensio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensio.data.local.entity.BalanceEntity

@Dao
interface BalanceDao {
    @Upsert
    suspend fun upsertAll(balances: List<BalanceEntity>)

    @Query("SELECT * FROM balances WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: String): List<BalanceEntity>

    @Query("DELETE FROM balances WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}
