package hu.ait.walletify.ui.screens.profile

import androidx.lifecycle.ViewModel
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class ProfileViewModel: ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

}