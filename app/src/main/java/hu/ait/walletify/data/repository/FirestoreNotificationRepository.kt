package hu.ait.walletify.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hu.ait.walletify.data.model.AppNotification
import hu.ait.walletify.data.model.NotificationDocument
import hu.ait.walletify.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    fun observeNotifications(): Flow<List<AppNotification>> {
        return callbackFlow {
            var registration: com.google.firebase.firestore.ListenerRegistration? = null

            val currentUserId = userId
            if (currentUserId == null) {
                trySend(emptyList())
            } else {
                registration = firestore
                    .collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val notifications = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                NotificationDocument(
                                    notificationId = doc.getString("notificationId") ?: doc.id,
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    type = doc.getString("type") ?: "info",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    relatedBudgetId = doc.getString("relatedBudgetId")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()

                        trySend(notifications)
                    }
            }

            awaitClose { registration?.remove() }
        }.map { docs ->
            docs.map { AppNotification.fromDocument(it) }
        }
    }

    suspend fun createNotification(notification: AppNotification): AppNotification {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        val notificationId = notification.notificationId.ifEmpty {
            firestore.collection("users").document(currentUserId)
                .collection("notifications").document().id
        }

        val doc = AppNotification.toDocument(notification.copy(notificationId = notificationId))

        firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notificationId)
            .set(
                mapOf(
                    "notificationId" to doc.notificationId,
                    "title" to doc.title,
                    "message" to doc.message,
                    "type" to doc.type,
                    "timestamp" to doc.timestamp,
                    "isRead" to doc.isRead,
                    "relatedBudgetId" to doc.relatedBudgetId
                )
            )
            .await()

        return notification.copy(notificationId = notificationId)
    }

    suspend fun markAsRead(notificationId: String) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    suspend fun deleteNotification(notificationId: String) {
        val currentUserId = userId ?: throw IllegalStateException("User must be authenticated")
        firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notificationId)
            .delete()
            .await()
    }

    suspend fun getUnreadCount(): Int {
        val currentUserId = userId ?: return 0
        val snapshot = firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .await()
        return snapshot.size()
    }

    /**
     * Check if a budget alert notification already exists for the given budget in the current period.
     * This prevents duplicate notifications for the same budget alert.
     */
    suspend fun hasRecentBudgetAlert(budgetId: String, periodStartTime: Long): Boolean {
        val currentUserId = userId ?: return false
        val snapshot = firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("relatedBudgetId", budgetId)
            .whereGreaterThan("timestamp", periodStartTime)
            .get()
            .await()
        return !snapshot.isEmpty
    }
}
