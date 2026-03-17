package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecurringExpenseCreateRequest(
    val title: String,
    val amount: Double,
    val category: String,
    @SerializedName("paid_by_user_id") val paidByUserId: String? = null,
    @SerializedName("paid_by_guest_id") val paidByGuestId: String? = null,
    @SerializedName("split_type") val splitType: String,
    val splits: List<SplitInputDto> = emptyList(),
    val frequency: String,
    @SerializedName("start_date") val startDate: String,
)

data class RecurringExpenseResponseDto(
    val id: String,
    @SerializedName("group_id") val groupId: String,
    val title: String,
    val amount: Double,
    val category: String,
    @SerializedName("paid_by_user_id") val paidByUserId: String?,
    @SerializedName("paid_by_guest_id") val paidByGuestId: String?,
    @SerializedName("paid_by_name") val paidByName: String,
    @SerializedName("split_type") val splitType: String,
    val frequency: String,
    @SerializedName("next_due_date") val nextDueDate: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
)
