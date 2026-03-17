package com.expensio.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensio.data.local.dao.PendingExpenseDao
import com.expensio.data.remote.api.ExpenseApi
import com.expensio.data.remote.dto.ExpenseCreateRequest
import com.expensio.data.remote.dto.SplitInputDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingExpenseDao: PendingExpenseDao,
    private val expenseApi: ExpenseApi,
    private val gson: Gson,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = pendingExpenseDao.getAll()
        for (p in pending) {
            try {
                val splitsType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val splits: List<Map<String, Any>> = gson.fromJson(p.splitsJson, splitsType)
                val splitDtos = splits.map { s ->
                    SplitInputDto(
                        userId = s["user_id"] as? String,
                        guestId = s["guest_id"] as? String,
                        amount = (s["amount"] as? Double),
                        percentage = (s["percentage"] as? Double),
                    )
                }
                expenseApi.createExpense(
                    p.groupId,
                    ExpenseCreateRequest(
                        title = p.title,
                        amount = p.amount,
                        category = p.category,
                        paidByUserId = p.paidByUserId,
                        paidByGuestId = p.paidByGuestId,
                        splitType = p.splitType,
                        splits = splitDtos,
                    ),
                )
                pendingExpenseDao.deleteById(p.localId)
            } catch (_: Exception) {
                // leave it for the next run
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "sync_pending_expenses"
    }
}
