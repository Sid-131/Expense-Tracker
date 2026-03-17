package com.expensio.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.Group
import com.expensio.domain.model.GroupDetail
import com.expensio.domain.model.UserSearchResult
import com.expensio.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _groupDetail = MutableStateFlow<GroupDetail?>(null)
    val groupDetail = _groupDetail.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess = _actionSuccess.asStateFlow()

    fun loadGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.getGroups()
                .onSuccess { _groups.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadGroupDetail(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.getGroupDetail(groupId)
                .onSuccess { _groupDetail.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun createGroup(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.createGroup(name)
                .onSuccess {
                    loadGroups()
                    onSuccess()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addMemberByEmail(groupId: String, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.addMemberByEmail(groupId, email)
                .onSuccess {
                    loadGroupDetail(groupId)
                    _actionSuccess.value = "${it.name} added to group"
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addGuestMember(groupId: String, guestName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            groupRepository.addGuestMember(groupId, guestName)
                .onSuccess {
                    loadGroupDetail(groupId)
                    _actionSuccess.value = "${it.name} added as guest"
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun removeMember(groupId: String, memberId: String) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, memberId)
                .onSuccess { loadGroupDetail(groupId) }
                .onFailure { _error.value = it.message }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            groupRepository.searchUsers(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchResults.value = emptyList() }
        }
    }

    fun clearError() { _error.value = null }
    fun clearActionSuccess() { _actionSuccess.value = null }
    fun clearGroupDetail() { _groupDetail.value = null }
}
