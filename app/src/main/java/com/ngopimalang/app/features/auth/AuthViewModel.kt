package com.ngopimalang.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

/**
 * ViewModel fondasi Login/Register/Profil.
 * Progress 1: autentikasi masih dummy (delay), diganti Supabase Auth pada Progress 2.
 */
class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        validate(state, requireName = false)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }
        authenticate(displayName = state.email.substringBefore("@"))
    }

    fun register() {
        val state = _uiState.value
        validate(state, requireName = true)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }
        authenticate(displayName = state.name.trim())
    }

    fun logout() {
        _uiState.value = AuthUiState()
    }

    private fun authenticate(displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(700) // TODO Progress 2: ganti dengan supabase.auth.signIn / signUp
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    name = displayName
                )
            }
        }
    }

    private fun validate(state: AuthUiState, requireName: Boolean): String? = when {
        requireName && state.name.isBlank() -> "Nama lengkap wajib diisi"
        state.email.isBlank() -> "Email wajib diisi"
        !state.email.contains("@") || !state.email.contains(".") -> "Format email tidak valid"
        state.password.length < 6 -> "Password minimal 6 karakter"
        else -> null
    }
}
