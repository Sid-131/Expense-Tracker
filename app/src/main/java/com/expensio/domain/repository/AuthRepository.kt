package com.expensio.domain.repository

import com.expensio.domain.model.User

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, otp: String): Result<User>
    /** Calls GET /users/me — used on app start to rehydrate the current user. */
    suspend fun fetchCurrentUser(): Result<User>
    suspend fun signOut()
    /** Returns in-memory cached user. Null until fetchCurrentUser() or a login completes. */
    fun getCurrentUser(): User?
    fun isLoggedIn(): Boolean
}
