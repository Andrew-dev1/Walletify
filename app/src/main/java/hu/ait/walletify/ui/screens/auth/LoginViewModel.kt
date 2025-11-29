package hu.ait.walletify.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


sealed interface LoginUiState{
    object Init: LoginUiState
    object Loading: LoginUiState
    object RegisterSuccess: LoginUiState
    object LoginSuccess: LoginUiState
    data class Error(val errorMessage: String?): LoginUiState

}

data class UserInfo(
    val email: String,
    val password: String,
    val purpose: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)


@HiltViewModel
class LoginViewModel @Inject constructor(): ViewModel(){
    var loginUiState: LoginUiState by mutableStateOf(LoginUiState.Init)
    private lateinit var auth: FirebaseAuth

    init {
        auth = Firebase.auth
    }

    fun registerUser(email: String, password: String, purpose: String, source: String) {
        loginUiState = LoginUiState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Only add to Firestore after successful authentication
                val db = Firebase.firestore
                val userInfo = UserInfo(
                    email = email,
                    password = password,
                    "",
                    "",
                    createdAt = System.currentTimeMillis()
                )
                loginUiState = LoginUiState.RegisterSuccess

            }
            .addOnFailureListener { authError ->
                loginUiState = LoginUiState.Error(
                    authError.localizedMessage ?: "Registration failed"
                )
            }
    }

    suspend fun loginUser(email: String, password: String) : AuthResult? {
        loginUiState = LoginUiState.Loading
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            loginUiState = LoginUiState.LoginSuccess
            result
        } catch (e: Exception) {
            loginUiState = LoginUiState.Error(
                e.localizedMessage ?: "Login failed"
            )
            e.printStackTrace()
            null
        }
    }

    suspend fun forgetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            loginUiState = LoginUiState.Error(
                e.localizedMessage ?: "Failed to send reset email"
            )
            false
        }
    }

    fun resetState() {
        loginUiState = LoginUiState.Init
    }



}