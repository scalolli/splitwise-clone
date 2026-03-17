package com.splitwise.service

import com.splitwise.domain.Balance
import com.splitwise.domain.BalanceCalculator
import com.splitwise.domain.GroupId
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.SettlementRepository

class BalanceService(
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
) {
    fun balancesForGroup(groupId: GroupId): List<Balance> {
        val expenses = expenseRepository.findByGroup(groupId)
        val settlements = settlementRepository.findByGroup(groupId)
        return BalanceCalculator.calculate(expenses, settlements)
    }
}
