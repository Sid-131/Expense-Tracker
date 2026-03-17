package com.expensio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_expenses")
data class PendingExpenseEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val paidByUserId: String?,
    val paidByGuestId: String?,
    val splitType: String,
    val splitsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)
