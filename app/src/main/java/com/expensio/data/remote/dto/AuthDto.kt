package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

data class OtpSendRequest(
    val phone: String
)

data class OtpVerifyRequest(
    val phone: String,
    val otp: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer"
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("created_at") val createdAt: String
)
