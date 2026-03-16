package com.splitwise.domain

import java.math.BigDecimal

data class Balance(
    val debtorId: UserId,
    val creditorId: UserId,
    val amount: Money,
)

object BalanceCalculator {
    fun calculate(expenses: List<Expense>, settlements: List<Settlement>): List<Balance> {
        val netBalances = mutableMapOf<Pair<UserId, UserId>, BigDecimal>()

        expenses.forEach { expense ->
            expense.shares
                .filter { it.userId != expense.payerId }
                .forEach { share ->
                    applyDebt(netBalances, share.userId, expense.payerId, share.amount.value)
                }
        }

        settlements.forEach { settlement ->
            applySettlement(netBalances, settlement.fromUserId, settlement.toUserId, settlement.amount.value)
        }

        return netBalances
            .mapNotNull { (pair, amount) ->
                when {
                    amount > BigDecimal.ZERO -> Balance(pair.first, pair.second, Money.from(amount))
                    amount < BigDecimal.ZERO -> Balance(pair.second, pair.first, Money.from(amount.negate()))
                    else -> null
                }
            }
            .sortedWith(compareBy<Balance>({ it.debtorId.value }, { it.creditorId.value }))
    }

    private fun applyDebt(
        netBalances: MutableMap<Pair<UserId, UserId>, BigDecimal>,
        debtorId: UserId,
        creditorId: UserId,
        amount: BigDecimal,
    ) {
        val (pair, signedAmount) = signedPair(debtorId, creditorId, amount)
        netBalances[pair] = netBalances.getOrDefault(pair, BigDecimal.ZERO).add(signedAmount)
    }

    private fun applySettlement(
        netBalances: MutableMap<Pair<UserId, UserId>, BigDecimal>,
        fromUserId: UserId,
        toUserId: UserId,
        amount: BigDecimal,
    ) {
        val pair = orderedPair(fromUserId, toUserId)
        val current = netBalances.getOrDefault(pair, BigDecimal.ZERO)
        val adjusted = when {
            fromUserId == pair.first && current > BigDecimal.ZERO -> current.subtract(amount).coerceAtLeast(BigDecimal.ZERO)
            fromUserId == pair.first -> current.subtract(amount)
            current < BigDecimal.ZERO -> current.add(amount).coerceAtMost(BigDecimal.ZERO)
            else -> current.add(amount)
        }

        netBalances[pair] = adjusted
    }

    private fun signedPair(
        debtorId: UserId,
        creditorId: UserId,
        amount: BigDecimal,
    ): Pair<Pair<UserId, UserId>, BigDecimal> {
        val pair = orderedPair(debtorId, creditorId)
        return if (debtorId == pair.first) {
            pair to amount
        } else {
            pair to amount.negate()
        }
    }

    private fun orderedPair(first: UserId, second: UserId): Pair<UserId, UserId> =
        if (first.value <= second.value) first to second else second to first

    private fun BigDecimal.coerceAtLeast(minimum: BigDecimal): BigDecimal =
        if (this < minimum) minimum else this

    private fun BigDecimal.coerceAtMost(maximum: BigDecimal): BigDecimal =
        if (this > maximum) maximum else this
}
