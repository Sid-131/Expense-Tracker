package com.expensio.domain.repository

import com.expensio.domain.model.Balance
import com.expensio.domain.model.Expense
import com.expensio.domain.model.ExpenseDetail

interface ExpenseRepository {
    suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        category: String,
        paidByUserId: String?,
        paidByGuestId: String?,
        splitType: String,
        splits: List<Map<String, Any>>,
    ): Result<Expense>

    suspend fun getGroupExpenses(groupId: String, skip: Int = 0, limit: Int = 20): Result<List<Expense>>
    suspend fun getExpenseDetail(expenseId: String): Result<ExpenseDetail>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
    suspend fun getGroupBalances(groupId: String): Result<List<Balance>>
}
