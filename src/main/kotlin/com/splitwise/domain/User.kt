package com.splitwise.domain

data class User(
    val id: UserId,
    val username: String,
    val email: String,
    val passwordHash: String,
)
