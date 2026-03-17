package com.expensio.data.repository

import com.expensio.data.remote.api.GroupApi
import com.expensio.data.remote.dto.AddMemberRequest
import com.expensio.data.remote.dto.GroupCreateRequest
import com.expensio.domain.model.Group
import com.expensio.domain.model.GroupDetail
import com.expensio.domain.model.GroupMember
import com.expensio.domain.model.UserSearchResult
import com.expensio.domain.repository.GroupRepository
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val api: GroupApi,
) : GroupRepository {

    override suspend fun createGroup(name: String): Result<Group> = runCatching {
        val dto = api.createGroup(GroupCreateRequest(name))
        Group(dto.id, dto.name, dto.createdBy, dto.createdAt, dto.memberCount)
    }

    override suspend fun getGroups(): Result<List<Group>> = runCatching {
        api.getGroups().map { Group(it.id, it.name, it.createdBy, it.createdAt, it.memberCount) }
    }

    override suspend fun getGroupDetail(id: String): Result<GroupDetail> = runCatching {
        val dto = api.getGroupDetail(id)
        GroupDetail(
            id = dto.id,
            name = dto.name,
            createdBy = dto.createdBy,
            createdAt = dto.createdAt,
            members = dto.members.map {
                GroupMember(it.id, it.name, it.isGuest, it.userId, it.guestId, it.profilePic)
            },
        )
    }

    override suspend fun addMemberByEmail(groupId: String, email: String): Result<GroupMember> =
        runCatching {
            val dto = api.addMember(groupId, AddMemberRequest(email = email))
            GroupMember(dto.id, dto.name, dto.isGuest, dto.userId, dto.guestId, dto.profilePic)
        }

    override suspend fun addGuestMember(groupId: String, guestName: String): Result<GroupMember> =
        runCatching {
            val dto = api.addMember(groupId, AddMemberRequest(guestName = guestName))
            GroupMember(dto.id, dto.name, dto.isGuest, dto.userId, dto.guestId, dto.profilePic)
        }

    override suspend fun removeMember(groupId: String, memberId: String): Result<Unit> =
        runCatching {
            api.removeMember(groupId, memberId)
            Unit
        }

    override suspend fun searchUsers(query: String): Result<List<UserSearchResult>> = runCatching {
        api.searchUsers(query).map { UserSearchResult(it.id, it.name, it.email, it.profilePic) }
    }
}
