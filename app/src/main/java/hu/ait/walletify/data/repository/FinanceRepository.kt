package hu.ait.walletify.data.repository

import hu.ait.walletify.data.model.AlertSeverity
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
 * transaction feeds, and savings telemetry before the real backend arrives.
 */
interface FinanceRepository {
    fun observeDashboard(): Flow<DashboardSnapshot>
    fun observeTransactions(): Flow<List<MonthlyTransactions>>
    fun observeSavings(): Flow<SavingsSnapshot>
    fun observePlaidAccounts(): Flow<List<PlaidAccount>>
    suspend fun linkSandbox(): Result<String>
}

@Singleton
class FakeFinanceRepository @Inject constructor() : FinanceRepository {

    private val dashboard = MutableStateFlow(buildDashboard())
    private val transactions = MutableStateFlow(buildTransactions())
    private val savings = MutableStateFlow(buildSavings())
    private val plaidAccounts = MutableStateFlow(buildPlaidAccounts())

    override fun observeDashboard(): Flow<DashboardSnapshot> = dashboard.asStateFlow()
    override fun observeTransactions(): Flow<List<MonthlyTransactions>> = transactions.asStateFlow()
    override fun observeSavings(): Flow<SavingsSnapshot> = savings.asStateFlow()
    override fun observePlaidAccounts(): Flow<List<PlaidAccount>> = plaidAccounts.asStateFlow()

    override suspend fun linkSandbox(): Result<String> {
        delay(500)
        val token = "public-sandbox-${System.currentTimeMillis()}"
        plaidAccounts.update { list ->
            if (list.size < 3) list + buildPlaidAccount(list.size + 1) else list
        }
        return Result.success(token)
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

    private fun buildTransactions(): List<MonthlyTransactions> = listOf(
        MonthlyTransactions(
            monthLabel = "November 2025",
            transactions = listOf(
                TransactionItem(
                    id = "tx1",
                    merchant = "Whole Foods",
                    category = "Groceries",
                    amount = 82.45,
                    date = "Nov 18",
                    isDebit = true,
                    accountId = "acc1"
                ),
                TransactionItem(
                    id = "tx2",
                    merchant = "Amtrak",
                    category = "Travel",
                    amount = 156.80,
                    date = "Nov 18",
                    isDebit = true,
                    accountId = "acc1"
                ),
                TransactionItem(
                    id = "tx3",
                    merchant = "Spotify",
                    category = "Subscriptions",
                    amount = 10.99,
                    date = "Nov 18",
                    isDebit = true,
                    accountId = "acc2"
                ),
                TransactionItem(
                    id = "tx4",
                    merchant = "Employer Inc.",
                    category = "Income",
                    amount = 4200.0,
                    date = "Nov 15",
                    isDebit = false,
                    accountId = "acc3"
                )
            )
        ),
        MonthlyTransactions(
            monthLabel = "October 2025",
            transactions = listOf(
                TransactionItem(
                    id = "tx5",
                    merchant = "Target",
                    category = "Home",
                    amount = 210.74,
                    date = "Oct 21",
                    isDebit = true,
                    accountId = "acc1"
                ),
                TransactionItem(
                    id = "tx6",
                    merchant = "Lyft",
                    category = "Transport",
                    amount = 23.50,
                    date = "Oct 19",
                    isDebit = true,
                    accountId = "acc1"
                ),
                TransactionItem(
                    id = "tx7",
                    merchant = "Netflix",
                    category = "Subscriptions",
                    amount = 19.99,
                    date = "Oct 14",
                    isDebit = true,
                    accountId = "acc2"
                )
            )
        )
    )

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
        id = "plaid-$index",
        name = "Checking $index",
        mask = "10$index",
        institution = "Walletify Demo Bank",
        type = if (index % 2 == 0) "Savings" else "Checking",
        currentBalance = 2400.0 - index * 120,
        availableBalance = 2000.0 - index * 100,
        currency = "USD"
    )
}

