package hu.ait.walletify.ui.screens.transctions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.ait.walletify.data.model.MonthlyTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Data(
        val months: List<MonthlyTransactions>,
        val selectedCategory: String?
    ) : TransactionsUiState
}



class TransactionsViewModel: ViewModel() {

    private val _state = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {

        }
    }

    fun onCategorySelected(category: String?) {
        val current = _state.value as? TransactionsUiState.Data ?: return
        _state.value = current.copy(selectedCategory = category)
    }
}