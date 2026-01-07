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
 * Result of an item status check
 */
data class ItemStatusResult(
    val status: String,
    val requiresReauth: Boolean,
    val error: ItemError? = null
)

/**
 * Plaid item error information
 */
data class ItemError(
    val errorCode: String,
    val errorMessage: String?
)

/**
 * Repository interface for Plaid API operations.
 * Handles link token creation, public token exchange, transaction syncing, and re-authentication.
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
    
    /**
     * Creates an update link token for re-authenticating an existing Plaid item.
     * Used when an item requires user action (e.g., credentials expired).
     * @param itemId The Plaid item ID that needs re-authentication
     * @return Result containing the link token string, or failure if error occurs
     */
    suspend fun createUpdateLinkToken(itemId: String): Result<String>
    
    /**
     * Updates access token after re-authentication.
     * Exchanges the public token from update link flow for a new access token.
     * @param itemId The Plaid item ID
     * @param publicToken The public token received from Plaid Link update flow
     * @return Result containing success status, or failure if error occurs
     */
    suspend fun updateAccessToken(itemId: String, publicToken: String): Result<Boolean>
    
    /**
     * Gets the status of a Plaid item to check if re-authentication is needed.
     * @param itemId The Plaid item ID
     * @return Result containing item status information, or failure if error occurs
     */
    suspend fun getItemStatus(itemId: String): Result<ItemStatusResult>
}


