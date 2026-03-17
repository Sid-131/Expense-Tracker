package com.expensio.data.remote.api

import com.expensio.data.remote.dto.SettlementCreateRequest
import com.expensio.data.remote.dto.SettlementResponseDto
import com.expensio.data.remote.dto.SettlementSuggestionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SettlementApi {

    @GET("api/v1/groups/{groupId}/settlements/suggestions")
    suspend fun getSuggestions(@Path("groupId") groupId: String): List<SettlementSuggestionDto>

    @POST("api/v1/groups/{groupId}/settlements")
    suspend fun createSettlement(
        @Path("groupId") groupId: String,
        @Body request: SettlementCreateRequest,
    ): SettlementResponseDto

    @GET("api/v1/groups/{groupId}/settlements")
    suspend fun getGroupSettlements(@Path("groupId") groupId: String): List<SettlementResponseDto>

    @PATCH("api/v1/settlements/{settlementId}/complete")
    suspend fun completeSettlement(@Path("settlementId") settlementId: String): SettlementResponseDto
}
