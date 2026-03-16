package com.expensio.data.remote.interceptor

import com.expensio.data.local.prefs.TokenManager
import com.expensio.data.remote.dto.RefreshTokenRequest
import com.expensio.data.remote.dto.TokenResponse
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject

/**
 * Automatically refreshes the JWT access token when a 401 is received.
 * Uses a plain OkHttpClient (no interceptors) to avoid circular dependencies.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops
        if (response.responseCount >= 2) {
            tokenManager.clearTokens()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearTokens()
            return null
        }

        return try {
            val newTokens = refreshTokensSync(refreshToken) ?: run {
                tokenManager.clearTokens()
                return null
            }
            tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            tokenManager.clearTokens()
            null
        }
    }

    private fun refreshTokensSync(refreshToken: String): TokenResponse? {
        val body = gson.toJson(RefreshTokenRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${com.expensio.utils.Constants.BASE_URL}api/v1/auth/refresh")
            .post(body)
            .build()
        val refreshResponse = OkHttpClient().newCall(request).execute()
        if (!refreshResponse.isSuccessful) return null
        val json = refreshResponse.body?.string() ?: return null
        return gson.fromJson(json, TokenResponse::class.java)
    }

    private val Response.responseCount: Int
        get() = generateSequence(priorResponse) { it.priorResponse }.count() + 1
}
