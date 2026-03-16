package com.splitwise.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseValidatorTest {
    private val alice = UserId(1)
    private val bob = UserId(2)
    private val charlie = UserId(3)
    private val groupMembers = listOf(alice, bob)

    @Test
    fun `missing description returns an error`() {
        val result = ExpenseValidator.validate(
            description = "   ",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(alice, "10.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(listOf("Description is required")),
            result,
        )
    }

    @Test
    fun `amount must be greater than zero`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("0.00"),
            payerId = alice,
            splits = listOf(split(alice, "0.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(listOf("Amount must be greater than zero")),
            result,
        )
    }

    @Test
    fun `payer not in group returns the exact error`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = charlie,
            splits = listOf(split(alice, "10.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(
                listOf(
                    "Payer is not a member of this group",
                    "The payer must be included in the expense splits",
                ),
            ),
            result,
        )
    }

    @Test
    fun `payer must be included in splits`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(bob, "10.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(listOf("The payer must be included in the expense splits")),
            result,
        )
    }

    @Test
    fun `split user not in group returns the exact error`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(alice, "5.00"), split(charlie, "5.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(listOf("Split user is not a member of this group")),
            result,
        )
    }

    @Test
    fun `duplicate split user returns the exact error`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(alice, "5.00"), split(alice, "5.00")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(listOf("Duplicate users found in the expense splits")),
            result,
        )
    }

    @Test
    fun `split totals must match the amount within tolerance`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(alice, "4.00"), split(bob, "5.98")),
            memberIds = groupMembers,
        )

        assertEquals(
            ValidationResult.Invalid(
                listOf("The sum of all splits must equal the total amount. Current total: \u00A39.98"),
            ),
            result,
        )
    }

    @Test
    fun `valid expense returns valid`() {
        val result = ExpenseValidator.validate(
            description = "Lunch",
            amount = Money("10.00"),
            payerId = alice,
            splits = listOf(split(alice, "5.00"), split(bob, "5.00")),
            memberIds = groupMembers,
        )

        assertEquals(ValidationResult.Valid, result)
    }

    private fun split(userId: UserId, amount: String) = ExpenseShare(
        userId = userId,
        amount = Money(amount),
    )
}
