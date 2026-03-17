package com.expensio.domain.model

data class RecurringExpense(
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val paidByUserId: String?,
    val paidByGuestId: String?,
    val paidByName: String,
    val splitType: String,
    val frequency: String,
    val nextDueDate: String,
    val isActive: Boolean,
    val createdAt: String,
)

enum class Frequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}
