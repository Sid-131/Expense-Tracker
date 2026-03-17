package com.expensio.domain.repository

import com.expensio.domain.model.Analytics

interface AnalyticsRepository {
    suspend fun getAnalytics(range: String): Analytics
}
