package com.expensio.data.repository

import com.expensio.data.remote.api.SettlementApi
import com.expensio.data.remote.dto.SettlementCreateRequest
import com.expensio.domain.model.Settlement
import com.expensio.domain.model.SettlementSuggestion
import com.expensio.domain.repository.SettlementRepository
import javax.inject.Inject

class SettlementRepositoryImpl @Inject constructor(
    private val api: SettlementApi,
) : SettlementRepository {

    override suspend fun getSuggestions(groupId: String): List<SettlementSuggestion> =
        api.getSuggestions(groupId).map { dto ->
            SettlementSuggestion(
                fromUserId = dto.fromUserId,
                fromGuestId = dto.fromGuestId,
                fromName = dto.fromName,
                toUserId = dto.toUserId,
                toGuestId = dto.toGuestId,
                toName = dto.toName,
                amount = dto.amount,
            )
        }

    override suspend fun createSettlement(
        groupId: String,
        fromUserId: String?,
        fromGuestId: String?,
        toUserId: String?,
        toGuestId: String?,
        amount: Double,
    ): Settlement = api.createSettlement(
        groupId,
        SettlementCreateRequest(
            fromUserId = fromUserId,
            fromGuestId = fromGuestId,
            toUserId = toUserId,
            toGuestId = toGuestId,
            amount = amount,
        )
    ).toDomain()

    override suspend fun getGroupSettlements(groupId: String): List<Settlement> =
        api.getGroupSettlements(groupId).map { it.toDomain() }

    override suspend fun completeSettlement(settlementId: String): Settlement =
        api.completeSettlement(settlementId).toDomain()

    private fun com.expensio.data.remote.dto.SettlementResponseDto.toDomain() = Settlement(
        id = id,
        groupId = groupId,
        fromUserId = fromUserId,
        fromGuestId = fromGuestId,
        fromName = fromName,
        toUserId = toUserId,
        toGuestId = toGuestId,
        toName = toName,
        amount = amount,
        status = status,
        createdAt = createdAt,
        completedAt = completedAt,
    )
}
