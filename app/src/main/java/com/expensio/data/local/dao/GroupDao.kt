package com.expensio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.expensio.data.local.entity.GroupEntity

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    suspend fun getAll(): List<GroupEntity>
}
