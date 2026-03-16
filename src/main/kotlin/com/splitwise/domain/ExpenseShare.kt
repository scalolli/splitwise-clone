package com.splitwise.domain

data class ExpenseShare(
    val userId: UserId,
    val amount: Money,
)
