package com.expensio.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.expensio.data.local.dao.BalanceDao
import com.expensio.data.local.dao.ExpenseDao
import com.expensio.data.local.dao.PendingExpenseDao
import com.expensio.data.local.entity.BalanceEntity
import com.expensio.data.local.entity.ExpenseEntity
import com.expensio.data.local.entity.PendingExpenseEntity
import com.expensio.data.remote.api.ExpenseApi
import com.expensio.data.remote.dto.ExpenseCreateRequest
import com.expensio.data.remote.dto.SplitInputDto
import com.expensio.domain.model.Balance
import com.expensio.domain.model.Expense
import com.expensio.domain.model.ExpenseDetail
import com.expensio.domain.model.ExpenseSplitDetail
import com.expensio.domain.repository.ExpenseRepository
import com.expensio.sync.SyncWorker
import com.expensio.utils.OfflineQueuedException
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
    private val expenseDao: ExpenseDao,
    private val balanceDao: BalanceDao,
    private val pendingExpenseDao: PendingExpenseDao,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
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
        try {
            val dto = api.createExpense(
                groupId,
                ExpenseCreateRequest(title, amount, category, paidByUserId, paidByGuestId, splitType, splitDtos),
            )
            val expense = dto.toDomain()
            expenseDao.upsertAll(listOf(expense.toEntity()))
            expense
        } catch (e: IOException) {
            pendingExpenseDao.insert(
                PendingExpenseEntity(
                    groupId = groupId,
                    title = title,
                    amount = amount,
                    category = category,
                    paidByUserId = paidByUserId,
                    paidByGuestId = paidByGuestId,
                    splitType = splitType,
                    splitsJson = gson.toJson(splits),
                )
            )
            enqueueSyncWork()
            throw OfflineQueuedException()
        }
    }

    override suspend fun getGroupExpenses(groupId: String, skip: Int, limit: Int): Result<List<Expense>> {
        return try {
            val list = api.getGroupExpenses(groupId, skip, limit).map { it.toDomain() }
            expenseDao.upsertAll(list.map { it.toEntity() })
            Result.success(list)
        } catch (e: Exception) {
            val cached = expenseDao.getByGroup(groupId)
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.failure(e)
        }
    }

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

    override suspend fun getGroupBalances(groupId: String): Result<List<Balance>> {
        return try {
            val list = api.getGroupBalances(groupId).map {
                Balance(it.userId, it.guestId, it.memberName, it.netAmount)
            }
            balanceDao.deleteByGroup(groupId)
            balanceDao.upsertAll(list.map { b ->
                BalanceEntity(
                    id = "$groupId-${b.userId ?: b.guestId}",
                    groupId = groupId,
                    userId = b.userId,
                    guestId = b.guestId,
                    memberName = b.memberName,
                    netAmount = b.netAmount,
                )
            })
            Result.success(list)
        } catch (e: Exception) {
            val cached = balanceDao.getByGroup(groupId)
            if (cached.isNotEmpty()) Result.success(cached.map {
                Balance(it.userId, it.guestId, it.memberName, it.netAmount)
            })
            else Result.failure(e)
        }
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SyncWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private fun com.expensio.data.remote.dto.ExpenseResponseDto.toDomain() =
        Expense(id, groupId, title, amount, category, paidByUserId, paidByGuestId, paidByName, splitType, createdAt)

    private fun Expense.toEntity() =
        ExpenseEntity(id, groupId, title, amount, category, paidByUserId, paidByGuestId, paidByName, splitType, createdAt)

    private fun ExpenseEntity.toDomain() =
        Expense(id, groupId, title, amount, category, paidByUserId, paidByGuestId, paidByName, splitType, createdAt)
}
