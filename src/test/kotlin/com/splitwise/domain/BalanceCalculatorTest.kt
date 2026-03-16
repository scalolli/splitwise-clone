package com.splitwise.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceCalculatorTest {
    private val groupId = GroupId(1)
    private val alice = UserId(1)
    private val bob = UserId(2)
    private val charlie = UserId(3)
    private val now = Instant.parse("2026-03-16T10:15:30Z")

    @Test
    fun `single expense creates balances for non-payers`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(
                    id = 1,
                    payerId = alice,
                    amount = "90.00",
                    shares = listOf(
                        share(alice, "30.00"),
                        share(bob, "30.00"),
                        share(charlie, "30.00"),
                    ),
                ),
            ),
            settlements = emptyList(),
        )

        assertEquals(
            listOf(
                Balance(debtorId = bob, creditorId = alice, amount = Money("30.00")),
                Balance(debtorId = charlie, creditorId = alice, amount = Money("30.00")),
            ),
            balances,
        )
    }

    @Test
    fun `reciprocal debts are netted`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(1, alice, "30.00", listOf(share(alice, "0.00"), share(bob, "30.00"))),
                expense(2, bob, "10.00", listOf(share(alice, "10.00"), share(bob, "0.00"))),
            ),
            settlements = emptyList(),
        )

        assertEquals(
            listOf(Balance(debtorId = bob, creditorId = alice, amount = Money("20.00"))),
            balances,
        )
    }

    @Test
    fun `full settlement removes the balance`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(1, alice, "30.00", listOf(share(alice, "0.00"), share(bob, "30.00"))),
            ),
            settlements = listOf(settlement(1, fromUserId = bob, toUserId = alice, amount = "30.00")),
        )

        assertEquals(emptyList(), balances)
    }

    @Test
    fun `partial settlement reduces the displayed balance`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(1, alice, "30.00", listOf(share(alice, "0.00"), share(bob, "30.00"))),
            ),
            settlements = listOf(settlement(1, fromUserId = bob, toUserId = alice, amount = "12.00")),
        )

        assertEquals(
            listOf(Balance(debtorId = bob, creditorId = alice, amount = Money("18.00"))),
            balances,
        )
    }

    @Test
    fun `zero balances are not included`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(1, alice, "10.00", listOf(share(alice, "0.00"), share(bob, "10.00"))),
                expense(2, bob, "10.00", listOf(share(alice, "10.00"), share(bob, "0.00"))),
            ),
            settlements = emptyList(),
        )

        assertEquals(emptyList(), balances)
    }

    @Test
    fun `multiple expenses with multiple payers are combined`() {
        val balances = BalanceCalculator.calculate(
            expenses = listOf(
                expense(
                    id = 1,
                    payerId = alice,
                    amount = "60.00",
                    shares = listOf(share(alice, "20.00"), share(bob, "20.00"), share(charlie, "20.00")),
                ),
                expense(
                    id = 2,
                    payerId = bob,
                    amount = "45.00",
                    shares = listOf(share(alice, "15.00"), share(bob, "15.00"), share(charlie, "15.00")),
                ),
            ),
            settlements = emptyList(),
        )

        assertEquals(
            listOf(
                Balance(debtorId = bob, creditorId = alice, amount = Money("5.00")),
                Balance(debtorId = charlie, creditorId = alice, amount = Money("20.00")),
                Balance(debtorId = charlie, creditorId = bob, amount = Money("15.00")),
            ),
            balances,
        )
    }

    private fun expense(id: Long, payerId: UserId, amount: String, shares: List<ExpenseShare>) = Expense(
        id = ExpenseId(id),
        groupId = groupId,
        description = "Expense $id",
        amount = Money(amount),
        payerId = payerId,
        shares = shares,
        incurredAt = now,
    )

    private fun share(userId: UserId, amount: String) = ExpenseShare(
        userId = userId,
        amount = Money(amount),
    )

    private fun settlement(id: Long, fromUserId: UserId, toUserId: UserId, amount: String) = Settlement(
        id = SettlementId(id),
        groupId = groupId,
        fromUserId = fromUserId,
        toUserId = toUserId,
        amount = Money(amount),
        recordedAt = now,
    )
}
