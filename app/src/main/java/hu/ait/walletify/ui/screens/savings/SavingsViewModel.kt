package hu.ait.walletify.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.SavingsSnapshot
import hu.ait.walletify.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface SavingsUiState {
    data object Loading : SavingsUiState
    data class Data(val snapshot: SavingsSnapshot) : SavingsUiState
}
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SavingsUiState>(SavingsUiState.Loading)
    val state: StateFlow<SavingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            financeRepository.observeSavings().collect { snapshot ->
                _state.value = SavingsUiState.Data(snapshot)
            }
        }
    }
}
