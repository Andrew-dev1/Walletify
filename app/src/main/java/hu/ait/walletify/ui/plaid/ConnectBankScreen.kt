package hu.ait.walletify.ui.plaid

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
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkResultHandler
import com.plaid.link.result.LinkSuccess

@Composable
fun rememberPlaidHandler(
    configuration: LinkTokenConfiguration?,
    onSuccess: (LinkSuccess) -> Unit,
    onExit: (LinkExit) -> Unit
): PlaidHandler? {
    val context = LocalContext.current

    return remember(configuration) {
        configuration?.let {
            PlaidHandler.create(
                context.applicationContext as android.app.Application,
                it
            ).apply {
                onSuccess { result -> onSuccess(result) }
                onExit { result -> onExit(result) }
            }
        }
    }
}

@Composable
fun ConnectBankScreen(
    viewModel: PlaidViewModel = hiltViewModel(),
    onConnected: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState = viewModel.plaidUiState

    // Create Plaid Link configuration when we receive link token
    val linkTokenConfiguration = remember(uiState) {
        if (uiState is PlaidUiState.LinkTokenReceived) {
            LinkTokenConfiguration.Builder()
                .token(uiState.linkToken)
                .build()
        } else null
    }

    // Create Plaid handler
    val plaidHandler = rememberPlaidHandler(
        configuration = linkTokenConfiguration,
        onSuccess = { success ->
            viewModel.exchangePublicToken(success.publicToken)
        },
        onExit = { exit ->
            if (exit.error != null) {
                Toast.makeText(
                    context,
                    "Link error: ${exit.error.displayMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Link cancelled", Toast.LENGTH_SHORT).show()
            }
            viewModel.resetState()
        }
    )

    // Launch Plaid Link when token is ready
    LaunchedEffect(uiState) {
        if (uiState is PlaidUiState.LinkTokenReceived) {
            plaidHandler?.open()
        }
    }

    // Launch Plaid Link when token is ready
    LaunchedEffect(linkTokenConfiguration) {
        linkTokenConfiguration?.let { config ->
            try {
                val linkIntent = Plaid.create(context.applicationContext, config)
                linkActivityResult.launch(linkIntent)
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
    LaunchedEffect(uiState) {
        if (uiState is PlaidUiState.Connected) {
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
            when (uiState) {
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
                            text = uiState.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
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
                enabled = uiState !is PlaidUiState.Loading
            ) {
                Text(
                    text = if (uiState is PlaidUiState.Error) "Try Again" else "Connect Bank Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

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