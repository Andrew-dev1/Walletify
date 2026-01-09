package hu.ait.walletify.ui.screens.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.ait.walletify.data.model.AlertSeverity
import hu.ait.walletify.data.model.Budget
import hu.ait.walletify.data.model.BudgetAlert
import hu.ait.walletify.data.model.BudgetPeriod
import hu.ait.walletify.data.model.GoalStatus
import hu.ait.walletify.data.model.SavingsGoal
import hu.ait.walletify.data.model.SavingsSnapshot

/**
 * Savings screen displaying savings goals and budgets.
 * Follows MVVM pattern with sealed UI state.
 */
@Composable
fun SavingsScreen(
    state: SavingsUiState,
    showCreateDialog: Boolean,
    editingBudget: Budget?,
    onShowCreateDialog: () -> Unit,
    onDismissCreateDialog: () -> Unit,
    onCreateBudget: (String, Double, BudgetPeriod) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onEditBudget: (Budget) -> Unit,
    onCancelEditBudget: () -> Unit,
    onUpdateBudget: (String, String, Double, BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        SavingsUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        }
        is SavingsUiState.Data -> {
            Scaffold(
                modifier = modifier,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onShowCreateDialog,
                        containerColor = Color(0xFF4CAF50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Budget",
                            tint = Color.White
                        )
                    }
                }
            ) { innerPadding ->
                SavingsContent(
                    snapshot = state.snapshot,
                    budgets = state.budgets,
                    onDeleteBudget = onDeleteBudget,
                    onEditBudget = onEditBudget,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            if (showCreateDialog) {
                CreateBudgetDialog(
                    availableCategories = state.availableCategories,
                    onDismiss = onDismissCreateDialog,
                    onConfirm = onCreateBudget
                )
            }

            if (editingBudget != null) {
                EditBudgetDialog(
                    budget = editingBudget,
                    availableCategories = state.availableCategories,
                    onDismiss = onCancelEditBudget,
                    onConfirm = { category, limit, period ->
                        onUpdateBudget(editingBudget.budgetId, category, limit, period)
                    }
                )
            }
        }
    }
}

@Composable
private fun SavingsContent(
    snapshot: SavingsSnapshot,
    budgets: List<Budget>,
    onDeleteBudget: (String) -> Unit,
    onEditBudget: (Budget) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Budgets Section Header
        item {
            Text(
                text = "Budgets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
            )
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap + to create a budget",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(budgets, key = { it.budgetId }) { budget ->
                BudgetCard(
                    budget = budget,
                    onDelete = { onDeleteBudget(budget.budgetId) },
                    onEdit = { onEditBudget(budget) }
                )
            }
        }

        // Savings Goals Section Header
        item {
            Text(
                text = "Savings Goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
            )
        }

        if (snapshot.goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No savings goals yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(snapshot.goals, key = { it.id }) { goal ->
                GoalCard(goal = goal)
            }
        }
    }
}

@Composable
private fun GoalCard(goal: SavingsGoal) {
    val progress = (goal.contributed.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f)
    val statusColor = when (goal.status) {
        GoalStatus.ON_TRACK -> Color(0xFF4CAF50)
        GoalStatus.AT_RISK -> Color(0xFFFF9800)
        GoalStatus.MISSED -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = goal.status.name.replace("_", " "),
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Contributed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "%.2f".format(goal.contributed),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "%.2f".format(goal.target),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Due: ${goal.dueBy}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AlertCard(alert: BudgetAlert) {
    val severityColor = when (alert.severity) {
        AlertSeverity.LOW -> Color(0xFFFF9800)
        AlertSeverity.MEDIUM -> Color(0xFFFF5722)
        AlertSeverity.HIGH -> Color(0xFFE53935)
    }

    val percentage = (alert.spent.toFloat() / alert.limit.toFloat() * 100).coerceAtMost(100f)
    val isOverBudget = alert.spent > alert.limit

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
                // Alert icon with colored background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = severityColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOverBudget) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = severityColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = alert.category.first().toString().uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = severityColor
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${"%.0f".format(percentage)}% used • ${
                            when (alert.severity) {
                                AlertSeverity.LOW -> "Low"
                                AlertSeverity.MEDIUM -> "Medium"
                                AlertSeverity.HIGH -> "High"
                            }
                        }",
                        fontSize = 12.sp,
                        color = if (isOverBudget) severityColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.2f".format(alert.spent),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverBudget) severityColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of ${"%.2f".format(alert.limit)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: Budget, onDelete: () -> Unit, onEdit: () -> Unit) {
    val progress = budget.usagePercentage.toFloat().coerceIn(0f, 1f)
    val progressColor = when {
        budget.isExceeded -> Color(0xFFE53935)
        budget.shouldAlert -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = progressColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = budget.category.first().toString().uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = progressColor
                        )
                    }
                    Column {
                        Text(
                            text = budget.category,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = budget.period.value.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete budget",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${String.format("%.2f", budget.spent)} spent",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$${String.format("%.2f", budget.limit)} limit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBudgetDialog(
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, BudgetPeriod) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val defaultCategories = listOf(
        "Food and Drink", "Transportation", "Entertainment",
        "Shopping", "Bills", "Travel", "Health", "Education"
    )
    val allCategories = (availableCategories + defaultCategories).distinct().sorted()

    val isValid = category.isNotBlank() && (limitText.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Limit input
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Limit ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("$") }
                )

                // Period selector
                Column {
                    Text(
                        text = "Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BudgetPeriod.entries.forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = BudgetPeriod.entries.size
                                )
                            ) {
                                Text(period.value.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (isValid) {
                        onConfirm(category, limit, selectedPeriod)
                    }
                },
                enabled = isValid
            ) {
                Text("Create", color = if (isValid) Color(0xFF4CAF50) else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetDialog(
    budget: Budget,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, BudgetPeriod) -> Unit
) {
    var category by remember { mutableStateOf(budget.category) }
    var limitText by remember { mutableStateOf(budget.limit.toString()) }
    var selectedPeriod by remember { mutableStateOf(budget.period) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val defaultCategories = listOf(
        "Food and Drink", "Transportation", "Entertainment",
        "Shopping", "Bills", "Travel", "Health", "Education"
    )
    val allCategories = (availableCategories + defaultCategories).distinct().sorted()

    val isValid = category.isNotBlank() && (limitText.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Limit input
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Limit ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("$") }
                )

                // Period selector
                Column {
                    Text(
                        text = "Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BudgetPeriod.entries.forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = BudgetPeriod.entries.size
                                )
                            ) {
                                Text(period.value.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (isValid) {
                        onConfirm(category, limit, selectedPeriod)
                    }
                },
                enabled = isValid
            ) {
                Text("Save", color = if (isValid) Color(0xFF4CAF50) else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}