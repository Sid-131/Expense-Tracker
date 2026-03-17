package com.expensio.ui.settlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.Settlement
import com.expensio.domain.model.SettlementSuggestion
import com.expensio.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val repository: SettlementRepository,
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<SettlementSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SettlementSuggestion>> = _suggestions

    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess

    fun loadSettlements(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _suggestions.value = repository.getSuggestions(groupId)
                _settlements.value = repository.getGroupSettlements(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load settlements"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun settle(
        groupId: String,
        suggestion: SettlementSuggestion,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createSettlement(
                    groupId = groupId,
                    fromUserId = suggestion.fromUserId,
                    fromGuestId = suggestion.fromGuestId,
                    toUserId = suggestion.toUserId,
                    toGuestId = suggestion.toGuestId,
                    amount = suggestion.amount,
                )
                _actionSuccess.value = "${suggestion.fromName} settled ₹%.2f with ${suggestion.toName}".format(suggestion.amount)
                loadSettlements(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create settlement"
                _isLoading.value = false
            }
        }
    }

    fun completeSettlement(groupId: String, settlementId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.completeSettlement(settlementId)
                _actionSuccess.value = "Settlement marked as completed"
                loadSettlements(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete settlement"
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearActionSuccess() { _actionSuccess.value = null }
}
