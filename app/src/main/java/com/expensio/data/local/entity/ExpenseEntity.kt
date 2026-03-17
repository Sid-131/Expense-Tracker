package com.expensio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val paidByUserId: String?,
    val paidByGuestId: String?,
    val paidByName: String,
    val splitType: String,
    val createdAt: String,
)
