package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SettlementCreateRequest(
    @SerializedName("from_user_id") val fromUserId: String? = null,
    @SerializedName("from_guest_id") val fromGuestId: String? = null,
    @SerializedName("to_user_id") val toUserId: String? = null,
    @SerializedName("to_guest_id") val toGuestId: String? = null,
    val amount: Double,
)

data class SettlementResponseDto(
    val id: String,
    @SerializedName("group_id") val groupId: String,
    @SerializedName("from_user_id") val fromUserId: String?,
    @SerializedName("from_guest_id") val fromGuestId: String?,
    @SerializedName("from_name") val fromName: String,
    @SerializedName("to_user_id") val toUserId: String?,
    @SerializedName("to_guest_id") val toGuestId: String?,
    @SerializedName("to_name") val toName: String,
    val amount: Double,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("completed_at") val completedAt: String?,
)

data class SettlementSuggestionDto(
    @SerializedName("from_user_id") val fromUserId: String?,
    @SerializedName("from_guest_id") val fromGuestId: String?,
    @SerializedName("from_name") val fromName: String,
    @SerializedName("to_user_id") val toUserId: String?,
    @SerializedName("to_guest_id") val toGuestId: String?,
    @SerializedName("to_name") val toName: String,
    val amount: Double,
)
