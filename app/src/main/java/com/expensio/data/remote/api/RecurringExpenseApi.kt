package com.expensio.data.remote.api

import com.expensio.data.remote.dto.RecurringExpenseCreateRequest
import com.expensio.data.remote.dto.RecurringExpenseResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RecurringExpenseApi {

    @POST("api/v1/groups/{groupId}/recurring")
    suspend fun createRecurring(
        @Path("groupId") groupId: String,
        @Body request: RecurringExpenseCreateRequest,
    ): RecurringExpenseResponseDto

    @GET("api/v1/groups/{groupId}/recurring")
    suspend fun listRecurring(
        @Path("groupId") groupId: String,
    ): List<RecurringExpenseResponseDto>

    @DELETE("api/v1/groups/{groupId}/recurring/{recurringId}")
    suspend fun deactivateRecurring(
        @Path("groupId") groupId: String,
        @Path("recurringId") recurringId: String,
    )
}
