package hu.ait.walletify.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.ait.walletify.data.model.SavingsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed interface SavingsUiState {
    data object Loading : SavingsUiState
    data class Data(val snapshot: SavingsSnapshot) : SavingsUiState
}

class SavingsViewModel: ViewModel() {
    private val _state = MutableStateFlow(SavingsUiState.Loading)
    val state: StateFlow<SavingsUiState> = _state

    init {
        viewModelScope.launch {

        }
    }

}