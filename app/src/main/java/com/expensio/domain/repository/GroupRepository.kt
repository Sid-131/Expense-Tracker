package com.expensio.domain.repository

import com.expensio.domain.model.Group
import com.expensio.domain.model.GroupDetail
import com.expensio.domain.model.GroupMember
import com.expensio.domain.model.UserSearchResult

interface GroupRepository {
    suspend fun createGroup(name: String): Result<Group>
    suspend fun getGroups(): Result<List<Group>>
    suspend fun getGroupDetail(id: String): Result<GroupDetail>
    suspend fun addMemberByEmail(groupId: String, email: String): Result<GroupMember>
    suspend fun addGuestMember(groupId: String, guestName: String): Result<GroupMember>
    suspend fun removeMember(groupId: String, memberId: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>>
}
