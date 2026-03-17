package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SplitInputDto(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("guest_id") val guestId: String? = null,
    val amount: Double? = null,
    val percentage: Double? = null,
)

data class ExpenseCreateRequest(
    val title: String,
    val amount: Double,
    val category: String,
    @SerializedName("paid_by_user_id") val paidByUserId: String? = null,
    @SerializedName("paid_by_guest_id") val paidByGuestId: String? = null,
    @SerializedName("split_type") val splitType: String,
    val splits: List<SplitInputDto> = emptyList(),
)

data class ExpenseResponseDto(
    val id: String,
    @SerializedName("group_id") val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    @SerializedName("paid_by_user_id") val paidByUserId: String?,
    @SerializedName("paid_by_guest_id") val paidByGuestId: String?,
    @SerializedName("paid_by_name") val paidByName: String,
    @SerializedName("split_type") val splitType: String,
    @SerializedName("created_at") val createdAt: String,
)

data class ExpenseSplitDetailDto(
    val id: String,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("guest_id") val guestId: String?,
    @SerializedName("member_name") val memberName: String,
    val amount: Double,
    val percentage: Double?,
)

data class ExpenseDetailResponseDto(
    val id: String,
    @SerializedName("group_id") val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    @SerializedName("paid_by_user_id") val paidByUserId: String?,
    @SerializedName("paid_by_guest_id") val paidByGuestId: String?,
    @SerializedName("paid_by_name") val paidByName: String,
    @SerializedName("split_type") val splitType: String,
    @SerializedName("created_at") val createdAt: String,
    val splits: List<ExpenseSplitDetailDto>,
)

data class BalanceResponseDto(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("guest_id") val guestId: String?,
    @SerializedName("member_name") val memberName: String,
    @SerializedName("net_amount") val netAmount: Double,
)
