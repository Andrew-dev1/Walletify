package hu.ait.walletify.data.plaid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkTokenRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("secret") val secret: String,
    @SerialName("client_name") val clientName: String,
    val language: String = "en",
    @SerialName("country_codes") val countryCodes: List<String> = listOf("US"),
    val products: List<String> = listOf("transactions"),
    val user: PlaidUser
)

@Serializable
data class PlaidUser(
    @SerialName("client_user_id") val clientUserId: String
)

@Serializable
data class LinkTokenResponse(
    @SerialName("link_token") val linkToken: String,
    val expiration: String
)

@Serializable
data class ExchangePublicTokenRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("secret") val secret: String,
    @SerialName("public_token") val publicToken: String
)

@Serializable
data class ExchangePublicTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("item_id") val itemId: String
)

data class PlaidConfig(
    val clientId: String,
    val secret: String,
    val clientName: String,
    val environment: String = "sandbox"
)


