package com.ngopimalang.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.data.toUserMessage
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
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val role: String = "user"
) {
    val isAdmin: Boolean get() = role == "admin"
}

/**
 * ViewModel fondasi Login/Register/Profil.
 *
 * Alur: layar -> fungsi di sini -> [AuthRepository] -> Supabase Auth +
 * tabel `profiles`/`user_roles` -> state baru -> recompose.
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

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
        if (!ensureConfigured()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.signIn(state.email, state.password) }
                .onSuccess { account -> applyAccount(account) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun register() {
        val state = _uiState.value
        validate(state, requireName = true)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }
        if (!ensureConfigured()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.signUp(state.name, state.email, state.password)
            }
                .onSuccess { account ->
                    if (account != null) {
                        applyAccount(account)
                    } else {
                        // "Confirm email" aktif: akun dibuat, sesi belum diberikan.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                password = "",
                                errorMessage = "Akun dibuat. Cek email untuk konfirmasi, " +
                                    "lalu masuk kembali"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun logout() {
        val wasLoggedIn = _uiState.value.isLoggedIn
        _uiState.value = AuthUiState()
        if (!wasLoggedIn) return
        viewModelScope.launch {
            runCatching { repository.signOut() }
        }
    }

    private fun restoreSession() {
        if (!Supabase.isConfigured) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            runCatching { repository.restoreSession() }
                .onSuccess { account ->
                    if (account != null) applyAccount(account) else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    private fun applyAccount(account: Account) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = true,
                errorMessage = null,
                password = "",
                name = account.displayName,
                email = account.email,
                userId = account.id,
                role = account.role
            )
        }
    }

    private fun ensureConfigured(): Boolean {
        if (Supabase.isConfigured) return true
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = "Supabase belum diatur. Isi SUPABASE_URL dan " +
                    "SUPABASE_ANON_KEY di local.properties lalu build ulang"
            )
        }
        return false
    }

    private fun validate(state: AuthUiState, requireName: Boolean): String? = when {
        requireName && state.name.isBlank() -> "Nama lengkap wajib diisi"
        state.email.isBlank() -> "Email wajib diisi"
        !state.email.contains("@") || !state.email.contains(".") -> "Format email tidak valid"
        state.password.length < 6 -> "Password minimal 6 karakter"
        else -> null
    }
}
