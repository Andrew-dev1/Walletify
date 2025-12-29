package hu.ait.walletify.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.PlaidAccountDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Plaid accounts in Firestore.
 * Handles reading accounts stored in users/{uid}/plaid_accounts/{accountId}
 */
@Singleton
class FirestorePlaidAccountRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    /**
     * Observes all Plaid accounts for the current user.
     * Returns a Flow that emits updated lists when accounts change.
     * Returns empty list if user is not authenticated.
     */
    fun observeAccounts(): Flow<List<PlaidAccount>> {
        return callbackFlow {
            var registration: com.google.firebase.firestore.ListenerRegistration? = null
            
            // Initial emit for unauthenticated state
            val currentUserId = userId
            if (currentUserId == null) {
                trySend(emptyList())
            } else {
                // Set up listener for authenticated user
                registration = firestore
                    .collection("users")
                    .document(currentUserId)
                    .collection("plaid_accounts")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList()) // Emit empty on error instead of closing
                            return@addSnapshotListener
                        }
                        
                        val accounts = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                PlaidAccountDocument(
                                    accountId = doc.getString("accountId") ?: doc.id,
                                    accountName = doc.getString("accountName") ?: "",
                                    institutionName = doc.getString("institutionName") ?: "",
                                    type = doc.getString("type") ?: "depository",
                                    balance = doc.getDouble("balance") ?: 0.0,
                                    linkedAt = doc.getLong("linkedAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()
                        
                        trySend(accounts)
                    }
            }
            
            awaitClose { registration?.remove() }
        }
        .map { accounts ->
            accounts.map { PlaidAccount.fromDocument(it) }
        }
    }

    /**
     * Gets a single account by ID.
     */
    suspend fun getAccount(accountId: String): PlaidAccount? {
        val currentUserId = userId ?: return null
        val doc = firestore
            .collection("users")
            .document(currentUserId)
            .collection("plaid_accounts")
            .document(accountId)
            .get()
            .await()
        
        return if (doc.exists()) {
            try {
                PlaidAccount.fromDocument(
                    PlaidAccountDocument(
                        accountId = doc.getString("accountId") ?: accountId,
                        accountName = doc.getString("accountName") ?: "",
                        institutionName = doc.getString("institutionName") ?: "",
                        type = doc.getString("type") ?: "depository",
                        balance = doc.getDouble("balance") ?: 0.0,
                        linkedAt = doc.getLong("linkedAt") ?: System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}


