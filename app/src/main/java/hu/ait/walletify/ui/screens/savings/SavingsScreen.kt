package hu.ait.walletify.ui.screens.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.ait.walletify.data.model.BudgetAlert
import hu.ait.walletify.data.model.SavingsGoal
import hu.ait.walletify.data.model.SavingsSnapshot

@Composable
fun SavingsScreen(state: SavingsUiState, modifier: Modifier) {
    when (state) {
        SavingsUiState.Loading -> Text("Loading savings...", modifier = modifier.padding(16.dp))
        is SavingsUiState.Data -> SavingsContent(state.snapshot, modifier)
    }
}

@Composable
private fun SavingsContent(snapshot: SavingsSnapshot, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Savings goals", style = MaterialTheme.typography.titleMedium)
        }
        items(snapshot.goals, key = { it.id }) { goal ->
            GoalCard(goal)
        }
        item {
            Text("Budget alerts", style = MaterialTheme.typography.titleMedium)
        }
        items(snapshot.alerts, key = { it.category }) { alert ->
            AlertCard(alert)
        }
    }
}

@Composable
private fun GoalCard(goal: SavingsGoal) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(goal.title, style = MaterialTheme.typography.titleSmall)
            Text("Target: $${goal.target}")
            Text("Contributed: $${goal.contributed}")
            Text("Due: ${goal.dueBy}")
            Text("Status: ${goal.status}")
        }
    }
}

@Composable
private fun AlertCard(alert: BudgetAlert) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("${alert.category} limit", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Limit: $${alert.limit}")
                Text("Spent: $${alert.spent}")
            }
            Text("Severity: ${alert.severity}")
        }
    }
}

