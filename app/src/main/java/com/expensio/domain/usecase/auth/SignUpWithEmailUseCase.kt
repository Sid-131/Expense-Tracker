package com.expensio.domain.usecase.auth

import android.util.Patterns
import com.expensio.domain.model.User
import com.expensio.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return Result.failure(IllegalArgumentException("Invalid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        return authRepository.signUpWithEmail(email.trim(), password, name.trim())
    }
}
