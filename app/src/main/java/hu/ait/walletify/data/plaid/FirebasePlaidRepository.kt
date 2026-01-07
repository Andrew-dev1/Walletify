package hu.ait.walletify.data.plaid

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of PlaidRepository.
 * Communicates with Firebase Cloud Functions to interact with Plaid API.
 */
@Singleton
class FirebasePlaidRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth
) : PlaidRepository {


    /**
     * Creates a Plaid Link token for the current authenticated user.
     * Calls the createLinkToken Cloud Function which generates a token
     * that can be used to initialize Plaid Link.
     */
    override suspend fun createLinkToken(): Result<String> = runCatching {
        val result = functions
            .getHttpsCallable("createLinkToken")
            .call()
            .await()
        Log.d("FirebasePlaidRepository", "createLinkToken response: $result")

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        data["link_token"] as? String
            ?: throw IllegalStateException("Link token not found in response")
    }
    /**
     * Exchanges a Plaid public token for an access token.
     * Calls the exchangePublicToken Cloud Function which exchanges the token
     * and stores the access token in Firestore.
     */
    override suspend fun exchangePublicToken(publicToken: String): Result<String> = runCatching {
        val params = hashMapOf("publicToken" to publicToken)
        val result = functions
            .getHttpsCallable("exchangePublicToken")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        data["item_id"] as? String
            ?: throw IllegalStateException("Item ID not found in response")
    }

    /**
     * Syncs transactions from Plaid for a given item and date range.
     * Calls the syncTransactions Cloud Function which retrieves transactions from Plaid
     * and stores them in Firestore.
     */
    override suspend fun syncTransactions(itemId: String, startDate: String, endDate: String): Result<TransactionSyncResult> = runCatching {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User must be authenticated")
        
        val params = hashMapOf(
            "itemId" to itemId,
            "startDate" to startDate,
            "endDate" to endDate
        )
        
        val result = functions
            .getHttpsCallable("syncTransactions")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        val synced = (data["synced"] as? Number)?.toInt() ?: 0
        val updated = (data["updated"] as? Number)?.toInt() ?: 0
        val removed = (data["removed"] as? Number)?.toInt() ?: 0

        TransactionSyncResult(
            synced = synced,
            updated = updated,
            removed = removed
        )
    }

    /**
     * Creates an update link token for re-authenticating an existing Plaid item.
     * Calls the createUpdateLinkToken Cloud Function.
     */
    override suspend fun createUpdateLinkToken(itemId: String): Result<String> = runCatching {
        val params = hashMapOf("itemId" to itemId)
        val result = functions
            .getHttpsCallable("createUpdateLinkToken")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        data["link_token"] as? String
            ?: throw IllegalStateException("Link token not found in response")
    }

    /**
     * Updates access token after re-authentication.
     * Calls the updateAccessToken Cloud Function.
     */
    override suspend fun updateAccessToken(itemId: String, publicToken: String): Result<Boolean> = runCatching {
        val params = hashMapOf(
            "itemId" to itemId,
            "publicToken" to publicToken
        )
        val result = functions
            .getHttpsCallable("updateAccessToken")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        (data["success"] as? Boolean) ?: false
    }

    /**
     * Gets the status of a Plaid item to check if re-authentication is needed.
     * Calls the getItemStatus Cloud Function.
     */
    override suspend fun getItemStatus(itemId: String): Result<ItemStatusResult> = runCatching {
        val params = hashMapOf("itemId" to itemId)
        val result = functions
            .getHttpsCallable("getItemStatus")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        val status = data["status"] as? String ?: "unknown"
        val requiresReauth = (data["requiresReauth"] as? Boolean) ?: false
        
        val errorMap = data["error"] as? Map<*, *>
        val error = errorMap?.let {
            ItemError(
                errorCode = it["error_code"] as? String ?: "",
                errorMessage = it["error_message"] as? String
            )
        }

        ItemStatusResult(
            status = status,
            requiresReauth = requiresReauth,
            error = error
        )
    }
}