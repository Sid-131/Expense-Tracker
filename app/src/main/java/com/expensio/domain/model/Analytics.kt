package com.expensio.domain.model

import java.math.BigDecimal

data class CategorySpend(
    val category: String,
    val amount: BigDecimal,
    val count: Int,
    val percentage: Float,
)

data class MonthlySpend(
    val month: String,   // "2026-01"
    val amount: BigDecimal,
)

data class Analytics(
    val totalSpent: BigDecimal,
    val thisMonth: BigDecimal,
    val lastMonth: BigDecimal,
    val netBalance: BigDecimal,
    val groupCount: Int,
    val byCategory: List<CategorySpend>,
    val byMonth: List<MonthlySpend>,
)
