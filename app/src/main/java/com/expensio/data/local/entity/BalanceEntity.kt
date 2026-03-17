package com.expensio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balances")
data class BalanceEntity(
    @PrimaryKey val id: String, // "$groupId-${userId ?: guestId}"
    val groupId: String,
    val userId: String?,
    val guestId: String?,
    val memberName: String,
    val netAmount: Double,
)
