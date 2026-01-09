package hu.ait.walletify.ui.screens.dashboard

import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.protobuf.enum
import hu.ait.walletify.data.model.DashboardSnapshot
import hu.ait.walletify.data.model.TransactionItem



data class MonthlyData(
    val month: String,
    val amount: Double
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    modifier: Modifier) {

    when (state) {
        DashboardUiState.Loading -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
        is DashboardUiState.Error -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFE53935)
                )
                Spacer(Modifier.height(16.dp))
                Text(state.message, color = Color(0xFFE53935))
            }
        }
        is DashboardUiState.Data -> DashboardContent(
            snapshot = state.snapshot,
            modifier = modifier
        )
    }
}
@Composable
fun DashboardContent(
    snapshot: DashboardSnapshot,
    modifier:Modifier
) {

    val currentMonth = "November"
    val totalSpent = snapshot.monthToDateSpend
    val cashFlow = snapshot.netCashFlow

    val currentMonthSpent = snapshot.insights[0].spent
    val lastMonthSpent = snapshot.insights[1].spent

    // Mock spending data
    val spendingData = listOf(
        MonthlyData("Oct", 3124.20),
        MonthlyData("Nov", 2847.50)
    )


    val recentTransactions2 = snapshot.recentTransactions
    val remainingTasks = snapshot.onboardingChecklist

    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Line Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Spending Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "This month vs Last month",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Mock line chart visualization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        spendingData.forEach { data ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height((data.amount / 40).dp)
                                        .background(
                                            color = if (data.month == "Nov")
                                                Color(0xFF4CAF50)
                                            else
                                                Color(0xFFE0E0E0),
                                            shape = MaterialTheme.shapes.small
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = data.month,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${String.format("%.0f", data.amount)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val difference = lastMonthSpent - totalSpent
                    val percentChange = (difference / lastMonthSpent) * 100

                    Text(
                        text = "You spent ${"%.2f".format(difference)} less than last month (${String.format("%.1f", percentChange)}% decrease)",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Spent Card
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total Spent",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "%.2f".format(totalSpent),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Budget Remaining Card
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Cash Flow",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$%.2f".format(cashFlow),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cashFlow >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Setup Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column (
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Complete Your Setup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val incompleteTasks = remainingTasks.filter { !it.completed }

                    incompleteTasks.forEachIndexed { index, action ->
                        SetupActionItem(
                            action.title,
                            action.description,
                            onClick = { /* Handle action click */ }
                        )

                        // Only add divider if not the last item
                        if (index < incompleteTasks.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                }
            }
        }

        // Recent Transactions
        item {
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }



        items(
            items = recentTransactions2,
            key = { it.transactionId }
        ) { transaction ->
            TransactionItemCard(transaction = transaction)
        }
        item {
            TextButton(
                onClick = { /* Navigate to all transactions */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View All Transactions")
            }
        }


        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

}

@Composable
fun SetupActionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        TextButton(onClick = onClick) {
            Text("Set Up", color = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun TransactionItemCard(transaction: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.category.first().first().toString(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column {
                    Text(
                        text = transaction.merchantName?: "Blank",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${transaction.primaryCategory} • ${transaction.formattedDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "${if (transaction.isDebit) "" else "+"}${
                    "%.2f".format(transaction.amount)
                }",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.isDebit)
                    MaterialTheme.colorScheme.onBackground
                else
                    Color(0xFF4CAF50)
            )
        }
    }
}