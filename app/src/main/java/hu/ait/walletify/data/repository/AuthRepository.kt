package hu.ait.walletify.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.protobuf.sourceContext
import hu.ait.walletify.data.model.UserProfile
import hu.ait.walletify.ui.screens.auth.LoginUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication abstraction so we can swap between fake data and Plaid/Firebase later.
 * Business logic lives here while composables only react to exposed flows.
 */
interface AuthRepository {
    val activeUser: Flow<UserProfile?>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(name: String, email: String,
                         password: String, source:
                         String, purpose: String): Result<UserProfile>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val userState = MutableStateFlow<UserProfile?>(null)
    override val activeUser: Flow<UserProfile?> = userState.asStateFlow()

    init {
        // Check if user is already logged in
        auth.currentUser?.let { firebaseUser ->
            // Optionally load user profile from Firestore here
        }
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(
                Exception("User ID is null")
            )

            // Load user profile from Firestore
            val profile = loadUserProfile(userId, email)
            userState.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String,
                                  password: String, source:
                                  String, purpose: String): Result<UserProfile> {
        return try {
            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(
                Exception("User ID is null")
            )

            // Create user profile
            val profile = UserProfile(
                id = userId,
                name = name,
                email = email,
                householdMembers = 1,
                pushNotificationsEnabled = true,
                source = source,
                purpose = purpose
            )

            // Save to Firestore
            saveUserProfile(profile)

            userState.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
        userState.value = null
    }

    private suspend fun saveUserProfile(profile: UserProfile) {
        firestore.collection("users")
            .document(profile.id)
            .set(
                mapOf(
                    "name" to profile.name,
                    "email" to profile.email,
                    "householdMembers" to profile.householdMembers,
                    "pushNotificationsEnabled" to profile.pushNotificationsEnabled,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    private suspend fun loadUserProfile(userId: String, email: String): UserProfile {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            UserProfile(
                id = userId,
                name = document.getString("name") ?: "User",
                email = email,
                householdMembers = document.getLong("householdMembers")?.toInt() ?: 1,
                pushNotificationsEnabled = document.getBoolean("pushNotificationsEnabled") ?: true,
                source = document.getString("source") ?: "unknown source",
                purpose = document.getString("purpose") ?: "unknown purpose"
            )
        } catch (e: Exception) {
            // If Firestore fetch fails, create a basic profile
            UserProfile(
                id = userId,
                name = "User",
                email = email,
                householdMembers = 1,
                pushNotificationsEnabled = true,
                purpose = "unknown purpose",
                source = "unknown source"
            )
        }
    }
}
