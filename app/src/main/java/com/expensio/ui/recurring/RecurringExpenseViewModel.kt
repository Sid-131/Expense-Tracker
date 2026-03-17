package com.expensio.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.RecurringExpense
import com.expensio.domain.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringExpenseViewModel @Inject constructor(
    private val repository: RecurringExpenseRepository,
) : ViewModel() {

    private val _recurring = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurring: StateFlow<List<RecurringExpense>> = _recurring

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess

    fun load(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.listRecurring(groupId)
                .onSuccess { _recurring.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun create(
        groupId: String,
        title: String,
        amount: Double,
        category: String,
        paidByUserId: String?,
        paidByGuestId: String?,
        splitType: String,
        splits: List<Map<String, Any>>,
        frequency: String,
        startDate: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createRecurring(
                groupId, title, amount, category, paidByUserId, paidByGuestId,
                splitType, splits, frequency, startDate,
            ).onSuccess {
                load(groupId)
                _actionSuccess.value = "Recurring expense created"
                onSuccess()
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun deactivate(groupId: String, recurringId: String) {
        viewModelScope.launch {
            repository.deactivateRecurring(groupId, recurringId)
                .onSuccess {
                    _recurring.value = _recurring.value.filter { it.id != recurringId }
                    _actionSuccess.value = "Recurring expense removed"
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
    fun clearActionSuccess() { _actionSuccess.value = null }
}
