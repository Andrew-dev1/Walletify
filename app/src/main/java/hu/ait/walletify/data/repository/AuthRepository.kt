package hu.ait.walletify.data.repository

import hu.ait.walletify.data.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication abstraction so we can swap between fake data and Plaid/Firebase later.
 * Business logic lives here while composables only react to exposed flows.
 */
interface AuthRepository {
    val activeUser: Flow<UserProfile?>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(name: String, email: String, password: String): Result<UserProfile>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val userState = MutableStateFlow<UserProfile?>(null)
    override val activeUser: Flow<UserProfile?> = userState.asStateFlow()

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        delay(600) // mimic network
        val profile = UserProfile(
            id = "user-${email.hashCode()}",
            name = "Casey",
            email = email,
            householdMembers = 1,
            pushNotificationsEnabled = true
        )
        userState.value = profile
        return Result.success(profile)
    }

    override suspend fun register(name: String, email: String, password: String): Result<UserProfile> {
        delay(800)
        val profile = UserProfile(
            id = "user-${System.currentTimeMillis()}",
            name = name,
            email = email,
            householdMembers = 2,
            pushNotificationsEnabled = false
        )
        userState.value = profile
        return Result.success(profile)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(400)
        return Result.success(Unit)
    }

    override fun logout() {
        userState.value = null
    }
}

