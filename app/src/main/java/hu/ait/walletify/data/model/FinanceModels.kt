package hu.ait.walletify.data.model

import kotlinx.serialization.Serializable

/**
 * Core domain models surfaced through repositories.
 * Keep models in the data layer so they can be easily serialized
 * and shared between navigation destinations if required.
 */

@Serializable
data class PlaidAccount(
    val id: String,
    val name: String,
    val mask: String,
    val institution: String,
    val type: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val currency: String = "USD"
)

data class TransactionItem(
    val id: String,
    val merchant: String,
    val category: String,
    val amount: Double,
    val date: String,
    val isDebit: Boolean,
    val accountId: String
)

data class MonthlyTransactions(
    val monthLabel: String,
    val transactions: List<TransactionItem>
)

data class SpendingInsight(
    val monthLabel: String,
    val spent: Double,
    val budget: Double
)

data class SavingsGoal(
    val id: String,
    val title: String,
    val target: Double,
    val contributed: Double,
    val dueBy: String,
    val status: GoalStatus
)

enum class GoalStatus {
    ON_TRACK,
    AT_RISK,
    MISSED
}

data class BudgetAlert(
    val category: String,
    val limit: Double,
    val spent: Double,
    val severity: AlertSeverity
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH
}

data class DashboardSnapshot(
    val greetingName: String,
    val netCashFlow: Double,
    val monthToDateSpend: Double,
    val onboardingChecklist: List<OnboardingTask>,
    val insights: List<SpendingInsight>,
    val recentTransactions: List<TransactionItem>
)

data class OnboardingTask(
    val id: String,
    val title: String,
    val description: String,
    val completed: Boolean
)

data class SavingsSnapshot(
    val goals: List<SavingsGoal>,
    val alerts: List<BudgetAlert>
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val householdMembers: Int,
    val pushNotificationsEnabled: Boolean
)


