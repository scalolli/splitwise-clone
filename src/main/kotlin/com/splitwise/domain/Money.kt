package com.splitwise.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val value: BigDecimal) {
    init {
        require(value >= BigDecimal.ZERO) { "Money cannot be negative" }
    }

    constructor(value: String) : this(value.toBigDecimal().setScale(SCALE, ROUNDING_MODE))

    operator fun plus(other: Money): Money = from(value.add(other.value))

    operator fun minus(other: Money): Money = from(value.subtract(other.value))

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP

        fun from(value: BigDecimal): Money = Money(value.setScale(SCALE, ROUNDING_MODE))
    }
}
