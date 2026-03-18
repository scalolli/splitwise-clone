package com.splitwise.service

import com.splitwise.domain.Expense
import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.ExpenseValidator
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import com.splitwise.domain.ValidationResult
import com.splitwise.persistence.ExpenseRepository

class ExpenseService(private val expenseRepository: ExpenseRepository) {

    /**
     * Validates and saves an expense.
     * Returns the saved [Expense] on success, or [ValidationResult.Invalid] on failure.
     */
    fun addExpense(
        groupId: GroupId,
        description: String,
        amount: Money,
        payerId: UserId,
        splits: List<ExpenseShare>,
        memberIds: List<UserId>,
    ): Result<Expense> {
        val validation = ExpenseValidator.validate(
            description = description,
            amount = amount,
            payerId = payerId,
            splits = splits,
            memberIds = memberIds,
        )

        if (validation is ValidationResult.Invalid) {
            return Result.failure(ValidationException(validation.errors))
        }

        val expense = expenseRepository.create(
            groupId = groupId,
            description = description,
            amount = amount,
            payerId = payerId,
            shares = splits,
        )
        return Result.success(expense)
    }
}

class ValidationException(val errors: List<String>) : Exception(errors.joinToString(", "))
