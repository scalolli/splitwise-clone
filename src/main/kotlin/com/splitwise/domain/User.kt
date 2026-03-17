package com.splitwise.domain

data class User(
    val id: UserId,
    val username: String,
    val email: String,
)

data class UserCredentials(
    val user: User,
    val passwordHash: String,
)
