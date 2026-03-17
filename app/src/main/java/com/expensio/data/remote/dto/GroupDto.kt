package com.expensio.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupCreateRequest(
    val name: String,
)

data class GroupResponseDto(
    val id: String,
    val name: String,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("member_count") val memberCount: Int,
)

data class MemberResponseDto(
    val id: String,
    val name: String,
    @SerializedName("is_guest") val isGuest: Boolean,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("guest_id") val guestId: String?,
    @SerializedName("profile_pic") val profilePic: String?,
)

data class GroupDetailResponseDto(
    val id: String,
    val name: String,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String,
    val members: List<MemberResponseDto>,
)

data class AddMemberRequest(
    val email: String? = null,
    @SerializedName("guest_name") val guestName: String? = null,
)

data class UserSearchResponseDto(
    val id: String,
    val name: String,
    val email: String?,
    @SerializedName("profile_pic") val profilePic: String?,
)
