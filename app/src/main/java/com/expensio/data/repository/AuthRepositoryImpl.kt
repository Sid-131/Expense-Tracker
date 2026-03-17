package com.expensio.data.repository

import com.expensio.data.local.dao.UserDao
import com.expensio.data.local.prefs.TokenManager
import com.expensio.data.mapper.toDomain
import com.expensio.data.mapper.toEntity
import com.expensio.data.remote.api.AuthApi
import com.expensio.data.remote.api.UserApi
import com.expensio.data.remote.dto.GoogleAuthRequest
import com.expensio.data.remote.dto.LoginRequest
import com.expensio.data.remote.dto.OtpSendRequest
import com.expensio.data.remote.dto.OtpVerifyRequest
import com.expensio.data.remote.dto.SignupRequest
import com.expensio.domain.model.User
import com.expensio.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val firebaseMessaging: FirebaseMessaging,
) : AuthRepository {

    /** In-memory cache — avoids blocking DB reads for synchronous getCurrentUser() calls. */
    private var _currentUser: User? = null

    private suspend fun handleTokenResponse(response: retrofit2.Response<com.expensio.data.remote.dto.TokenResponse>): Result<User> {
        if (!response.isSuccessful) {
            val error = response.errorBody()?.string() ?: "Unknown error"
            return Result.failure(Exception(error))
        }
        val tokens = response.body() ?: return Result.failure(Exception("Empty response"))
        tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
        return fetchCurrentUser()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            handleTokenResponse(authApi.login(LoginRequest(email, password)))
        } catch (e: Exception) {
            Timber.e(e, "signInWithEmail failed")
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            handleTokenResponse(authApi.signup(SignupRequest(name, email, password)))
        } catch (e: Exception) {
            Timber.e(e, "signUpWithEmail failed")
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            handleTokenResponse(authApi.googleAuth(GoogleAuthRequest(idToken)))
        } catch (e: Exception) {
            Timber.e(e, "signInWithGoogle failed")
            Result.failure(e)
        }
    }

    override suspend fun sendOtp(phone: String): Result<Unit> {
        return try {
            val response = authApi.sendOtp(OtpSendRequest(phone))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "Failed to send OTP"))
        } catch (e: Exception) {
            Timber.e(e, "sendOtp failed")
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<User> {
        return try {
            handleTokenResponse(authApi.verifyOtp(OtpVerifyRequest(phone, otp)))
        } catch (e: Exception) {
            Timber.e(e, "verifyOtp failed")
            Result.failure(e)
        }
    }

    override suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val response = authApi.getMe()
            if (!response.isSuccessful) {
                return Result.failure(Exception(response.errorBody()?.string() ?: "Unauthorized"))
            }
            val userDto = response.body() ?: return Result.failure(Exception("Empty user response"))
            val user = userDto.toDomain()
            _currentUser = user
            userDao.insertUser(user.toEntity())
            registerFcmToken()
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "fetchCurrentUser failed")
            Result.failure(e)
        }
    }

    private suspend fun registerFcmToken() {
        runCatching {
            val token = firebaseMessaging.token.await()
            userApi.registerFcmToken(mapOf("token" to token))
        }.onFailure { Timber.w(it, "FCM token registration failed") }
    }

    override suspend fun signOut() {
        try { authApi.logout() } catch (_: Exception) {}
        tokenManager.clearTokens()
        _currentUser = null
    }

    override fun getCurrentUser(): User? = _currentUser

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
