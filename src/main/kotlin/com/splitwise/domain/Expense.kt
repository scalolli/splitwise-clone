package com.splitwise.domain

import java.time.Instant

data class Expense(
    val id: ExpenseId,
    val groupId: GroupId,
    val description: String,
    val amount: Money,
    val payerId: UserId,
    val shares: List<ExpenseShare>,
    val incurredAt: Instant,
)
