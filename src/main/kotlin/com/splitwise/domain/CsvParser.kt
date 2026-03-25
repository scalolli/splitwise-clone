package com.splitwise.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class CsvImportRow(
    val rowIndex: Int,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
)

sealed class CsvParseResult {
    data class Success(val rows: List<CsvImportRow>) : CsvParseResult()
    data class Failure(val errors: List<String>) : CsvParseResult()
}

object CsvParser {

    fun parse(content: String): CsvParseResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return CsvParseResult.Failure(listOf("The file contains no rows."))
        }

        val errors = mutableListOf<String>()
        val rows = mutableListOf<CsvImportRow>()

        lines.forEachIndexed { index, line ->
            val parts = line.split(",", limit = 3)
            if (parts.size < 3) {
                errors += "Row ${index + 1}: expected 3 columns (date,description,amount) but found ${parts.size}."
                return@forEachIndexed
            }

            val rawDate = parts[0].trim()
            val rawDescription = parts[1].trim()
            val rawAmount = parts[2].trim()

            val rowErrors = mutableListOf<String>()

            val date = try {
                LocalDate.parse(rawDate)
            } catch (e: DateTimeParseException) {
                rowErrors += "Row ${index + 1}: invalid date '$rawDate' — expected YYYY-MM-DD."
                null
            }

            if (rawDescription.isEmpty()) {
                rowErrors += "Row ${index + 1}: description must not be empty."
            }

            val amount = rawAmount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
                ?: run {
                    rowErrors += "Row ${index + 1}: invalid amount '$rawAmount' — expected a positive number."
                    null
                }

            if (rowErrors.isEmpty() && date != null && amount != null) {
                rows += CsvImportRow(
                    rowIndex = index,
                    date = date,
                    description = rawDescription,
                    amount = amount,
                )
            } else {
                errors += rowErrors
            }
        }

        return if (errors.isEmpty()) CsvParseResult.Success(rows) else CsvParseResult.Failure(errors)
    }
}
