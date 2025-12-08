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


    fun createLinkToken() {
//        var result = "first"
        viewModelScope.launch {
            _plaidUiState.value = PlaidUiState.Loading
            Log.d("plaidRepository", "createLinkToken button clicked")

            plaidRepository.createLinkToken()
                .onSuccess { linkToken ->
                    _plaidUiState.value  = PlaidUiState.LinkTokenReceived(linkToken)
                }
                .onFailure { error ->
                    _plaidUiState.value  = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to create link token"
                    )
                }
//            result = plaidRepository.createLinkToken2()
//            if(result == ""){
//                _plaidUiState.value  = PlaidUiState.Error("Failed to create link token")
//            }else{
//                _plaidUiState.value  = PlaidUiState.LinkTokenReceived(result)
//            }
        }
//        return result
    }

    fun exchangePublicToken(publicToken: String) {
        viewModelScope.launch {
            _plaidUiState.value  = PlaidUiState.Loading

            plaidRepository.exchangePublicToken(publicToken)
                .onSuccess { itemId ->
                    _plaidUiState.value  = PlaidUiState.Connected(itemId)
                }
                .onFailure { error ->
                    _plaidUiState.value  = PlaidUiState.Error(
                        error.localizedMessage ?: "Failed to connect account"
                    )
                }
        }
    }

    fun resetState() {
        _plaidUiState.value  = PlaidUiState.Idle
    }
}

