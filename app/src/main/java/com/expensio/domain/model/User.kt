package com.expensio.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profilePic: String,
    val createdAt: Long
)
