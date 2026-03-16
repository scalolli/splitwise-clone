package com.splitwise.domain

import java.time.Instant

data class Settlement(
    val id: SettlementId,
    val groupId: GroupId,
    val fromUserId: UserId,
    val toUserId: UserId,
    val amount: Money,
    val recordedAt: Instant,
)
