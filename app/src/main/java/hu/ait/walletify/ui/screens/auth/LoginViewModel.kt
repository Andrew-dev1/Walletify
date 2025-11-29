package hu.ait.walletify.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.UserProfile
import hu.ait.walletify.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


sealed interface LoginUiState{
    object Init: LoginUiState
    object Loading: LoginUiState
    object RegisterSuccess: LoginUiState
    data class LoginSuccess(val profile: UserProfile) : LoginUiState
    data class Error(val errorMessage: String?): LoginUiState

}



@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Init)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    // Also keep the activeUser flow from repository
    val activeUser = authRepository.activeUser

    fun registerUser(name: String, email: String, password: String, source: String, purpose: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading

            authRepository.register(name, email, password, source, purpose).fold(
                onSuccess = { profile ->
                    _loginUiState.value = LoginUiState.RegisterSuccess
                },
                onFailure = { error ->
                    _loginUiState.value = LoginUiState.Error(
                        error.localizedMessage ?: "Registration failed"
                    )
                }
            )
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading

            authRepository.login(email, password).fold(
                onSuccess = { profile ->
                    _loginUiState.value = LoginUiState.LoginSuccess(profile)
                },
                onFailure = { error ->
                    _loginUiState.value = LoginUiState.Error(
                        error.localizedMessage ?: "Login failed"
                    )
                }
            )
        }
    }

    fun forgetPassword(email: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading

            authRepository.sendPasswordReset(email).fold(
                onSuccess = {
                    // You might want a specific state for password reset success
                    _loginUiState.value = LoginUiState.Init
                },
                onFailure = { error ->
                    _loginUiState.value = LoginUiState.Error(
                        error.localizedMessage ?: "Failed to send reset email"
                    )
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _loginUiState.value = LoginUiState.Init
    }

    fun resetState() {
        _loginUiState.value = LoginUiState.Init
    }
}