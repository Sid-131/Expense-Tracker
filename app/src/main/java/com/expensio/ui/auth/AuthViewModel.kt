package com.expensio.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.User
import com.expensio.domain.repository.AuthRepository
import com.expensio.domain.usecase.auth.GetCurrentUserUseCase
import com.expensio.domain.usecase.auth.SignInWithEmailUseCase
import com.expensio.domain.usecase.auth.SignInWithGoogleUseCase
import com.expensio.domain.usecase.auth.SignOutUseCase
import com.expensio.domain.usecase.auth.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signUpWithEmail: SignUpWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signOut: SignOutUseCase,
    private val getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val signupState: StateFlow<AuthUiState> = _signupState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // If a token exists, rehydrate the current user from the backend
        if (authRepository.isLoggedIn()) {
            viewModelScope.launch(Dispatchers.IO) {
                authRepository.fetchCurrentUser()
                    .onSuccess { _currentUser.value = it }
                    .onFailure {
                        // Token invalid/expired — force re-login
                        authRepository.signOut()
                        _currentUser.value = null
                    }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = AuthUiState.Loading
            signInWithEmail(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _loginState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _loginState.value = AuthUiState.Error(e.message ?: "Login failed")
                }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _signupState.value = AuthUiState.Loading
            signUpWithEmail(email, password, name)
                .onSuccess { user ->
                    _currentUser.value = user
                    _signupState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _signupState.value = AuthUiState.Error(e.message ?: "Sign up failed")
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = AuthUiState.Loading
            signInWithGoogle.invoke(idToken)
                .onSuccess { user ->
                    _currentUser.value = user
                    _loginState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _loginState.value = AuthUiState.Error(e.message ?: "Google sign-in failed")
                }
        }
    }

    fun doSignOut() {
        viewModelScope.launch(Dispatchers.IO) {
            signOut()
            _currentUser.value = null
            _loginState.value = AuthUiState.Idle
            _signupState.value = AuthUiState.Idle
        }
    }

    fun resetLoginState() { _loginState.value = AuthUiState.Idle }
    fun resetSignupState() { _signupState.value = AuthUiState.Idle }
}
