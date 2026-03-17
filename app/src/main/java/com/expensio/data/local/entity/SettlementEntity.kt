package com.expensio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromUserId: String?,
    val fromGuestId: String?,
    val fromName: String,
    val toUserId: String?,
    val toGuestId: String?,
    val toName: String,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val completedAt: String?,
)
