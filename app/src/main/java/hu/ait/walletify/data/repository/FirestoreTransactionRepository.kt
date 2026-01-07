package hu.ait.walletify.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hu.ait.walletify.data.model.MonthlyTransactions
import hu.ait.walletify.data.model.TransactionDocument
import hu.ait.walletify.data.model.TransactionItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing transactions in Firestore.
 * Handles CRUD operations for transactions stored in users/{uid}/transactions/{transactionId}
 */
@Singleton
class FirestoreTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    /**
     * Observes all transactions for the current user, grouped by month.
     * Returns a Flow that emits updated lists when transactions change.
     * Returns empty list if user is not authenticated.
     */
    fun observeTransactions(): Flow<List<MonthlyTransactions>> {
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
                    .collection("transactions")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList()) // Emit empty on error instead of closing
                            return@addSnapshotListener
                        }
                        
                        val transactions = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                TransactionDocument(
                                    transactionId = doc.getString("transactionId") ?: doc.id,
                                    accountId = doc.getString("accountId") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    date = doc.getLong("date") ?: 0L,
                                    name = doc.getString("name") ?: "",
                                    category = (doc.get("category") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    pending = doc.getBoolean("pending") ?: false,
                                    merchantName = doc.getString("merchantName"),
                                    syncedAt = doc.getLong("syncedAt") ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()
                        
                        trySend(transactions)
                    }
            }
            
            awaitClose { registration?.remove() }
        }
        .map { transactions ->
            // Convert to TransactionItem and group by month
            val transactionItems = transactions.map { TransactionItem.fromDocument(it) }
            transactionItems
                .groupBy { getMonthLabel(it.date) }
                .map { (monthLabel, monthTransactions) ->
                    hu.ait.walletify.data.model.MonthlyTransactions(
                        monthLabel = monthLabel,
                        transactions = monthTransactions
                    )
                }
                .sortedByDescending { it.monthLabel }
        }
    }

    /**
     * Adds a new transaction to Firestore.
     */
    suspend fun addTransaction(transaction: TransactionItem) {
        Log.d("FirestoreTransactionRepository", "Adding transaction: $transaction")
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        val doc = TransactionItem.toDocument(transaction)
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("transactions")
            .document(transaction.transactionId)
            .set(
                mapOf(
                    "transactionId" to doc.transactionId,
                    "accountId" to doc.accountId,
                    "amount" to doc.amount,
                    "date" to doc.date,
                    "name" to doc.name,
                    "category" to doc.category,
                    "pending" to doc.pending,
                    "merchantName" to doc.merchantName,
                    "syncedAt" to doc.syncedAt
                )
            )
            .await()

    }

    /**
     * Updates an existing transaction in Firestore.
     */
    suspend fun updateTransaction(transaction: TransactionItem) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        val doc = TransactionItem.toDocument(transaction)
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("transactions")
            .document(transaction.transactionId)
            .update(
                mapOf(
                    "accountId" to doc.accountId,
                    "amount" to doc.amount,
                    "date" to doc.date,
                    "name" to doc.name,
                    "category" to doc.category,
                    "pending" to doc.pending,
                    "merchantName" to doc.merchantName,
                    "syncedAt" to doc.syncedAt
                )
            )
            .await()
    }

    /**
     * Deletes a transaction from Firestore.
     */
    suspend fun deleteTransaction(transactionId: String) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .await()
    }

    /**
     * Gets month label from timestamp (e.g., "November 2025")
     */
    private fun getMonthLabel(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        return outputFormat.format(calendar.time)
    }
}

