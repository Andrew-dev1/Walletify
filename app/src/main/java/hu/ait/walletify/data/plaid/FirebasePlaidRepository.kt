package hu.ait.walletify.data.plaid

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePlaidRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth // Inject auth to check user state

) : PlaidRepository {


    override suspend fun createLinkToken(): Result<String> = runCatching {
        val result = functions
            .getHttpsCallable("createLinkToken")
            .call()
            .await()
        Log.d("FirebasePlaidRepository", "createLinkToken response: $result")

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        data["link_token"] as? String
            ?: throw IllegalStateException("Link token not found in response")
        return Result.success(data["link_token"] as String)
    }
    suspend fun createLinkToken2(): String  {
        val currentUser = Firebase.auth.currentUser
        Log.d("FirebasePlaidRepository", currentUser.toString())
        currentUser?.getIdToken(true)?.await()


        try {
            val result = functions
                .getHttpsCallable("createLinkToken")
                .call()
                .await()
            Log.d("FirebasePlaidRepository", "createLinkToken response: $result")

            val data = result.data as? Map<*, *>
                ?: throw IllegalStateException("Invalid response format")

            data["link_token"] as? String
                ?: throw IllegalStateException("Link token not found in response")
        }catch (e: Exception){
            Log.d("FirebasePlaidRepository", "createLinkToken error: $e")
        }
        Log.d("FirebasePlaidRepository", "createLinkToken end")
        return ""
    }

    override suspend fun exchangePublicToken(publicToken: String): Result<String> = runCatching {
        val params = hashMapOf("publicToken" to publicToken)
        val result = functions
            .getHttpsCallable("exchangePublicToken")
            .call(params)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Invalid response format")

        data["item_id"] as? String
            ?: throw IllegalStateException("Link token not found in response")
    }
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
//}