package hu.ait.walletify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import dagger.hilt.android.AndroidEntryPoint
import hu.ait.walletify.ui.navigation.NavHost
import hu.ait.walletify.ui.theme.WalletifyTheme

@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    private val linkAccountToPlaid = registerForActivityResult(FastOpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> {} // handleSuccess(result)
            is LinkExit -> {} // handleFailure(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalletifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {innerPadding ->
                    NavHost(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}



