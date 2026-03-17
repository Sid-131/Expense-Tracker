package com.expensio.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.Balance
import com.expensio.domain.model.Expense
import com.expensio.domain.model.ExpenseDetail
import com.expensio.domain.model.GroupMember
import com.expensio.domain.repository.ExpenseRepository
import com.expensio.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses = _expenses.asStateFlow()

    private val _expenseDetail = MutableStateFlow<ExpenseDetail?>(null)
    val expenseDetail = _expenseDetail.asStateFlow()

    private val _balances = MutableStateFlow<List<Balance>>(emptyList())
    val balances = _balances.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers = _groupMembers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadExpenses(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            expenseRepository.getGroupExpenses(groupId)
                .onSuccess { _expenses.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroupDetail(groupId)
                .onSuccess { _groupMembers.value = it.members }
                .onFailure { _error.value = it.message }
        }
    }

    fun loadExpenseDetail(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            expenseRepository.getExpenseDetail(expenseId)
                .onSuccess { _expenseDetail.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadBalances(groupId: String) {
        viewModelScope.launch {
            expenseRepository.getGroupBalances(groupId)
                .onSuccess { _balances.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        category: String,
        paidByUserId: String?,
        paidByGuestId: String?,
        splitType: String,
        splits: List<Map<String, Any>>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            expenseRepository.createExpense(
                groupId, title, amount, category, paidByUserId, paidByGuestId, splitType, splits
            )
                .onSuccess { loadExpenses(groupId); onSuccess() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteExpense(expenseId: String, groupId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
                .onSuccess { loadExpenses(groupId) }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
    fun clearExpenseDetail() { _expenseDetail.value = null }
}
