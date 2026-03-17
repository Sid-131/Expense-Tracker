package com.expensio.domain.model

data class Group(
    val id: String,
    val name: String,
    val createdBy: String?,
    val createdAt: String,
    val memberCount: Int,
)

data class GroupMember(
    val id: String,
    val name: String,
    val isGuest: Boolean,
    val userId: String?,
    val guestId: String?,
    val profilePic: String?,
)

data class GroupDetail(
    val id: String,
    val name: String,
    val createdBy: String?,
    val createdAt: String,
    val members: List<GroupMember>,
)

data class UserSearchResult(
    val id: String,
    val name: String,
    val email: String?,
    val profilePic: String?,
)
