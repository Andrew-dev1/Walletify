package hu.ait.walletify.ui.plaid

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

/**
 * ConnectBankScreen using the current Plaid Link Android SDK best practices.
 * Uses Activity Result API for handling Plaid Link flow.
 * 
 * Flow:
 * 1. User clicks "Connect Bank Account" -> ViewModel creates link token
 * 2. When link token is received -> Create LinkTokenConfiguration and launch Plaid Link
 * 3. User completes Plaid Link flow -> Receive publicToken
 * 4. Exchange publicToken for accessToken -> Navigate to dashboard
 */
@Composable
fun ConnectBankScreen(
    state: PlaidUiState,
    viewModel: PlaidViewModel = viewModel(),
    onConnected: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
        val application = context.applicationContext as Application

    // Create Plaid Link configuration when we receive link token
    val linkTokenConfiguration = remember(state) {
        if (state is PlaidUiState.LinkTokenReceived) {
            LinkTokenConfiguration.Builder()
                .token(state.linkToken)
                .build()
        } else null
    }


    // Activity Result Launcher for Plaid Link
    // Uses the modern Activity Result API recommended by Plaid SDK 5.x
    val plaidLinkLauncher = rememberLauncherForActivityResult(
        contract = FastOpenPlaidLink()
    ) { result ->
        when (result) {
            is LinkSuccess -> {
                // Extract public token from successful link
                viewModel.exchangePublicToken(result.publicToken)
            }
            is LinkExit -> {
                // Handle user exit or error
                result.error?.let { error ->
                    Toast.makeText(
                        context,
                        "Link error: ${error.displayMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                } ?: run {
                    Toast.makeText(
                        context,
                        "Link cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.resetState()
            }
        }
    }

    // Launch Plaid Link when token is ready
    LaunchedEffect(linkTokenConfiguration) {
        linkTokenConfiguration?.let { config ->
            try {
                val linkIntent = Plaid.create(
                    application,
                    config
                )
                plaidLinkLauncher.launch(linkIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to launch Plaid Link: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }
        }
    }

    // Navigate to dashboard on successful connection
    LaunchedEffect(state) {
        if (state is PlaidUiState.Connected) {
            Toast.makeText(context, "Bank account connected!", Toast.LENGTH_SHORT).show()
            onConnected()
        }
    }

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
            when (state) {
                is PlaidUiState.Loading -> {
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Connecting to Plaid...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is PlaidUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is PlaidUiState.LinkTokenReceived -> {
                    // Plaid Link is launching, show loading
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Opening Plaid Link...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is PlaidUiState.Connected -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "✓ Bank account connected successfully!",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                else -> {}
            }
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
                enabled = state !is PlaidUiState.Loading && 
                         state !is PlaidUiState.LinkTokenReceived &&
                         state !is PlaidUiState.Connected
            ) {
                Text(
                    text = when (state) {
                        is PlaidUiState.Error -> "Try Again"
                        is PlaidUiState.Connected -> "Connected"
                        else -> "Connect Bank Account"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Optional skip button
            onSkip?.let { skip ->
                TextButton(
                    onClick = skip,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state !is PlaidUiState.Loading && 
                             state !is PlaidUiState.LinkTokenReceived
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
