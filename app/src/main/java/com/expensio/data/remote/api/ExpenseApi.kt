package com.expensio.data.remote.api

import com.expensio.data.remote.dto.BalanceResponseDto
import com.expensio.data.remote.dto.ExpenseCreateRequest
import com.expensio.data.remote.dto.ExpenseDetailResponseDto
import com.expensio.data.remote.dto.ExpenseResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpenseApi {

    @POST("api/v1/groups/{groupId}/expenses")
    suspend fun createExpense(
        @Path("groupId") groupId: String,
        @Body request: ExpenseCreateRequest,
    ): ExpenseResponseDto

    @GET("api/v1/groups/{groupId}/expenses")
    suspend fun getGroupExpenses(
        @Path("groupId") groupId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
    ): List<ExpenseResponseDto>

    @GET("api/v1/expenses/{expenseId}")
    suspend fun getExpenseDetail(@Path("expenseId") expenseId: String): ExpenseDetailResponseDto

    @DELETE("api/v1/expenses/{expenseId}")
    suspend fun deleteExpense(@Path("expenseId") expenseId: String): Response<Unit>

    @GET("api/v1/groups/{groupId}/balances")
    suspend fun getGroupBalances(@Path("groupId") groupId: String): List<BalanceResponseDto>
}
