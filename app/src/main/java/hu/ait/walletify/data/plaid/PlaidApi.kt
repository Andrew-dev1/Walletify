package hu.ait.walletify.data.plaid

import retrofit2.http.Body
import retrofit2.http.POST

interface PlaidApi {

    @POST("link/token/create")
    suspend fun createLinkToken(
        @Body request: CreateLinkTokenRequest
    ): LinkTokenResponse

    @POST("item/public_token/exchange")
    suspend fun exchangePublicToken(
        @Body request: ExchangePublicTokenRequest
    ): ExchangePublicTokenResponse
}


