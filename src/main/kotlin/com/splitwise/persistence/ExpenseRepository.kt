package com.splitwise.persistence

import com.splitwise.domain.Expense
import com.splitwise.domain.ExpenseId
import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExpenseRepository(private val database: Database) {

    fun create(
        groupId: GroupId,
        description: String,
        amount: Money,
        payerId: UserId,
        shares: List<ExpenseShare>,
    ): Expense =
        transaction(database.exposed) {
            val now = OffsetDateTime.now()
            val expenseId = ExpensesTable.insert {
                it[ExpensesTable.groupId] = groupId.value
                it[ExpensesTable.description] = description
                it[ExpensesTable.amount] = amount.value
                it[ExpensesTable.payerId] = payerId.value
                it[ExpensesTable.incurredAt] = now
            } get ExpensesTable.id

            ExpenseSharesTable.batchInsert(shares) { share ->
                this[ExpenseSharesTable.expenseId] = expenseId
                this[ExpenseSharesTable.userId] = share.userId.value
                this[ExpenseSharesTable.amount] = share.amount.value
            }

            Expense(
                id = ExpenseId(expenseId),
                groupId = groupId,
                description = description,
                amount = amount,
                payerId = payerId,
                shares = shares,
                incurredAt = now.toInstant(),
            )
        }

    fun findById(id: ExpenseId): Expense? =
        transaction(database.exposed) {
            val row = ExpensesTable.selectAll().where(ExpensesTable.id eq id.value).singleOrNull()
                ?: return@transaction null

            val shares = sharesFor(id)

            Expense(
                id = ExpenseId(row[ExpensesTable.id]),
                groupId = GroupId(row[ExpensesTable.groupId]),
                description = row[ExpensesTable.description],
                amount = Money.from(row[ExpensesTable.amount]),
                payerId = UserId(row[ExpensesTable.payerId]),
                shares = shares,
                incurredAt = row[ExpensesTable.incurredAt].toInstant(),
            )
        }

    fun findByGroup(groupId: GroupId): List<Expense> =
        transaction(database.exposed) {
            val rows = ExpensesTable.selectAll().where(ExpensesTable.groupId eq groupId.value).toList()
            val expenseIds = rows.map { it[ExpensesTable.id] }.toSet()

            val allShares = ExpenseSharesTable.selectAll()
                .where(ExpenseSharesTable.expenseId inList expenseIds)
                .toList()

            rows.map { row ->
                val eid = row[ExpensesTable.id]
                val shares = allShares
                    .filter { it[ExpenseSharesTable.expenseId] == eid }
                    .map { ExpenseShare(UserId(it[ExpenseSharesTable.userId]), Money.from(it[ExpenseSharesTable.amount])) }

                Expense(
                    id = ExpenseId(eid),
                    groupId = GroupId(row[ExpensesTable.groupId]),
                    description = row[ExpensesTable.description],
                    amount = Money.from(row[ExpensesTable.amount]),
                    payerId = UserId(row[ExpensesTable.payerId]),
                    shares = shares,
                    incurredAt = row[ExpensesTable.incurredAt].toInstant(),
                )
            }
        }

    fun update(
        id: ExpenseId,
        description: String,
        amount: Money,
        payerId: UserId,
        shares: List<ExpenseShare>,
    ) {
        transaction(database.exposed) {
            ExpensesTable.update({ ExpensesTable.id eq id.value }) {
                it[ExpensesTable.description] = description
                it[ExpensesTable.amount] = amount.value
                it[ExpensesTable.payerId] = payerId.value
            }

            ExpenseSharesTable.deleteWhere { ExpenseSharesTable.expenseId eq id.value }

            ExpenseSharesTable.batchInsert(shares) { share ->
                this[ExpenseSharesTable.expenseId] = id.value
                this[ExpenseSharesTable.userId] = share.userId.value
                this[ExpenseSharesTable.amount] = share.amount.value
            }
        }
    }

    fun delete(id: ExpenseId) {
        transaction(database.exposed) {
            // shares cascade-delete via FK
            ExpensesTable.deleteWhere { ExpensesTable.id eq id.value }
        }
    }

    private fun sharesFor(id: ExpenseId): List<ExpenseShare> =
        ExpenseSharesTable.selectAll().where(ExpenseSharesTable.expenseId eq id.value)
            .map { ExpenseShare(UserId(it[ExpenseSharesTable.userId]), Money.from(it[ExpenseSharesTable.amount])) }
}
