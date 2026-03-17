package com.expensio.data.repository

import com.expensio.data.remote.api.ExpenseApi
import com.expensio.data.remote.dto.ExpenseCreateRequest
import com.expensio.data.remote.dto.SplitInputDto
import com.expensio.domain.model.Balance
import com.expensio.domain.model.Expense
import com.expensio.domain.model.ExpenseDetail
import com.expensio.domain.model.ExpenseSplitDetail
import com.expensio.domain.repository.ExpenseRepository
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
) : ExpenseRepository {

    override suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        category: String,
        paidByUserId: String?,
        paidByGuestId: String?,
        splitType: String,
        splits: List<Map<String, Any>>,
    ): Result<Expense> = runCatching {
        val splitDtos = splits.map { s ->
            SplitInputDto(
                userId = s["user_id"] as? String,
                guestId = s["guest_id"] as? String,
                amount = s["amount"] as? Double,
                percentage = s["percentage"] as? Double,
            )
        }
        val dto = api.createExpense(
            groupId,
            ExpenseCreateRequest(title, amount, category, paidByUserId, paidByGuestId, splitType, splitDtos),
        )
        dto.toDomain()
    }

    override suspend fun getGroupExpenses(groupId: String, skip: Int, limit: Int): Result<List<Expense>> =
        runCatching { api.getGroupExpenses(groupId, skip, limit).map { it.toDomain() } }

    override suspend fun getExpenseDetail(expenseId: String): Result<ExpenseDetail> = runCatching {
        val dto = api.getExpenseDetail(expenseId)
        ExpenseDetail(
            expense = dto.run {
                Expense(id, groupId, title, amount, category, paidByUserId, paidByGuestId, paidByName, splitType, createdAt)
            },
            splits = dto.splits.map {
                ExpenseSplitDetail(it.id, it.userId, it.guestId, it.memberName, it.amount, it.percentage)
            },
        )
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        api.deleteExpense(expenseId); Unit
    }

    override suspend fun getGroupBalances(groupId: String): Result<List<Balance>> = runCatching {
        api.getGroupBalances(groupId).map { Balance(it.userId, it.guestId, it.memberName, it.netAmount) }
    }

    private fun com.expensio.data.remote.dto.ExpenseResponseDto.toDomain() =
        Expense(id, groupId, title, amount, category, paidByUserId, paidByGuestId, paidByName, splitType, createdAt)
}
