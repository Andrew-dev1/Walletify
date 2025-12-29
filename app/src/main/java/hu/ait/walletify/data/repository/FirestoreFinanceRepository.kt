package hu.ait.walletify.data.repository

import hu.ait.walletify.data.model.Budget
import hu.ait.walletify.data.model.BudgetAlert
import hu.ait.walletify.data.model.DashboardSnapshot
import hu.ait.walletify.data.model.MonthlyTransactions
import hu.ait.walletify.data.model.OnboardingTask
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.SavingsSnapshot
import hu.ait.walletify.data.model.SpendingInsight
import hu.ait.walletify.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-based implementation of FinanceRepository.
 * Uses FirestoreTransactionRepository, FirestoreBudgetRepository, and FirestorePlaidAccountRepository for data persistence.
 */
@Singleton
class FirestoreFinanceRepository @Inject constructor(
    private val transactionRepository: FirestoreTransactionRepository,
    private val budgetRepository: FirestoreBudgetRepository,
    private val plaidAccountRepository: FirestorePlaidAccountRepository
) : FinanceRepository {

    override fun observeDashboard(): Flow<DashboardSnapshot> {
        // Combine transactions, accounts, and budgets to build dashboard
        return combine(
            transactionRepository.observeTransactions(),
            plaidAccountRepository.observeAccounts(),
            budgetRepository.observeBudgets()
        ) { transactions, accounts, budgets ->
            val allTransactions = transactions.flatMap { it.transactions }
            val recentTransactions = allTransactions.take(4)
            
            // Calculate net cash flow (income - expenses) for current month
            val calendar = java.util.Calendar.getInstance()
            val currentMonth = calendar.get(java.util.Calendar.MONTH)
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            
            val currentMonthTransactions = allTransactions.filter { transaction ->
                val transactionCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = transaction.date
                }
                transactionCalendar.get(java.util.Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(java.util.Calendar.YEAR) == currentYear
            }
            
            val income = currentMonthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val expenses = currentMonthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
            val netCashFlow = income - expenses
            val monthToDateSpend = expenses
            
            // Calculate spending insights for last 3 months
            val insights = calculateSpendingInsights(transactions, budgets)
            
            // Check onboarding status
            val hasLinkedAccounts = accounts.isNotEmpty()
            val hasBudgets = budgets.isNotEmpty()
            
            DashboardSnapshot(
                greetingName = "User", // TODO: Get from user profile
                netCashFlow = netCashFlow,
                monthToDateSpend = monthToDateSpend,
                onboardingChecklist = listOf(
                    OnboardingTask("connect_plaid", "Connect Plaid", "Securely aggregate your accounts.", completed = hasLinkedAccounts),
                    OnboardingTask("set_budget", "Set Budgets", "Create limits across categories.", completed = hasBudgets),
                    OnboardingTask("invite_partner", "Invite a partner", "Share read-only access.", completed = false)
                ),
                insights = insights,
                recentTransactions = recentTransactions
            )
        }
    }

    override fun observeTransactions(): Flow<List<MonthlyTransactions>> {
        return transactionRepository.observeTransactions()
    }

    override fun observeSavings(): Flow<SavingsSnapshot> {
        // Combine budgets with savings goals
        return combine(
            budgetRepository.observeBudgets(),
            MutableStateFlow(emptyList<hu.ait.walletify.data.model.SavingsGoal>()).asStateFlow()
        ) { budgets, goals ->
            // Convert budgets to budget alerts
            val alerts = budgets.mapNotNull { budget ->
                if (budget.shouldAlert) {
                    val severity = when {
                        budget.usagePercentage >= 1.0 -> hu.ait.walletify.data.model.AlertSeverity.HIGH
                        budget.usagePercentage >= budget.alertThreshold -> hu.ait.walletify.data.model.AlertSeverity.MEDIUM
                        else -> hu.ait.walletify.data.model.AlertSeverity.LOW
                    }
                    BudgetAlert(
                        category = budget.category,
                        limit = budget.limit,
                        spent = budget.spent,
                        severity = severity
                    )
                } else null
            }
            
            SavingsSnapshot(
                goals = goals,
                alerts = alerts
            )
        }
    }

    override fun observePlaidAccounts(): Flow<List<PlaidAccount>> {
        return plaidAccountRepository.observeAccounts()
    }

    override suspend fun linkSandbox(): Result<String> {
        // This is handled by PlaidViewModel and PlaidRepository
        // The actual linking happens through Plaid Link SDK
        return Result.success("sandbox-token")
    }

    override suspend fun addTransaction(transaction: TransactionItem) {
        transactionRepository.addTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: TransactionItem) {
        transactionRepository.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        transactionRepository.deleteTransaction(transactionId)
    }

    override suspend fun initializeDefaultTransactions() {
        // No-op for Firestore - transactions come from Plaid sync
    }

    // Budget operations - delegate to budget repository
    override fun observeBudgets(): Flow<List<Budget>> {
        return budgetRepository.observeBudgets()
    }

    override suspend fun createBudget(budget: Budget): Budget {
        return budgetRepository.createBudget(budget)
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetRepository.updateBudget(budget)
    }

    override suspend fun deleteBudget(budgetId: String) {
        budgetRepository.deleteBudget(budgetId)
    }

    override suspend fun getBudget(budgetId: String): Budget? {
        return budgetRepository.getBudget(budgetId)
    }

    /**
     * Calculates spending insights for the last 3 months.
     */
    private fun calculateSpendingInsights(
        transactions: List<MonthlyTransactions>,
        budgets: List<Budget>
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        val now = java.util.Calendar.getInstance()
        
        // Get last 3 months (including current month)
        for (i in 0..2) {
            val calendar = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MONTH, -i)
            }
            val month = calendar.get(java.util.Calendar.MONTH)
            val year = calendar.get(java.util.Calendar.YEAR)
            
            val monthLabel = java.text.SimpleDateFormat("MMM", java.util.Locale.US)
                .format(calendar.time)
            
            // Find transactions for this month
            val monthTransactions = transactions.flatMap { monthly ->
                monthly.transactions.filter { transaction ->
                    val transactionCalendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = transaction.date
                    }
                    transactionCalendar.get(java.util.Calendar.MONTH) == month &&
                    transactionCalendar.get(java.util.Calendar.YEAR) == year
                }
            }
            
            val spent = monthTransactions
                .filter { it.amount < 0 }
                .sumOf { -it.amount }
            
            // Calculate budget for this month (sum of all monthly budgets)
            val budget = budgets
                .filter { it.period == hu.ait.walletify.data.model.BudgetPeriod.MONTHLY }
                .sumOf { it.limit }
            
            insights.add(SpendingInsight(monthLabel, spent, budget))
        }
        
        return insights.reversed() // Return in chronological order (oldest first)
    }
}

