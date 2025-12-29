package hu.ait.walletify.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.walletify.data.model.Budget
import hu.ait.walletify.data.model.BudgetDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing budgets in Firestore.
 * Handles CRUD operations for budgets stored in users/{uid}/budgets/{budgetId}
 */
@Singleton
class FirestoreBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    /**
     * Observes all budgets for the current user.
     * Returns a Flow that emits updated lists when budgets change.
     * Returns empty list if user is not authenticated.
     */
    fun observeBudgets(): Flow<List<Budget>> {
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
                    .collection("budgets")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList()) // Emit empty on error instead of closing
                            return@addSnapshotListener
                        }
                        
                        val budgets = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                BudgetDocument(
                                    budgetId = doc.getString("budgetId") ?: doc.id,
                                    category = doc.getString("category") ?: "",
                                    limit = doc.getDouble("limit") ?: 0.0,
                                    spent = doc.getDouble("spent") ?: 0.0,
                                    period = doc.getString("period") ?: "monthly",
                                    alertThreshold = doc.getDouble("alertThreshold") ?: 0.8,
                                    createdAt = doc.getLong("createdAt") ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()
                        
                        trySend(budgets)
                    }
            }
            
            awaitClose { registration?.remove() }
        }
        .map { budgets ->
            budgets.map { Budget.fromDocument(it) }
        }
    }

    /**
     * Creates a new budget in Firestore.
     * 
     * @param budget The budget to create
     * @return The created budget with generated budgetId if not provided
     */
    suspend fun createBudget(budget: Budget): Budget {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        val budgetId = budget.budgetId.ifEmpty { 
            firestore.collection("users").document(currentUserId).collection("budgets").document().id 
        }
        
        val doc = Budget.toDocument(budget.copy(budgetId = budgetId))
        val createdAt = System.currentTimeMillis()
        
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("budgets")
            .document(budgetId)
            .set(
                mapOf(
                    "budgetId" to doc.budgetId,
                    "category" to doc.category,
                    "limit" to doc.limit,
                    "spent" to doc.spent,
                    "period" to doc.period,
                    "alertThreshold" to doc.alertThreshold,
                    "createdAt" to createdAt
                )
            )
            .await()
        
        return budget.copy(budgetId = budgetId, createdAt = createdAt)
    }

    /**
     * Updates an existing budget in Firestore.
     */
    suspend fun updateBudget(budget: Budget) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        val doc = Budget.toDocument(budget)
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("budgets")
            .document(budget.budgetId)
            .update(
                mapOf(
                    "category" to doc.category,
                    "limit" to doc.limit,
                    "spent" to doc.spent,
                    "period" to doc.period,
                    "alertThreshold" to doc.alertThreshold
                )
            )
            .await()
    }

    /**
     * Deletes a budget from Firestore.
     */
    suspend fun deleteBudget(budgetId: String) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("budgets")
            .document(budgetId)
            .delete()
            .await()
    }

    /**
     * Gets a single budget by ID.
     */
    suspend fun getBudget(budgetId: String): Budget? {
        val currentUserId = userId ?: return null
        val doc = firestore
            .collection("users")
            .document(currentUserId)
            .collection("budgets")
            .document(budgetId)
            .get()
            .await()
        
        return if (doc.exists()) {
            try {
                Budget.fromDocument(
                    BudgetDocument(
                        budgetId = doc.getString("budgetId") ?: budgetId,
                        category = doc.getString("category") ?: "",
                        limit = doc.getDouble("limit") ?: 0.0,
                        spent = doc.getDouble("spent") ?: 0.0,
                        period = doc.getString("period") ?: "monthly",
                        alertThreshold = doc.getDouble("alertThreshold") ?: 0.8,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Updates the spent amount for a budget.
     * This is typically called when transactions are synced.
     */
    suspend fun updateBudgetSpent(budgetId: String, spent: Double) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("budgets")
            .document(budgetId)
            .update("spent", spent)
            .await()
    }
}

