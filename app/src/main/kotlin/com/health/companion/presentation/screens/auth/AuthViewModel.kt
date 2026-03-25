package com.health.companion.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateEmail(value: String) {
        _email.value = value
        clearError()
    }

    fun updatePassword(value: String) {
        _password.value = value
        clearError()
    }

    fun updateName(value: String) {
        _name.value = value
        clearError()
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
        clearError()
    }

    fun updatePhone(value: String) {
        _phone.value = value
        clearError()
    }

    private fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun login() {
        if (!validateLoginInput()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = AuthUiState.Loading

                val result = authRepository.login(
                    email = _email.value.trim(),
                    password = _password.value
                )

                result.onSuccess { response ->
                    Timber.d("Login successful")
                    _uiState.value = AuthUiState.Success
                }.onFailure { e ->
                    Timber.e(e, "Login failed")
                    _uiState.value = AuthUiState.Error(
                        e.message ?: "Login failed. Please check your credentials."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Login failed. Please check your credentials."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register() {
        if (!validateRegisterInput()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = AuthUiState.Loading

                val result = authRepository.register(
                    email = _email.value.trim(),
                    password = _password.value,
                    name = _name.value.trim()
                )

                result.onSuccess { response ->
                    Timber.d("Registration successful")
                    _uiState.value = AuthUiState.Success
                }.onFailure { e ->
                    Timber.e(e, "Registration failed")
                    _uiState.value = AuthUiState.Error(
                        e.message ?: "Registration failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Registration failed")
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Registration failed. Please try again."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateLoginInput(): Boolean {
        return when {
            _email.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Email is required")
                false
            }
            !isValidEmail(_email.value) -> {
                _uiState.value = AuthUiState.Error("Please enter a valid email")
                false
            }
            _password.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Password is required")
                false
            }
            _password.value.length < 6 -> {
                _uiState.value = AuthUiState.Error("Пароль минимум 6 символов")
                false
            }
            else -> true
        }
    }

    private fun validateRegisterInput(): Boolean {
        return when {
            _name.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Name is required")
                false
            }
            _name.value.length < 2 -> {
                _uiState.value = AuthUiState.Error("Name must be at least 2 characters")
                false
            }
            _email.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Email is required")
                false
            }
            !isValidEmail(_email.value) -> {
                _uiState.value = AuthUiState.Error("Please enter a valid email")
                false
            }
            _password.value.isBlank() -> {
                _uiState.value = AuthUiState.Error("Password is required")
                false
            }
            _password.value.length < 6 -> {
                _uiState.value = AuthUiState.Error("Пароль минимум 6 символов")
                false
            }
            _password.value != _confirmPassword.value -> {
                _uiState.value = AuthUiState.Error("Passwords don't match")
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // ===== CHANGE PASSWORD =====

    private val _changePasswordState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmNewPassword = MutableStateFlow("")
    val confirmNewPassword: StateFlow<String> = _confirmNewPassword.asStateFlow()

    fun updateCurrentPassword(value: String) {
        _currentPassword.value = value
        if (_changePasswordState.value is ChangePasswordUiState.Error) {
            _changePasswordState.value = ChangePasswordUiState.Idle
        }
    }

    fun updateNewPassword(value: String) {
        _newPassword.value = value
        if (_changePasswordState.value is ChangePasswordUiState.Error) {
            _changePasswordState.value = ChangePasswordUiState.Idle
        }
    }

    fun updateConfirmNewPassword(value: String) {
        _confirmNewPassword.value = value
        if (_changePasswordState.value is ChangePasswordUiState.Error) {
            _changePasswordState.value = ChangePasswordUiState.Idle
        }
    }

    fun changePassword() {
        val current = _currentPassword.value
        val new = _newPassword.value
        val confirm = _confirmNewPassword.value

        when {
            current.isBlank() -> {
                _changePasswordState.value = ChangePasswordUiState.Error("Введите текущий пароль")
                return
            }
            new.isBlank() -> {
                _changePasswordState.value = ChangePasswordUiState.Error("Введите новый пароль")
                return
            }
            new.length < 6 -> {
                _changePasswordState.value = ChangePasswordUiState.Error("Пароль минимум 6 символов")
                return
            }
            new != confirm -> {
                _changePasswordState.value = ChangePasswordUiState.Error("Пароли не совпадают")
                return
            }
        }

        viewModelScope.launch {
            try {
                _changePasswordState.value = ChangePasswordUiState.Loading
                val result = authRepository.changePassword(current, new)
                result.onSuccess {
                    _changePasswordState.value = ChangePasswordUiState.Success
                    _currentPassword.value = ""
                    _newPassword.value = ""
                    _confirmNewPassword.value = ""
                }.onFailure { e ->
                    _changePasswordState.value = ChangePasswordUiState.Error(
                        e.message ?: "Не удалось сменить пароль"
                    )
                }
            } catch (e: Exception) {
                _changePasswordState.value = ChangePasswordUiState.Error(
                    e.message ?: "Не удалось сменить пароль"
                )
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordUiState.Idle
        _currentPassword.value = ""
        _newPassword.value = ""
        _confirmNewPassword.value = ""
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
        _email.value = ""
        _password.value = ""
        _name.value = ""
        _confirmPassword.value = ""
        _phone.value = ""
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class ChangePasswordUiState {
    object Idle : ChangePasswordUiState()
    object Loading : ChangePasswordUiState()
    object Success : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}
