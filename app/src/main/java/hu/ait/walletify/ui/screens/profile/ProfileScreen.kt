package hu.ait.walletify.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.ait.walletify.data.model.PlaidAccount
import hu.ait.walletify.data.model.UserProfile

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onLinkPlaid: () -> Unit,
    onExchangePublicToken: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        ProfileUiState.Loading -> Text("Loading profile...", modifier = modifier.padding(16.dp))
        is ProfileUiState.Error -> Text(
            text = state.message,
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        is ProfileUiState.Data -> ProfileContent(
            user = state.user,
            accounts = state.plaidAccounts,
            linkToken = state.linkToken,
            isLinking = state.isLinking,
            statusMessage = state.statusMessage,
            onLinkPlaid = onLinkPlaid,
            onExchangePublicToken = onExchangePublicToken,
            onLogout = onLogout,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileContent(
    user: UserProfile,
    accounts: List<PlaidAccount>,
    linkToken: String?,
    isLinking: Boolean,
    statusMessage: String?,
    onLinkPlaid: () -> Unit,
    onExchangePublicToken: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier
) {
    var publicTokenInput by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    Text("Household members: ${user.householdMembers}")
                    Text("Notifications: ${if (user.pushNotificationsEnabled) "On" else "Off"}")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onLinkPlaid,
                    enabled = !isLinking
                ) {
                    Text(if (isLinking) "Requesting…" else "Create link token")
                }
                Button(
                    onClick = onLogout,
                    enabled = !isLinking
                ) {
                    Text("Logout")
                }
            }
        }
        if (linkToken != null) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Sandbox link token", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = linkToken,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Paste this token inside Plaid Link (sandbox) to obtain a public_token.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = publicTokenInput,
                onValueChange = { publicTokenInput = it },
                label = { Text("Sandbox public_token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onExchangePublicToken(publicTokenInput.trim())
                        publicTokenInput = ""
                    },
                    enabled = publicTokenInput.isNotBlank() && !isLinking
                ) {
                    Text("Exchange token")
                }
                TextButton(
                    onClick = { publicTokenInput = "" }
                ) {
                    Text("Clear")
                }
            }
            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        item {
            Text("Connected accounts", style = MaterialTheme.typography.titleMedium)
        }
        if (accounts.isEmpty()) {
            item {
                Text("No accounts connected yet.")
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(account.name, style = MaterialTheme.typography.titleSmall)
                        Text(account.institution, style = MaterialTheme.typography.bodySmall)
                        Text("Balance: $${account.currentBalance}")
                    }
                }
            }
        }
    }
}

