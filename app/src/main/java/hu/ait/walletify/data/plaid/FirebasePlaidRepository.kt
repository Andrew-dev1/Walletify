package hu.ait.walletify.data.plaid

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePlaidRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : PlaidRepository {

    override suspend fun createLinkToken(): Result<String> = runCatching {
        val result = functions
            .getHttpsCallable("createLinkToken")
            .call()
            .await()

        val data = result.data as Map<*, *>
        data["link_token"] as String
    }

    override suspend fun exchangePublicToken(publicToken: String): Result<String> = runCatching {
        val params = hashMapOf("publicToken" to publicToken)
        val result = functions
            .getHttpsCallable("exchangePublicToken")
            .call(params)
            .await()

        val data = result.data as Map<*, *>
        data["item_id"] as String
    }

//    suspend fun getTransactions(itemId: String, startDate: String, endDate: String): Result<List<Map<String, Any>>> = runCatching {
//        val params = hashMapOf(
//            "itemId" to itemId,
//            "startDate" to startDate,
//            "endDate" to endDate
//        )
//        val result = functions
//            .getHttpsCallable("getTransactions")
//            .call(params)
//            .await()
//
//        val data = result.data as Map<*, *>
//        data["transactions"] as List<Map<String, Any>>
//    }
}