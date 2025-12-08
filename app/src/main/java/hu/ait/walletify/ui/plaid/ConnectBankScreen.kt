package hu.ait.walletify.ui.plaid

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkResultHandler
import com.plaid.link.result.LinkSuccess



/**
 * ConnectBankScreen using the current Plaid Link Android SDK best practices.
 * Uses Activity Result API for handling Plaid Link flow.
 */
@Composable
fun ConnectBankScreen(
    state: PlaidUiState,
    viewModel: PlaidViewModel = hiltViewModel(),
    onConnected: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Create Plaid Link configuration when we receive link token
//    val linkTokenConfiguration = remember(state) {
//        if (state is PlaidUiState.LinkTokenReceived) {
//            LinkTokenConfiguration.Builder()
//                .token(state.linkToken)
//                .build()
//
//        } else null
//    }
//
//    // create link result handler, Set Up Parsing in onActivityResult, Create a PlaidHandler, Open Link
//    private val linkAccountToPlaid =
//        registerForActivityResult(FastOpenPlaidLink()) {
//            when (it) {
//                is LinkSuccess -> /* handle LinkSuccess */
//                is LinkExit -> /* handle LinkExit */
//            }
//        }
//    val resultHandler = LinkResultHandler(
//        onSuccess = {succeeded: LinkSuccess ->
//            // Extract public token from successful link
//            viewModel.exchangePublicToken(succeeded.publicToken)
//        },
//
//        onExit =  { exit: LinkExit ->
//            // Handle user exit or error
//            if (exit.error != null) {
//                Toast.makeText(
//                    context,
//                    "Link error: ${exit.error?.displayMessage}",
//                    Toast.LENGTH_LONG
//                ).show()
//            } else {
//                Toast.makeText(
//                    context,
//                    "Link cancelled",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            viewModel.resetState()
//        }
//    )
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (!resultHandler.onActivityResult(requestCode, resultCode, data)) {
//            // Not handled by the LinkResultHandler
//        }
//    }
//    resultHandler.onActivityResult()
//
//
//    if (!resultHandler.onActivityResult(0, activityResult.resultCode, activityResult.data)) {
//        Toast.makeText(context, "Unexpected result", Toast.LENGTH_SHORT).show()
//    }
//    // Activity Result Launcher for Plaid Link
//    // Uses the modern Activity Result API recommended by Plaid SDK 5.x
//    val plaidLinkLauncher = rememberLauncherForActivityResult(
//        contract  = FastOpenPlaidLink()
//    ) { activityResult ->
//        val resultHandler = LinkResultHandler(
//            onSuccess = {succeeded: LinkSuccess ->
//                // Extract public token from successful link
//                viewModel.exchangePublicToken(succeeded.publicToken)
//            },
//
//            onExit =  { exit: LinkExit ->
//                // Handle user exit or error
//                if (exit.error != null) {
//                    Toast.makeText(
//                        context,
//                        "Link error: ${exit.error?.displayMessage}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                } else {
//                    Toast.makeText(
//                        context,
//                        "Link cancelled",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                viewModel.resetState()
//            }
//        )
//        if (!resultHandler.onActivityResult(0, activityResult.resultCode, activityResult.data)) {
//            Toast.makeText(context, "Unexpected result", Toast.LENGTH_SHORT).show()
//        }
//        // Handle Plaid Link result using LinkResultHandler
//        // This is the recommended approach for Plaid SDK 5.x
//
//    }
//
//
//    // Launch Plaid Link when token is ready
//    LaunchedEffect(state) {
//        if (state is PlaidUiState.LinkTokenReceived) {
//            plaidHandler?.open()
//        }
//    }
//
//    // Launch Plaid Link when token is ready
//    LaunchedEffect(linkTokenConfiguration) {
//        linkTokenConfiguration?.let { config ->
//            try {
//                val linkIntent = Plaid.create(
//                    context.applicationContext,
//                    config
//                )
//                plaidLinkLauncher.launch(linkIntent)
//            } catch (e: Exception) {
//                Toast.makeText(
//                    context,
//                    "Failed to launch Plaid Link: ${e.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//                viewModel.resetState()
//            }
//        }
//    }
//    // Create PlaidHandler with config and result handler
//    val plaidHandler = remember(linkTokenConfiguration) {
//        linkTokenConfiguration?.let { config ->
//            Plaid.create(
//                context.applicationContext as android.app.Application,
//                config,
//                resultHandler
//            )
//        }
//    }
//
//    // Launch Plaid when handler is ready
//    LaunchedEffect(plaidHandler) {
//        plaidHandler?.open(this)
//    }
//
//    // Navigate to dashboard on successful connection
//    LaunchedEffect(state) {
//        if (state is PlaidUiState.Connected) {
//            Toast.makeText(context, "Bank account connected!", Toast.LENGTH_SHORT).show()
//            onConnected()
//        }
//    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connect Your Bank",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Securely link your bank account to automatically track transactions and manage your spending.",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Security note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔒 Bank-level security",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Your credentials are encrypted and we never store your banking password. Powered by Plaid, trusted by thousands of financial apps.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Show loading or error state
//            when (state) {
//                is PlaidUiState.Loading -> {
//                    CircularProgressIndicator(
//                        color = Color(0xFF4CAF50),
//                        modifier = Modifier.size(48.dp)
//                    )
//                    Text(
//                        text = "Connecting to Plaid...",
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
//                        modifier = Modifier.padding(top = 16.dp)
//                    )
//                }
//                is PlaidUiState.Error -> {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
//                        )
//                    ) {
//                        Text(
//                            text = (state).message,
//                            fontSize = 14.sp,
//                            color = MaterialTheme.colorScheme.error,
//                            modifier = Modifier.padding(16.dp),
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                }
//                else -> {}
//            }
        }


        // Bottom buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = { viewModel.createLinkToken() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = state !is PlaidUiState.Loading
            ) {
                Text(
                    text = if (state is PlaidUiState.Error) "Try Again" else "Connect Bank Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(text = if (state is PlaidUiState.LinkTokenReceived) state.linkToken.toString() else "Connect Bank Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold)






            // Optional skip button
            onSkip?.let { skip ->
                TextButton(
                    onClick = skip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for now",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}