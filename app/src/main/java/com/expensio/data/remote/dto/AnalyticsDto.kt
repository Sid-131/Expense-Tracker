package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class CategorySpendDto(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("count") val count: Int,
    @SerializedName("percentage") val percentage: Float,
)

data class MonthlySpendDto(
    @SerializedName("month") val month: String,
    @SerializedName("amount") val amount: BigDecimal,
)

data class AnalyticsResponseDto(
    @SerializedName("total_spent") val totalSpent: BigDecimal,
    @SerializedName("this_month") val thisMonth: BigDecimal,
    @SerializedName("last_month") val lastMonth: BigDecimal,
    @SerializedName("net_balance") val netBalance: BigDecimal,
    @SerializedName("group_count") val groupCount: Int,
    @SerializedName("by_category") val byCategory: List<CategorySpendDto>,
    @SerializedName("by_month") val byMonth: List<MonthlySpendDto>,
)
