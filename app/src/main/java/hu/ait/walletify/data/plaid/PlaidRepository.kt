package hu.ait.walletify.data.plaid

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a transaction sync operation
 */
data class TransactionSyncResult(
    val synced: Int,
    val updated: Int,
    val removed: Int = 0
)

/**
 * Repository interface for Plaid API operations.
 * Handles link token creation, public token exchange, and transaction syncing.
 */
interface PlaidRepository {
    /**
     * Creates a Plaid Link token for the current authenticated user.
     * @return Result containing the link token string, or failure if error occurs
     */
    suspend fun createLinkToken(): Result<String>
    
    /**
     * Exchanges a Plaid public token for an access token and stores it.
     * @param publicToken The public token received from Plaid Link
     * @return Result containing the item_id, or failure if error occurs
     */
    suspend fun exchangePublicToken(publicToken: String): Result<String>
    
    /**
     * Syncs transactions from Plaid for a given item and date range.
     * @param itemId The Plaid item ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Result containing sync statistics, or failure if error occurs
     */
    suspend fun syncTransactions(itemId: String, startDate: String, endDate: String): Result<TransactionSyncResult>
}


