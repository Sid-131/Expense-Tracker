package com.expensio.data.remote.api

import com.expensio.data.remote.dto.AddMemberRequest
import com.expensio.data.remote.dto.GroupCreateRequest
import com.expensio.data.remote.dto.GroupDetailResponseDto
import com.expensio.data.remote.dto.GroupResponseDto
import com.expensio.data.remote.dto.MemberResponseDto
import com.expensio.data.remote.dto.UserSearchResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApi {

    @POST("api/v1/groups")
    suspend fun createGroup(@Body request: GroupCreateRequest): GroupResponseDto

    @GET("api/v1/groups")
    suspend fun getGroups(): List<GroupResponseDto>

    @GET("api/v1/groups/{id}")
    suspend fun getGroupDetail(@Path("id") id: String): GroupDetailResponseDto

    @POST("api/v1/groups/{id}/members")
    suspend fun addMember(
        @Path("id") id: String,
        @Body request: AddMemberRequest,
    ): MemberResponseDto

    @DELETE("api/v1/groups/{id}/members/{memberId}")
    suspend fun removeMember(
        @Path("id") id: String,
        @Path("memberId") memberId: String,
    ): Response<Unit>

    @GET("api/v1/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserSearchResponseDto>
}
