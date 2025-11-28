package hu.ait.walletify.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import hu.ait.walletify.ui.navigation.BottomDestination
import hu.ait.walletify.ui.navigation.DashboardRoute
import hu.ait.walletify.ui.navigation.ProfileRoute
import hu.ait.walletify.ui.navigation.SavingsRoute
import hu.ait.walletify.ui.navigation.TransactionsRoute

@Composable
fun WalletifyBottomBar(
    destinations: List<BottomDestination>,
    current: BottomDestination,
    onDestinationSelected: (BottomDestination) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.iconVector(),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

private fun BottomDestination.iconVector(): ImageVector = when (this) {
    DashboardRoute -> Icons.Default.Dashboard
    TransactionsRoute -> Icons.Default.Assessment
    SavingsRoute -> Icons.Default.Savings
    ProfileRoute -> Icons.Default.AccountCircle
    else -> Icons.Default.Dashboard
}


