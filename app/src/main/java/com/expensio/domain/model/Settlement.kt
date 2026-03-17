package com.expensio.domain.model

data class Settlement(
    val id: String,
    val groupId: String,
    val fromUserId: String?,
    val fromGuestId: String?,
    val fromName: String,
    val toUserId: String?,
    val toGuestId: String?,
    val toName: String,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val completedAt: String?,
)

data class SettlementSuggestion(
    val fromUserId: String?,
    val fromGuestId: String?,
    val fromName: String,
    val toUserId: String?,
    val toGuestId: String?,
    val toName: String,
    val amount: Double,
)
