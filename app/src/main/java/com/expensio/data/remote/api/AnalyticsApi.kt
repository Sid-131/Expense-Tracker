package com.expensio.data.remote.api

import com.expensio.data.remote.dto.AnalyticsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("api/v1/users/analytics")
    suspend fun getAnalytics(@Query("range") range: String = "3m"): AnalyticsResponseDto
}
