package hu.ait.walletify.data.plaid

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PlaidRepository {
    suspend fun createLinkToken(): Result<String>
    suspend fun exchangePublicToken(publicToken: String): Result<String>
}


