package com.expensio.domain.repository

import com.expensio.domain.model.Settlement
import com.expensio.domain.model.SettlementSuggestion

interface SettlementRepository {
    suspend fun getSuggestions(groupId: String): List<SettlementSuggestion>
    suspend fun createSettlement(
        groupId: String,
        fromUserId: String?,
        fromGuestId: String?,
        toUserId: String?,
        toGuestId: String?,
        amount: Double,
    ): Settlement
    suspend fun getGroupSettlements(groupId: String): List<Settlement>
    suspend fun completeSettlement(settlementId: String): Settlement
}
