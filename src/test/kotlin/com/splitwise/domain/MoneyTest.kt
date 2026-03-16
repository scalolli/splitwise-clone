package com.splitwise.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test
    fun `money with equal values is equal`() {
        assertEquals(Money("10.00"), Money("10.00"))
    }

    @Test
    fun `money adds correctly`() {
        assertEquals(Money("15.00"), Money("10.00") + Money("5.00"))
    }

    @Test
    fun `money subtracts correctly`() {
        assertEquals(Money("7.00"), Money("10.00") - Money("3.00"))
    }

    @Test
    fun `zero is valid and negative values are rejected`() {
        assertEquals(Money("0.00"), Money("0"))
        assertFailsWith<IllegalArgumentException> { Money("-1") }
    }

    @Test
    fun `money preserves two decimal places`() {
        val result = Money("10.00") - Money("3")

        assertEquals(2, result.value.scale())
        assertEquals(BigDecimal("7.00"), result.value)
    }
}
