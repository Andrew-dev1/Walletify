package hu.ait.walletify.data.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Core domain models surfaced through repositories.
 * Keep models in the data layer so they can be easily serialized
 * and shared between navigation destinations if required.
 */

// ============================================================================
// Firestore Document Models (match Firestore structure exactly)
// ============================================================================

/**
 * Firestore document structure for user data in users/{uid}
 */
data class UserDocument(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val householdMembers: Int = 1,
    val pushNotificationsEnabled: Boolean = true,
    val source: String = "",
    val purpose: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Firestore document structure for Plaid accounts in users/{uid}/plaid_accounts/{accountId}
 */
data class PlaidAccountDocument(
    val accountId: String,
    val accountName: String,
    val institutionName: String,
    val type: String,
    val balance: Double = 0.0,
    val linkedAt: Long = System.currentTimeMillis()
)

/**
 * Firestore document structure for transactions in users/{uid}/transactions/{transactionId}
 */
data class TransactionDocument(
    val transactionId: String,
    val accountId: String,
    val amount: Double, // Negative for debits (spending), positive for credits (income)
    val date: Long, // Timestamp in milliseconds
    val name: String,
    val category: List<String>, // Plaid category hierarchy
    val pending: Boolean = false,
    val merchantName: String? = null,
    val syncedAt: Long = System.currentTimeMillis()
)

/**
 * Firestore document structure for budgets in users/{uid}/budgets/{budgetId}
 */
data class BudgetDocument(
    val budgetId: String,
    val category: String,
    val limit: Double,
    val spent: Double = 0.0,
    val period: String = "monthly", // "monthly", "weekly", "yearly"
    val alertThreshold: Double = 0.8, // Alert when 80% of limit is reached
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================================================
// Domain Models (used throughout the app)
// ============================================================================

@Serializable
data class PlaidAccount(
    val accountId: String,
    val accountName: String,
    val institutionName: String,
    val type: String,
    val balance: Double,
    val linkedAt: Long
) {
    companion object {
        fun fromDocument(doc: PlaidAccountDocument): PlaidAccount {
            return PlaidAccount(
                accountId = doc.accountId,
                accountName = doc.accountName,
                institutionName = doc.institutionName,
                type = doc.type,
                balance = doc.balance,
                linkedAt = doc.linkedAt
            )
        }

        fun toDocument(account: PlaidAccount): PlaidAccountDocument {
            return PlaidAccountDocument(
                accountId = account.accountId,
                accountName = account.accountName,
                institutionName = account.institutionName,
                type = account.type,
                balance = account.balance,
                linkedAt = account.linkedAt
            )
        }
    }

    // Legacy compatibility properties
    val currency: String = "USD"
}

data class TransactionItem(
    val transactionId: String,
    val accountId: String,
    val amount: Double, // Negative for debits, positive for credits
    val date: Long, // Timestamp in milliseconds
    val name: String,
    val category: List<String>, // Plaid category hierarchy
    val pending: Boolean = false,
    val merchantName: String? = null,
    val syncedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDocument(doc: TransactionDocument): TransactionItem {
            return TransactionItem(
                transactionId = doc.transactionId,
                accountId = doc.accountId,
                amount = doc.amount,
                date = doc.date,
                name = doc.name,
                category = doc.category,
                pending = doc.pending,
                merchantName = doc.merchantName,
                syncedAt = doc.syncedAt
            )
        }

        fun toDocument(transaction: TransactionItem): TransactionDocument {
            return TransactionDocument(
                transactionId = transaction.transactionId,
                accountId = transaction.accountId,
                amount = transaction.amount,
                date = transaction.date,
                name = transaction.name,
                category = transaction.category,
                pending = transaction.pending,
                merchantName = transaction.merchantName,
                syncedAt = transaction.syncedAt
            )
        }
    }

    // Helper properties
    val isDebit: Boolean get() = amount < 0
    val formattedDate: String
        get() {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date
            val format = SimpleDateFormat("MMM d", Locale.US)
            return format.format(calendar.time)
        }
    val primaryCategory: String get() = category.firstOrNull() ?: "Other"
    
    // Legacy compatibility properties
}

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
    val uid: String,
    val displayName: String,
    val email: String,
    val householdMembers: Int,
    val pushNotificationsEnabled: Boolean,
    val purpose: String,
    val source: String
) {
    companion object {
        fun fromDocument(doc: UserDocument, email: String): UserProfile {
            return UserProfile(
                uid = doc.uid,
                displayName = doc.displayName ?: "User",
                email = email,
                householdMembers = doc.householdMembers,
                pushNotificationsEnabled = doc.pushNotificationsEnabled,
                purpose = doc.purpose,
                source = doc.source
            )
        }

        fun toDocument(profile: UserProfile): UserDocument {
            return UserDocument(
                uid = profile.uid,
                displayName = profile.displayName,
                email = profile.email,
                householdMembers = profile.householdMembers,
                pushNotificationsEnabled = profile.pushNotificationsEnabled,
                source = profile.source,
                purpose = profile.purpose,
                createdAt = System.currentTimeMillis()
            )
        }
    }

}

/**
 * Budget domain model
 */
data class Budget(
    val budgetId: String,
    val category: String,
    val limit: Double,
    val spent: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val alertThreshold: Double = 0.8,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDocument(doc: BudgetDocument): Budget {
            return Budget(
                budgetId = doc.budgetId,
                category = doc.category,
                limit = doc.limit,
                spent = doc.spent,
                period = BudgetPeriod.fromString(doc.period),
                alertThreshold = doc.alertThreshold,
                createdAt = doc.createdAt
            )
        }

        fun toDocument(budget: Budget): BudgetDocument {
            return BudgetDocument(
                budgetId = budget.budgetId,
                category = budget.category,
                limit = budget.limit,
                spent = budget.spent,
                period = budget.period.value,
                alertThreshold = budget.alertThreshold,
                createdAt = budget.createdAt
            )
        }
    }

    // Helper properties
    val usagePercentage: Double get() = if (limit > 0) spent / limit else 0.0
    val isExceeded: Boolean get() = spent > limit
    val shouldAlert: Boolean get() = usagePercentage >= alertThreshold
    val remaining: Double get() = limit - spent
}

enum class BudgetPeriod(val value: String) {
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    companion object {
        fun fromString(value: String): BudgetPeriod {
            return entries.find { it.value == value } ?: MONTHLY
        }
    }
}
