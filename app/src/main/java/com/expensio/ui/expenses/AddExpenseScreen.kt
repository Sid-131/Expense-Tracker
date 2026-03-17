package com.expensio.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.expensio.domain.model.EXPENSE_CATEGORIES
import com.expensio.domain.model.GroupMember
import com.expensio.domain.model.SplitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    groupId: String,
    navController: NavController,
    viewModel: ExpenseViewModel = hiltViewModel(),
) {
    val members by viewModel.groupMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FOOD") }
    var selectedPayer by remember { mutableStateOf<GroupMember?>(null) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    val splitInputs = remember { mutableStateMapOf<String, String>() }

    var categoryExpanded by remember { mutableStateOf(false) }
    var payerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) { viewModel.loadGroupMembers(groupId) }
    LaunchedEffect(members) { if (members.isNotEmpty() && selectedPayer == null) selectedPayer = members[0] }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    fun memberId(m: GroupMember) = m.userId ?: m.guestId ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(
                value = amountStr, onValueChange = { amountStr = it },
                label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            )

            // Category dropdown
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = category, onValueChange = {},
                    readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                    }
                }
            }

            // Paid by dropdown
            if (members.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = payerExpanded, onExpandedChange = { payerExpanded = it }) {
                    OutlinedTextField(
                        value = selectedPayer?.name ?: "Select payer",
                        onValueChange = {}, readOnly = true, label = { Text("Paid by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(payerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = payerExpanded, onDismissRequest = { payerExpanded = false }) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.name}${if (m.isGuest) " (Guest)" else ""}") },
                                onClick = { selectedPayer = m; payerExpanded = false }
                            )
                        }
                    }
                }
            }

            // Split type
            Text("Split type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitType.entries.forEach { type ->
                    FilterChip(
                        selected = splitType == type,
                        onClick = { splitType = type },
                        label = { Text(type.name) },
                    )
                }
            }

            // Split inputs for PERCENTAGE / EXACT
            if (splitType != SplitType.EQUAL && members.isNotEmpty()) {
                val label = if (splitType == SplitType.PERCENTAGE) "%" else "₹"
                members.forEach { m ->
                    val key = memberId(m)
                    OutlinedTextField(
                        value = splitInputs[key] ?: "",
                        onValueChange = { splitInputs[key] = it },
                        label = { Text("${m.name} $label") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            } else if (splitType == SplitType.EQUAL && members.isNotEmpty()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val perPerson = if (members.isNotEmpty()) amount / members.size else 0.0
                Text(
                    "₹%.2f per person (${members.size} members)".format(perPerson),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: return@Button
                    val payer = selectedPayer ?: return@Button
                    val splits: List<Map<String, Any>> = when (splitType) {
                        SplitType.EQUAL -> emptyList()
                        SplitType.PERCENTAGE -> members.map { m ->
                            buildMap {
                                m.userId?.let { put("user_id", it) }
                                m.guestId?.let { put("guest_id", it) }
                                put("percentage", splitInputs[memberId(m)]?.toDoubleOrNull() ?: 0.0)
                            }
                        }
                        SplitType.EXACT -> members.map { m ->
                            buildMap {
                                m.userId?.let { put("user_id", it) }
                                m.guestId?.let { put("guest_id", it) }
                                put("amount", splitInputs[memberId(m)]?.toDoubleOrNull() ?: 0.0)
                            }
                        }
                    }
                    viewModel.createExpense(
                        groupId = groupId, title = title.trim(), amount = amount,
                        category = category, paidByUserId = payer.userId,
                        paidByGuestId = payer.guestId, splitType = splitType.name,
                        splits = splits,
                    ) { navController.popBackStack() }
                },
                enabled = title.isNotBlank() && amountStr.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) CircularProgressIndicator() else Text("Add Expense")
            }
        }
    }
}
