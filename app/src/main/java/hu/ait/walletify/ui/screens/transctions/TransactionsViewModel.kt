package hu.ait.walletify.ui.screens.transctions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.MonthlyTransactions
import hu.ait.walletify.data.model.TransactionItem
import hu.ait.walletify.data.repository.FinanceRepository
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



@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            financeRepository.observeTransactions().collect { months ->
                _state.value = TransactionsUiState.Data(months = months, selectedCategory = null)
            }
        }
    }
    fun addManualTransaction(
        merchant: String,
        category: String,
        amount: Double,
        isDebit: Boolean
    ) {
        viewModelScope.launch {
            val transaction = TransactionItem(
                transactionId = "manual_${java.util.UUID.randomUUID()}",
                accountId = "manual_account",
                amount = if (isDebit) -amount else amount, // Negative for debits
                date = System.currentTimeMillis(),
                name = merchant,
                category = listOf(category),
                pending = false,
                merchantName = merchant,
                syncedAt = System.currentTimeMillis()
            )

            financeRepository.addTransaction(transaction)
        }
    }

    fun onCategorySelected(category: String?) {
        val current = _state.value as? TransactionsUiState.Data ?: return
        _state.value = current.copy(selectedCategory = category)
    }
}