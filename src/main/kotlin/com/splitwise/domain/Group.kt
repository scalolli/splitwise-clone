package com.splitwise.domain

import java.time.Instant

data class Group(
    val id: GroupId,
    val name: String,
    val description: String?,
    val creatorId: UserId,
    val memberIds: List<UserId>,
    val createdAt: Instant,
    val currency: String = "GBP",
)
