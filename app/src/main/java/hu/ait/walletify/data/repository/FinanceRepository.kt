package hu.ait.walletify.data.repository

import hu.ait.walletify.data.model.AlertSeverity
import hu.ait.walletify.data.model.Budget
import hu.ait.walletify.data.model.BudgetAlert
import hu.ait.walletify.data.model.DashboardSnapshot
import hu.ait.walletify.data.model.GoalStatus
import hu.ait.walletify.data.model.MonthlyTransactions
import hu.ait.walletify.data.model.OnboardingTask
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.SavingsGoal
import hu.ait.walletify.data.model.SavingsSnapshot
import hu.ait.walletify.data.model.SpendingInsight
import hu.ait.walletify.data.model.TransactionItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finance data access. Responsible for orchestrating Plaid accounts,
 * transaction feeds, budgets, and savings telemetry.
 */
interface FinanceRepository {
    // Dashboard and overview
    fun observeDashboard(): Flow<DashboardSnapshot>
    fun observeTransactions(): Flow<List<MonthlyTransactions>>
    fun observeSavings(): Flow<SavingsSnapshot>
    fun observePlaidAccounts(): Flow<List<PlaidAccount>>
    suspend fun linkSandbox(): Result<String>
    
    // Transaction operations
    suspend fun addTransaction(transaction: TransactionItem)
    suspend fun updateTransaction(transaction: TransactionItem)
    suspend fun deleteTransaction(transactionId: String)
    suspend fun initializeDefaultTransactions()
    
    // Budget operations
    fun observeBudgets(): Flow<List<hu.ait.walletify.data.model.Budget>>
    suspend fun createBudget(budget: hu.ait.walletify.data.model.Budget): hu.ait.walletify.data.model.Budget
    suspend fun updateBudget(budget: hu.ait.walletify.data.model.Budget)
    suspend fun deleteBudget(budgetId: String)
    suspend fun getBudget(budgetId: String): hu.ait.walletify.data.model.Budget?
}

@Singleton
class FakeFinanceRepository @Inject constructor() : FinanceRepository {

    private val dashboard = MutableStateFlow(buildDashboard())
    private val transactions = MutableStateFlow(buildTransactions())
    private val savings = MutableStateFlow(buildSavings())
    private val plaidAccounts = MutableStateFlow(buildPlaidAccounts())
    private val budgets = MutableStateFlow<List<hu.ait.walletify.data.model.Budget>>(emptyList())

    override fun observeDashboard(): Flow<DashboardSnapshot> = dashboard.asStateFlow()
    override fun observeTransactions(): Flow<List<MonthlyTransactions>> = transactions.asStateFlow()
    override fun observeSavings(): Flow<SavingsSnapshot> = savings.asStateFlow()
    override fun observePlaidAccounts(): Flow<List<PlaidAccount>> = plaidAccounts.asStateFlow()

    override suspend fun linkSandbox(): Result<String> {
        val token = "public-sandbox-${System.currentTimeMillis()}"
        plaidAccounts.update { list ->
            if (list.size < 3) list + buildPlaidAccount(list.size + 1) else list
        }
        return Result.success(token)
    }

    // Transaction operations
    override suspend fun addTransaction(transaction: TransactionItem) {
        // Stub implementation - in real app, this would update Firestore
    }

    override suspend fun updateTransaction(transaction: TransactionItem) {
        // Stub implementation
    }

    override suspend fun deleteTransaction(transactionId: String) {
        // Stub implementation
    }

    override suspend fun initializeDefaultTransactions() {
        // Stub implementation - transactions come from Plaid sync
    }

    // Budget operations
    override fun observeBudgets(): Flow<List<Budget>> = budgets.asStateFlow()

    override suspend fun createBudget(budget: Budget):Budget {
        budgets.update { it + budget }
        return budget
    }

    override suspend fun updateBudget(budget: Budget) {
        budgets.update { list ->
            list.map { if (it.budgetId == budget.budgetId) budget else it }
        }
    }

    override suspend fun deleteBudget(budgetId: String) {
        budgets.update { it.filter { it.budgetId != budgetId } }
    }

    override suspend fun getBudget(budgetId: String): Budget? {
        return budgets.value.find { it.budgetId == budgetId }
    }

