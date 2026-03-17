package com.expensio.data.repository

import com.expensio.data.remote.api.AnalyticsApi
import com.expensio.domain.model.Analytics
import com.expensio.domain.model.CategorySpend
import com.expensio.domain.model.MonthlySpend
import com.expensio.domain.repository.AnalyticsRepository
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val api: AnalyticsApi,
) : AnalyticsRepository {

    override suspend fun getAnalytics(range: String): Analytics {
        val dto = api.getAnalytics(range)
        return Analytics(
            totalSpent = dto.totalSpent,
            thisMonth = dto.thisMonth,
            lastMonth = dto.lastMonth,
            netBalance = dto.netBalance,
            groupCount = dto.groupCount,
            byCategory = dto.byCategory.map {
                CategorySpend(it.category, it.amount, it.count, it.percentage)
            },
            byMonth = dto.byMonth.map { MonthlySpend(it.month, it.amount) },
        )
    }
}
