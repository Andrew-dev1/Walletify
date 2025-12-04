package hu.ait.walletify.ui.plaid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.plaid.PlaidRepository
import kotlinx.coroutines.launch
import javax.inject.Inject



sealed class PlaidUiState {
    object Idle : PlaidUiState()
    object Loading : PlaidUiState()
    data class LinkTokenReceived(val linkToken: String) : PlaidUiState()
    data class Connected(val itemId: String) : PlaidUiState()
    data class Error(val message: String) : PlaidUiState()
}

@HiltViewModel
class PlaidViewModel @Inject constructor(
    private val plaidRepository: PlaidRepository
) : ViewModel() {

    var plaidUiState by mutableStateOf<PlaidUiState>(PlaidUiState.Idle)
        private set

    fun createLinkToken() {
        viewModelScope.launch {
            plaidUiState = PlaidUiState.Loading

            plaidRepository.createLinkToken()
                .onSuccess { linkToken ->
                    plaidUiState = PlaidUiState.LinkTokenReceived(linkToken)
                }
                .onFailure { error ->
                    plaidUiState = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to create link token"
                    )
                }
        }
    }

    fun exchangePublicToken(publicToken: String) {
        viewModelScope.launch {
            plaidUiState = PlaidUiState.Loading

            plaidRepository.exchangePublicToken(publicToken)
                .onSuccess { itemId ->
                    plaidUiState = PlaidUiState.Connected(itemId)
                }
                .onFailure { error ->
                    plaidUiState = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to connect account"
                    )
                }
        }
    }

    fun resetState() {
        plaidUiState = PlaidUiState.Idle
    }
}

