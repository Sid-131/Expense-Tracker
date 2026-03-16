package com.expensio.data.remote.api

import com.expensio.data.remote.dto.GoogleAuthRequest
import com.expensio.data.remote.dto.LoginRequest
import com.expensio.data.remote.dto.OtpSendRequest
import com.expensio.data.remote.dto.OtpVerifyRequest
import com.expensio.data.remote.dto.RefreshTokenRequest
import com.expensio.data.remote.dto.SignupRequest
import com.expensio.data.remote.dto.TokenResponse
import com.expensio.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<TokenResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/v1/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<TokenResponse>

    @POST("api/v1/auth/otp/send")
    suspend fun sendOtp(@Body request: OtpSendRequest): Response<Unit>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserResponse>
}
