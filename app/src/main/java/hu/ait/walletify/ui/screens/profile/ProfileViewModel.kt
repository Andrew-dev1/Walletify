package hu.ait.walletify.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.UserProfile
import hu.ait.walletify.data.plaid.PlaidRepository
import hu.ait.walletify.data.repository.AuthRepository
import hu.ait.walletify.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Data(
        val user: UserProfile,
        val plaidAccounts: List<PlaidAccount>,
        val linkToken: String?,
        val isLinking: Boolean,
        val statusMessage: String?
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val financeRepository: FinanceRepository,
    private val plaidRepository: PlaidRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                authRepository.activeUser,
                financeRepository.observePlaidAccounts()
            ) { user, accounts ->
                user to accounts
            }.collect { (user, accounts) ->
                val existing = _state.value
                if (user == null) {
                    _state.value = ProfileUiState.Error("Please login again.")
                } else {
                    _state.value = ProfileUiState.Data(
                        user = user,
                        plaidAccounts = accounts,
                        linkToken = (existing as? ProfileUiState.Data)?.linkToken,
                        isLinking = false,
                        statusMessage = (existing as? ProfileUiState.Data)?.statusMessage
                    )
                }
            }
        }
    }

    fun updateProfile(displayName: String, householdMembers: Int) {
        viewModelScope.launch {
            val current = _state.value as? ProfileUiState.Data ?: return@launch

            try {
                authRepository.updateUserProfile(displayName, householdMembers)
                _state.value = current.copy(
                    statusMessage = "Profile updated successfully!"
                )
            } catch (e: Exception) {
                _state.value = current.copy(
                    statusMessage = e.message ?: "Failed to update profile"
                )
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = _state.value as? ProfileUiState.Data ?: return@launch

            try {
                authRepository.updateNotificationSettings(enabled)
                _state.value = current.copy(
                    statusMessage = "Notifications ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                _state.value = current.copy(
                    statusMessage = e.message ?: "Failed to update notifications"
                )
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            val current = _state.value as? ProfileUiState.Data ?: return@launch

            try {
                authRepository.changePassword(oldPassword, newPassword)
                _state.value = current.copy(
                    statusMessage = "Password changed successfully!"
                )
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("password") == true -> "Incorrect current password"
                    e.message?.contains("weak") == true -> "New password is too weak"
                    else -> e.message ?: "Failed to change password"
                }
                Log.d("ProfileViewModel", "changePassword: $message")
                _state.value = current.copy(
                    statusMessage = message
                )
            }
        }
    }

    fun linkPlaidSandbox() {
        viewModelScope.launch {
            val current = _state.value as? ProfileUiState.Data ?: return@launch
            _state.value = current.copy(
                isLinking = true,
                statusMessage = "Requesting Plaid link token…",
                linkToken = null
            )
            plaidRepository.createLinkToken()
                .onSuccess { token ->
                    _state.value = current.copy(
                        linkToken = token,
                        isLinking = false,
                        statusMessage = "Use this token with Plaid Link to finish sandbox linking."
                    )
                }
                .onFailure { throwable ->
                    _state.value = current.copy(
                        isLinking = false,
                        statusMessage = throwable.message ?: "Unable to request Plaid link token."
                    )
                }
        }
    }

    fun exchangePublicToken(publicToken: String) {
        viewModelScope.launch {
            val current = _state.value as? ProfileUiState.Data ?: return@launch
            _state.value = current.copy(
                isLinking = true,
                statusMessage = "Exchanging sandbox public token…"
            )
            plaidRepository.exchangePublicToken(publicToken)
                .onSuccess {
                    financeRepository.linkSandbox()
                        .onSuccess {
                            _state.value = current.copy(
                                linkToken = null,
                                isLinking = false,
                                statusMessage = "Sandbox account linked. Pull to refresh to view balances."
                            )
                        }
                        .onFailure { error ->
                            _state.value = current.copy(
                                linkToken = null,
                                isLinking = false,
                                statusMessage = error.message ?: "Account refresh failed."
                            )
                        }
                }
                .onFailure { throwable ->
                    _state.value = current.copy(
                        isLinking = false,
                        statusMessage = throwable.message ?: "Exchange failed. Double-check the public token."
                    )
                }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}