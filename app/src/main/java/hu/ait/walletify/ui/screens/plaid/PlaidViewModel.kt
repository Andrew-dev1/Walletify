package hu.ait.walletify.ui.plaid


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.plaid.FirebasePlaidRepository
import hu.ait.walletify.data.plaid.PlaidRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val plaidRepository: FirebasePlaidRepository
) : ViewModel() {


    private val _plaidUiState = MutableStateFlow<PlaidUiState>(PlaidUiState.Idle)
    var plaidUiState: StateFlow<PlaidUiState> = _plaidUiState.asStateFlow()


    /**
     * Creates a Plaid link token by calling the Cloud Function.
     * Updates UI state to Loading, then LinkTokenReceived on success, or Error on failure.
     */
    fun createLinkToken() {
        viewModelScope.launch {
            _plaidUiState.value = PlaidUiState.Loading
            Log.d("PlaidViewModel", "Creating link token...")

            plaidRepository.createLinkToken()
                .onSuccess { linkToken ->
                    Log.d("PlaidViewModel", "Link token received successfully")
                    _plaidUiState.value = PlaidUiState.LinkTokenReceived(linkToken)
                }
                .onFailure { error ->
                    Log.e("PlaidViewModel", "Failed to create link token", error)
                    _plaidUiState.value = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to create link token"
                    )
                }
        }
    }

    /**
     * Exchanges the public token received from Plaid Link for an access token.
     * Updates UI state to Loading, then Connected on success, or Error on failure.
     * 
     * @param publicToken The public token received from Plaid Link success callback
     */
    fun exchangePublicToken(publicToken: String) {
        viewModelScope.launch {
            _plaidUiState.value = PlaidUiState.Loading
            Log.d("PlaidViewModel", "Exchanging public token...")

            plaidRepository.exchangePublicToken(publicToken)
                .onSuccess { itemId ->
                    Log.d("PlaidViewModel", "Public token exchanged successfully, itemId: $itemId")
                    _plaidUiState.value = PlaidUiState.Connected(itemId)
                }
                .onFailure { error ->
                    Log.e("PlaidViewModel", "Failed to exchange public token", error)
                    _plaidUiState.value = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to connect account"
                    )
                }
        }
    }

    /**
     * Resets the Plaid UI state to Idle.
     * Called when user cancels or exits Plaid Link flow.
     */
    fun resetState() {
        _plaidUiState.value = PlaidUiState.Idle
    }
}

