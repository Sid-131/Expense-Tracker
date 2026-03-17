package com.expensio.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.expensio.domain.model.EXPENSE_CATEGORIES
import com.expensio.domain.model.Frequency
import com.expensio.domain.model.GroupMember
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecurringExpenseScreen(
    groupId: String,
    members: List<GroupMember>,
    navController: NavController,
    viewModel: RecurringExpenseViewModel = hiltViewModel(),
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var selectedPaidBy by remember { mutableStateOf(members.firstOrNull()) }
    var selectedFrequency by remember { mutableStateOf(Frequency.MONTHLY) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paidByExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Recurring Expense") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = {
                            selectedCategory = cat; categoryExpanded = false
                        })
                    }
                }
            }

            // Paid by dropdown
            if (members.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedPaidBy?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(paidByExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = paidByExpanded, onDismissRequest = { paidByExpanded = false }) {
                        members.forEach { member ->
                            DropdownMenuItem(text = { Text(member.name) }, onClick = {
                                selectedPaidBy = member; paidByExpanded = false
                            })
                        }
                    }
                }
            }

            // Frequency dropdown
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedFrequency.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                    Frequency.entries.forEach { freq ->
                        DropdownMenuItem(text = { Text(freq.label) }, onClick = {
                            selectedFrequency = freq; frequencyExpanded = false
                        })
                    }
                }
            }

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: return@Button
                    val paidBy = selectedPaidBy ?: return@Button
                    val startDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    viewModel.create(
                        groupId = groupId,
                        title = title.trim(),
                        amount = amountDouble,
                        category = selectedCategory,
                        paidByUserId = paidBy.userId,
                        paidByGuestId = paidBy.guestId,
                        splitType = "EQUAL",
                        splits = emptyList(),
                        frequency = selectedFrequency.name,
                        startDate = startDate,
                        onSuccess = { navController.popBackStack() },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && amount.isNotBlank() && selectedPaidBy != null,
            ) {
                Text("Create Recurring Expense")
            }
        }
    }
}