    private fun buildDashboard() = DashboardSnapshot(
        greetingName = "Casey",
        netCashFlow = 1250.0,
        monthToDateSpend = 1845.32,
        onboardingChecklist = listOf(
            OnboardingTask("connect_plaid", "Connect Plaid", "Securely aggregate your accounts.", completed = false),
            OnboardingTask("set_budget", "Set Budgets", "Create limits across categories.", completed = true),
            OnboardingTask("invite_partner", "Invite a partner", "Share read-only access.", completed = false)
        ),
        insights = listOf(
            SpendingInsight("Sep", 2130.0, 2500.0),
            SpendingInsight("Oct", 1980.0, 2300.0),
            SpendingInsight("Nov", 1845.32, 2300.0)
        ),
        recentTransactions = buildTransactions().first().transactions.take(4)
    )

    private fun buildTransactions(): List<MonthlyTransactions> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2025, 10, 18) // November 18, 2025
        
        val nov18 = calendar.timeInMillis
        calendar.set(2025, 10, 15) // November 15, 2025
        val nov15 = calendar.timeInMillis
        calendar.set(2025, 9, 21) // October 21, 2025
        val oct21 = calendar.timeInMillis
        calendar.set(2025, 9, 19) // October 19, 2025
        val oct19 = calendar.timeInMillis
        calendar.set(2025, 9, 14) // October 14, 2025
        val oct14 = calendar.timeInMillis
        
        return listOf(
            MonthlyTransactions(
                monthLabel = "November 2025",
                transactions = listOf(
                    TransactionItem(
                        transactionId = "tx1",
                        accountId = "acc1",
                        amount = -82.45, // Negative for debit
                        date = nov18,
                        name = "Whole Foods",
                        category = listOf("Food and Drink", "Groceries"),
                        pending = false,
                        merchantName = "Whole Foods"
                    ),
                    TransactionItem(
                        transactionId = "tx2",
                        accountId = "acc1",
                        amount = -156.80,
                        date = nov18,
                        name = "Amtrak",
                        category = listOf("Travel"),
                        pending = false,
                        merchantName = "Amtrak"
                    ),
                    TransactionItem(
                        transactionId = "tx3",
                        accountId = "acc2",
                        amount = -10.99,
                        date = nov18,
                        name = "Spotify",
                        category = listOf("Entertainment", "Music"),
                        pending = false,
                        merchantName = "Spotify"
                    ),
                    TransactionItem(
                        transactionId = "tx4",
                        accountId = "acc3",
                        amount = 4200.0, // Positive for credit (income)
                        date = nov15,
                        name = "Employer Inc.",
                        category = listOf("Income"),
                        pending = false,
                        merchantName = null
                    )
                )
            ),
            MonthlyTransactions(
                monthLabel = "October 2025",
                transactions = listOf(
                    TransactionItem(
                        transactionId = "tx5",
                        accountId = "acc1",
                        amount = -210.74,
                        date = oct21,
                        name = "Target",
                        category = listOf("General Merchandise"),
                        pending = false,
                        merchantName = "Target"
                    ),
                    TransactionItem(
                        transactionId = "tx6",
                        accountId = "acc1",
                        amount = -23.50,
                        date = oct19,
                        name = "Lyft",
                        category = listOf("Ride Share"),
                        pending = false,
                        merchantName = "Lyft"
                    ),
                    TransactionItem(
                        transactionId = "tx7",
                        accountId = "acc2",
                        amount = -19.99,
                        date = oct14,
                        name = "Netflix",
                        category = listOf("Entertainment", "Streaming"),
                        pending = false,
                        merchantName = "Netflix"
                    )
                )
            )
        )
    }

    private fun buildSavings() = SavingsSnapshot(
        goals = listOf(
            SavingsGoal("goal1", "Emergency Fund", 10000.0, 6200.0, "Mar 2026", GoalStatus.ON_TRACK),
            SavingsGoal("goal2", "Sabbatical", 15000.0, 3200.0, "Nov 2026", GoalStatus.AT_RISK),
            SavingsGoal("goal3", "Home Refresh", 5000.0, 5000.0, "Jul 2025", GoalStatus.MISSED)
        ),
        alerts = listOf(
            BudgetAlert("Dining Out", 300.0, 265.0, severity = AlertSeverity.MEDIUM),
            BudgetAlert("Transportation", 250.0, 280.0, severity = AlertSeverity.HIGH)
        )
    )

    private fun buildPlaidAccounts(): List<PlaidAccount> = listOf(
        buildPlaidAccount(1),
        buildPlaidAccount(2)
    )

    private fun buildPlaidAccount(index: Int) = PlaidAccount(
        accountId = "plaid-$index",
        accountName = "Checking $index",
        institutionName = "Walletify Demo Bank",
        type = if (index % 2 == 0) "Savings" else "Checking",
        balance = 2400.0 - index * 120,
        linkedAt = System.currentTimeMillis() - index * 86400000,
//        currency = "USD"
    )
}

