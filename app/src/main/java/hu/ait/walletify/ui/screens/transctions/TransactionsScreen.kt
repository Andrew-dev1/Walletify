package hu.ait.walletify.ui.screens.transctions


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.ait.walletify.data.model.MonthlyTransactions
import hu.ait.walletify.data.model.TransactionItem

/**
 * Transactions screen displaying chronological transactions with monthly headers.
 * Supports category filtering and follows MVVM pattern with sealed UI state.
 */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is TransactionsUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        }
        is TransactionsUiState.Data -> {
            TransactionsContent(
                months = state.months,
                selectedCategory = state.selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TransactionsContent(
    months: List<MonthlyTransactions>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract all unique categories from transactions (using primary category)
    val allCategories = months
        .flatMap { it.transactions }
        .map { it.primaryCategory }
        .distinct()
        .sorted()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category filter chips
        if (allCategories.isNotEmpty()) {
            CategoryFilterRow(
                categories = allCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Transactions list with monthly headers
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { monthData ->
                if(!monthData.transactions.isEmpty())
                    item {
                        MonthlyHeader(monthLabel = monthData.monthLabel)
                    }

                val filteredTransactions = if (selectedCategory != null) {
                    monthData.transactions.filter { it.primaryCategory == selectedCategory }
                } else {
                    monthData.transactions
                }

                items(
                    items = filteredTransactions,
                    key = { it.transactionId }
                ) { transaction ->
                    TransactionItemCard(transaction = transaction)
                }
            }

            if (months.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" filter chip
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4CAF50),
                selectedLabelColor = Color.White
            )
        )

        // Category filter chips
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun MonthlyHeader(monthLabel: String) {
    Text(
        text = monthLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
    )
}

@Composable
private fun TransactionItemCard(transaction: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        .size(48.dp)
                        .background(
                            color = if (transaction.isDebit) {
                                Color(0xFFE53935).copy(alpha = 0.1f)
                            } else {
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.primaryCategory.uppercase().take(1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (transaction.isDebit) {
                            Color(0xFFE53935)
                        } else {
                            Color(0xFF4CAF50)
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchantName ?: transaction.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.primaryCategory} • ${transaction.formattedDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Text(
                text = "${if (transaction.isDebit) "-" else "+"}$${
                    "%.2f".format(
                        kotlin.math.abs(transaction.amount)
                    )
                }",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.isDebit) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color(0xFF4CAF50)
                }
            )
        }
    }
}