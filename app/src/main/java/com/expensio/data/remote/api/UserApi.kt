package com.expensio.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("api/v1/users/fcm-token")
    suspend fun registerFcmToken(@Body body: Map<String, String>)
}
