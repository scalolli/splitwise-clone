package com.splitwise.web

import java.math.BigDecimal

fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "USD" -> "$"
    "EUR" -> "€"
    "JPY" -> "¥"
    "AUD" -> "A$"
    "CAD" -> "C$"
    "CHF" -> "Fr"
    "INR" -> "₹"
    else -> currency
}

fun formatMoney(amount: BigDecimal, currency: String): String =
    "${currencySymbol(currency)}${amount.toPlainString()}"
