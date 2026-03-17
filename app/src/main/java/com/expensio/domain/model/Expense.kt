package com.expensio.domain.model

data class Expense(
    val id: String,
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

data class ExpenseSplitDetail(
    val id: String,
    val userId: String?,
    val guestId: String?,
    val memberName: String,
    val amount: Double,
    val percentage: Double?,
)

data class ExpenseDetail(
    val expense: Expense,
    val splits: List<ExpenseSplitDetail>,
)

data class Balance(
    val userId: String?,
    val guestId: String?,
    val memberName: String,
    val netAmount: Double,
)

enum class SplitType { EQUAL, PERCENTAGE, EXACT }

val EXPENSE_CATEGORIES = listOf(
    "FOOD", "TRANSPORT", "SHOPPING", "ENTERTAINMENT",
    "HEALTH", "UTILITIES", "RENT", "OTHER"
)
