package com.expensio.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.expensio.ui.expenses.ExpenseViewModel
import com.expensio.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    navController: NavController,
    groupViewModel: GroupViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
) {
    val detail by groupViewModel.groupDetail.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val isLoadingGroup by groupViewModel.isLoading.collectAsState()
    val isLoadingExpenses by expenseViewModel.isLoading.collectAsState()
    val error by groupViewModel.error.collectAsState()
    val actionSuccess by groupViewModel.actionSuccess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(groupId) {
        groupViewModel.loadGroupDetail(groupId)
        expenseViewModel.loadExpenses(groupId)
    }
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); groupViewModel.clearError() }
    }
    LaunchedEffect(actionSuccess) {
        actionSuccess?.let { snackbarHostState.showSnackbar(it); groupViewModel.clearActionSuccess() }
    }
    DisposableEffect(Unit) { onDispose { groupViewModel.clearGroupDetail() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(onClick = {
                    navController.navigate(Screen.AddExpense.createRoute(groupId))
                }) { Icon(Icons.Default.Add, contentDescription = "Add expense") }
                1 -> FloatingActionButton(onClick = {
                    navController.navigate(Screen.AddMember.createRoute(groupId))
                }) { Icon(Icons.Default.Add, contentDescription = "Add member") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Expenses") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Members") })
            }

            when (selectedTab) {
                0 -> {
                    if (isLoadingExpenses && expenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (expenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No expenses yet", style = MaterialTheme.typography.bodyLarge)
                                Text("Tap + to add one",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(expenses) { expense ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable {
                                            navController.navigate(
                                                Screen.ExpenseDetail.createRoute(expense.id, groupId)
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(expense.title, style = MaterialTheme.typography.titleMedium)
                                            Text("Paid by ${expense.paidByName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(expense.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary)
                                        }
                                        Text("₹%.2f".format(expense.amount),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (isLoadingGroup && detail == null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        detail?.let { group ->
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        "Members (${group.members.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    HorizontalDivider()
                                }
                                items(group.members) { member ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(member.name, style = MaterialTheme.typography.bodyLarge)
                                            if (member.isGuest) {
                                                Text("Guest", style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
