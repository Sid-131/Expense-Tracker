package com.expensio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensio.data.local.entity.SettlementEntity

@Dao
interface SettlementDao {
    @Upsert
    suspend fun upsertAll(settlements: List<SettlementEntity>)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getByGroup(groupId: String): List<SettlementEntity>
}
