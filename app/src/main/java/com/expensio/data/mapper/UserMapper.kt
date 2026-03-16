package com.expensio.data.mapper

import com.expensio.data.local.entity.UserEntity
import com.expensio.data.remote.dto.UserResponse
import com.expensio.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email ?: "",
    profilePic = profilePic ?: "",
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    profilePic = profilePic,
    createdAt = createdAt,
    syncStatus = "SYNCED"
)

fun UserResponse.toDomain(): User = User(
    id = id,
    name = name,
    email = email ?: "",
    profilePic = profilePic ?: "",
    createdAt = System.currentTimeMillis()
)
