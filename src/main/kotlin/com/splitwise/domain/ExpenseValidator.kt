package com.splitwise.domain

import java.math.BigDecimal

sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<String>) : ValidationResult
}

object ExpenseValidator {
    private val tolerance = BigDecimal("0.01")

    fun validate(
        description: String,
        amount: Money,
        payerId: UserId,
        splits: List<ExpenseShare>,
        memberIds: List<UserId>,
    ): ValidationResult {
        val errors = buildList {
            if (description.isBlank()) {
                add("Description is required")
            }

            if (amount == Money("0.00")) {
                add("Amount must be greater than zero")
            }

            if (payerId !in memberIds) {
                add("Payer is not a member of this group")
            }

            if (splits.any { it.userId !in memberIds }) {
                add("Split user is not a member of this group")
            }

            if (splits.map { it.userId }.distinct().size != splits.size) {
                add("Duplicate users found in the expense splits")
            }

            if (splits.none { it.userId == payerId }) {
                add("The payer must be included in the expense splits")
            }

            val splitTotal = splits.fold(BigDecimal.ZERO) { total, split -> total + split.amount.value }
            if (splitTotal.subtract(amount.value).abs() > tolerance) {
                add("The sum of all splits must equal the total amount. Current total: ${formatCurrency(splitTotal)}")
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    private fun formatCurrency(amount: BigDecimal): String = "\u00A3${amount.toPlainString()}"
}
