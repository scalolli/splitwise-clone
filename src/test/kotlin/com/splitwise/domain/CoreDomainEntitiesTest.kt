package com.splitwise.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CoreDomainEntitiesTest {
    @Test
    fun `typed ids compare by value`() {
        assertEquals(UserId(1), UserId(1))
        assertNotEquals(UserId(1), UserId(2))
    }

    @Test
    fun `entities can be constructed with required fields`() {
        val createdAt = Instant.parse("2026-03-16T10:15:30Z")
        val aliceId = UserId(1)
        val bobId = UserId(2)
        val groupId = GroupId(3)
        val expenseId = ExpenseId(4)
        val settlementId = SettlementId(5)

        val alice = User(
            id = aliceId,
            username = "alice",
            email = "alice@example.com",
        )
        val bob = User(
            id = bobId,
            username = "bob",
            email = "bob@example.com",
        )
        val group = Group(
            id = groupId,
            name = "Trip",
            description = "Summer trip",
            creatorId = aliceId,
            memberIds = listOf(aliceId, bobId),
            createdAt = createdAt,
        )
        val expenseShare = ExpenseShare(
            userId = bobId,
            amount = Money("12.50"),
        )
        val expense = Expense(
            id = expenseId,
            groupId = groupId,
            description = "Dinner",
            amount = Money("25.00"),
            payerId = aliceId,
            shares = listOf(expenseShare),
            incurredAt = createdAt,
        )
        val settlement = Settlement(
            id = settlementId,
            groupId = groupId,
            fromUserId = bobId,
            toUserId = aliceId,
            amount = Money("12.50"),
            recordedAt = createdAt,
        )

        assertEquals("alice", alice.username)
        assertEquals(listOf(aliceId, bobId), group.memberIds)
        assertEquals(Money("12.50"), expenseShare.amount)
        assertEquals(listOf(expenseShare), expense.shares)
        assertEquals(aliceId, settlement.toUserId)
    }
}
