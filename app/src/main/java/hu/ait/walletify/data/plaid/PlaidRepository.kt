package hu.ait.walletify.data.plaid

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PlaidRepository {
    suspend fun createLinkToken(): Result<String>
    suspend fun exchangePublicToken(publicToken: String): Result<String>
}

@Singleton
class PlaidSandboxRepository @Inject constructor(
    private val plaidApi: PlaidApi,
    private val plaidConfig: PlaidConfig
) : PlaidRepository {

    override suspend fun createLinkToken(): Result<String> = runCatching {
        ensureConfigPresent()
        val request = CreateLinkTokenRequest(
            clientId = plaidConfig.clientId,
            secret = plaidConfig.secret,
            clientName = plaidConfig.clientName,
            user = PlaidUser(clientUserId = UUID.randomUUID().toString())
        )
        plaidApi.createLinkToken(request).linkToken
    }

    override suspend fun exchangePublicToken(publicToken: String): Result<String> = runCatching {
        ensureConfigPresent()
        val response = plaidApi.exchangePublicToken(
            ExchangePublicTokenRequest(
                clientId = plaidConfig.clientId,
                secret = plaidConfig.secret,
                publicToken = publicToken
            )
        )
        response.accessToken
    }

    private fun ensureConfigPresent() {
        require(plaidConfig.clientId.isNotBlank() && plaidConfig.secret.isNotBlank()) {
            "PLAID_CLIENT_ID and PLAID_SECRET must be provided in local.properties or environment variables."
        }
    }
}


