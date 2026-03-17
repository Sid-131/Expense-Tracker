package com.expensio.domain.repository

import com.expensio.domain.model.RecurringExpense

interface RecurringExpenseRepository {
    suspend fun createRecurring(
        groupId: String,
        title: String,
        amount: Double,
        category: String,
        paidByUserId: String?,
        paidByGuestId: String?,
        splitType: String,
        splits: List<Map<String, Any>>,
        frequency: String,
        startDate: String,
    ): Result<RecurringExpense>

    suspend fun listRecurring(groupId: String): Result<List<RecurringExpense>>
    suspend fun deactivateRecurring(groupId: String, recurringId: String): Result<Unit>
}
