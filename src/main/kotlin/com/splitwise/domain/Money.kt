package com.splitwise.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val value: BigDecimal) {
    init {
        require(value >= BigDecimal.ZERO) { "Money cannot be negative" }
    }

    constructor(value: String) : this(value.toBigDecimal().setScale(SCALE, ROUNDING_MODE))

    operator fun plus(other: Money): Money = Money(value.add(other.value).normalized())

    operator fun minus(other: Money): Money = Money(value.subtract(other.value).normalized())

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP

        private fun BigDecimal.normalized(): BigDecimal = setScale(SCALE, ROUNDING_MODE)
    }
}
