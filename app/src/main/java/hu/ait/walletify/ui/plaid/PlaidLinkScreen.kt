package hu.ait.walletify.ui.plaid

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

@Composable
fun PlaidLinkScreen(
){
    var plaidHandler by remember { mutableStateOf<PlaidHandler?>(null) }
    var publicToken by remember { mutableStateOf("") }
    var isOpenEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val application = context.applicationContext as Application

    val linkAccountToPlaid = rememberLauncherForActivityResult(
        contract = FastOpenPlaidLink()
    ) { result ->
        when (result) {
            is LinkSuccess -> publicToken = result.publicToken
            is LinkExit -> {}
        }
    }

    fun onLinkTokenSuccess(linkToken: String) {
        val tokenConfiguration = LinkTokenConfiguration.Builder()
            .token(linkToken)
            .build()
        plaidHandler = Plaid.create(application, tokenConfiguration)
        isOpenEnabled = true
    }



}