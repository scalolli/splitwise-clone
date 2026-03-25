package com.splitwise.persistence

import com.splitwise.domain.CsvImportRow
import com.splitwise.domain.UserId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class StagedImportRow(
    val rowIndex: Int,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val payerId: UserId?,
)

class CsvImportRepository(private val database: Database) {

    fun saveRows(importId: UUID, rows: List<CsvImportRow>) {
        transaction(database.exposed) {
            rows.forEach { row ->
                CsvImportRowsTable.insert {
                    it[CsvImportRowsTable.importId] = importId
                    it[CsvImportRowsTable.rowIndex] = row.rowIndex
                    it[CsvImportRowsTable.date] = row.date
                    it[CsvImportRowsTable.description] = row.description
                    it[CsvImportRowsTable.amount] = row.amount
                }
            }
        }
    }

    fun findRows(importId: UUID): List<StagedImportRow> = transaction(database.exposed) {
        CsvImportRowsTable
            .selectAll()
            .where { CsvImportRowsTable.importId eq importId }
            .orderBy(CsvImportRowsTable.rowIndex)
            .map { row ->
                StagedImportRow(
                    rowIndex = row[CsvImportRowsTable.rowIndex],
                    date = row[CsvImportRowsTable.date],
                    description = row[CsvImportRowsTable.description],
                    amount = row[CsvImportRowsTable.amount],
                    payerId = row[CsvImportRowsTable.payerId]?.let { UserId(it) },
                )
            }
    }

    fun deleteRows(importId: UUID) {
        transaction(database.exposed) {
            CsvImportRowsTable.deleteWhere { CsvImportRowsTable.importId eq importId }
        }
    }
}
