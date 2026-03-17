package com.expensio.data.repository

import com.expensio.data.remote.api.RecurringExpenseApi
import com.expensio.data.remote.dto.RecurringExpenseCreateRequest
import com.expensio.data.remote.dto.RecurringExpenseResponseDto
import com.expensio.data.remote.dto.SplitInputDto
import com.expensio.domain.model.RecurringExpense
import com.expensio.domain.repository.RecurringExpenseRepository
import javax.inject.Inject

class RecurringExpenseRepositoryImpl @Inject constructor(
    private val api: RecurringExpenseApi,
) : RecurringExpenseRepository {

    override suspend fun createRecurring(
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
    ): Result<RecurringExpense> = runCatching {
        api.createRecurring(
            groupId,
            RecurringExpenseCreateRequest(
                title = title,
                amount = amount,
                category = category,
                paidByUserId = paidByUserId,
                paidByGuestId = paidByGuestId,
                splitType = splitType,
                splits = splits.map { s ->
                    SplitInputDto(
                        userId = s["user_id"] as? String,
                        guestId = s["guest_id"] as? String,
                        amount = s["amount"] as? Double,
                        percentage = s["percentage"] as? Double,
                    )
                },
                frequency = frequency,
                startDate = startDate,
            )
        ).toDomain()
    }

    override suspend fun listRecurring(groupId: String): Result<List<RecurringExpense>> =
        runCatching { api.listRecurring(groupId).map { it.toDomain() } }

    override suspend fun deactivateRecurring(groupId: String, recurringId: String): Result<Unit> =
        runCatching { api.deactivateRecurring(groupId, recurringId); Unit }

    private fun RecurringExpenseResponseDto.toDomain() = RecurringExpense(
        id = id, groupId = groupId, title = title, amount = amount, category = category,
        paidByUserId = paidByUserId, paidByGuestId = paidByGuestId, paidByName = paidByName,
        splitType = splitType, frequency = frequency, nextDueDate = nextDueDate,
        isActive = isActive, createdAt = createdAt,
    )
}
