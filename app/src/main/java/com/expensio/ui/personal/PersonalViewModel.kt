package com.expensio.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensio.domain.model.Analytics
import com.expensio.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalViewModel @Inject constructor(
    private val repo: AnalyticsRepository,
) : ViewModel() {

    private val _analytics = MutableStateFlow<Analytics?>(null)
    val analytics: StateFlow<Analytics?> = _analytics

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _range = MutableStateFlow("3m")
    val range: StateFlow<String> = _range

    init {
        load("3m")
    }

    fun setRange(r: String) {
        _range.value = r
        load(r)
    }

    private fun load(range: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { repo.getAnalytics(range) }
                .onSuccess { _analytics.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
