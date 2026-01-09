package hu.ait.walletify.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.ait.walletify.data.model.AppNotification
import hu.ait.walletify.data.model.Budget
import hu.ait.walletify.data.model.BudgetPeriod
import hu.ait.walletify.data.model.NotificationType
import hu.ait.walletify.data.model.SavingsSnapshot
import hu.ait.walletify.data.model.TransactionItem
import hu.ait.walletify.data.repository.FinanceRepository
import hu.ait.walletify.data.repository.FirestoreNotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject


sealed interface SavingsUiState {
    data object Loading : SavingsUiState
    data class Data(
        val snapshot: SavingsSnapshot,
        val budgets: List<Budget> = emptyList(),
        val availableCategories: List<String> = emptyList()
    ) : SavingsUiState
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val notificationRepository: FirestoreNotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SavingsUiState>(SavingsUiState.Loading)
    val state: StateFlow<SavingsUiState> = _state.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _editingBudget = MutableStateFlow<Budget?>(null)
    val editingBudget: StateFlow<Budget?> = _editingBudget.asStateFlow()

    // Track which budgets we've already notified for in this session
    private val notifiedBudgetIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(
                financeRepository.observeSavings(),
                financeRepository.observeBudgets(),
                financeRepository.observeTransactions()
            ) { snapshot, budgets, monthlyTransactions ->
                val allTransactions = monthlyTransactions.flatMap { it.transactions }

                val categories = allTransactions
                    .map { it.primaryCategory }
                    .distinct()
                    .filter { it != "Other" && it != "Income" }
                    .sorted()

                // Calculate spent amount for each budget based on transactions
                val budgetsWithSpent = budgets.map { budget ->
                    val spent = calculateSpentForBudget(budget, allTransactions)
                    budget.copy(spent = spent)
                }

                Triple(snapshot, budgetsWithSpent, categories)
            }.collect { (snapshot, budgets, categories) ->
                // Check for budget alerts and create notifications
                checkBudgetAlerts(budgets)

                _state.value = SavingsUiState.Data(
                    snapshot = snapshot,
                    budgets = budgets,
                    availableCategories = categories
                )
            }
        }
    }

    private fun checkBudgetAlerts(budgets: List<Budget>) {
        viewModelScope.launch {
            budgets.forEach { budget ->
                if (budget.shouldAlert && !notifiedBudgetIds.contains(budget.budgetId)) {
                    val periodStart = getPeriodStartTime(budget.period, System.currentTimeMillis())

                    // Check if we already sent a notification for this budget in this period
                    val hasRecentAlert = notificationRepository.hasRecentBudgetAlert(
                        budget.budgetId,
                        periodStart
                    )

                    if (!hasRecentAlert) {
                        val percentage = (budget.usagePercentage * 100).toInt()
                        val isOver = budget.isExceeded

                        val notification = AppNotification(
                            notificationId = "",
                            title = if (isOver) "Budget Exceeded!" else "Budget Alert",
                            message = if (isOver) {
                                "You've exceeded your ${budget.period.value} ${budget.category} budget of $${String.format("%.2f", budget.limit)}"
                            } else {
                                "You've spent $percentage% of your ${budget.period.value} ${budget.category} budget ($${String.format("%.2f", budget.spent)} of $${String.format("%.2f", budget.limit)})"
                            },
                            type = if (isOver) NotificationType.WARNING else NotificationType.INFO,
                            relatedBudgetId = budget.budgetId
                        )

                        notificationRepository.createNotification(notification)
                        notifiedBudgetIds.add(budget.budgetId)
                    }
                }
            }
        }
    }

    private fun calculateSpentForBudget(budget: Budget, transactions: List<TransactionItem>): Double {
        val now = System.currentTimeMillis()
        val periodStart = getPeriodStartTime(budget.period, now)

        return transactions
            .filter { transaction ->
                // Match category (case-insensitive)
                val categoryMatches = transaction.primaryCategory.equals(budget.category, ignoreCase = true) ||
                    transaction.category.any { it.equals(budget.category, ignoreCase = true) }
                // Check if transaction is within the budget period
                val withinPeriod = transaction.date >= periodStart && transaction.date <= now
                // Only count debits (negative amounts = spending)
                val isDebit = transaction.amount < 0

                categoryMatches && withinPeriod && isDebit
            }
            .sumOf { kotlin.math.abs(it.amount) }
    }

    private fun getPeriodStartTime(period: BudgetPeriod, currentTime: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (period) {
            BudgetPeriod.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            }
            BudgetPeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            BudgetPeriod.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return calendar.timeInMillis
    }

    fun showCreateBudgetDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateBudgetDialog() {
        _showCreateDialog.value = false
    }

    fun createBudget(category: String, limit: Double, period: BudgetPeriod) {
        viewModelScope.launch {
            val budget = Budget(
                budgetId = "",
                category = category,
                limit = limit,
                period = period
            )
            financeRepository.createBudget(budget)
            _showCreateDialog.value = false
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            financeRepository.deleteBudget(budgetId)
        }
    }

    fun startEditingBudget(budget: Budget) {
        _editingBudget.value = budget
    }

    fun cancelEditingBudget() {
        _editingBudget.value = null
    }

    fun updateBudget(budgetId: String, category: String, limit: Double, period: BudgetPeriod) {
        viewModelScope.launch {
            val updatedBudget = Budget(
                budgetId = budgetId,
                category = category,
                limit = limit,
                period = period
            )
            financeRepository.updateBudget(updatedBudget)
            _editingBudget.value = null
        }
    }
}
