package com.splitwise.persistence

import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.Settlement
import com.splitwise.domain.SettlementId
import com.splitwise.domain.UserId
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SettlementRepository(private val database: Database) {

    fun create(groupId: GroupId, fromUserId: UserId, toUserId: UserId, amount: Money): Settlement =
        transaction(database.exposed) {
            val now = OffsetDateTime.now()
            val id = SettlementsTable.insert {
                it[SettlementsTable.groupId] = groupId.value
                it[SettlementsTable.fromUserId] = fromUserId.value
                it[SettlementsTable.toUserId] = toUserId.value
                it[SettlementsTable.amount] = amount.value
                it[SettlementsTable.recordedAt] = now
            } get SettlementsTable.id

            Settlement(
                id = SettlementId(id),
                groupId = groupId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                amount = amount,
                recordedAt = now.toInstant(),
            )
        }

    fun findByGroup(groupId: GroupId): List<Settlement> =
        transaction(database.exposed) {
            SettlementsTable.selectAll()
                .where(SettlementsTable.groupId eq groupId.value)
                .map { row ->
                    Settlement(
                        id = SettlementId(row[SettlementsTable.id]),
                        groupId = GroupId(row[SettlementsTable.groupId]),
                        fromUserId = UserId(row[SettlementsTable.fromUserId]),
                        toUserId = UserId(row[SettlementsTable.toUserId]),
                        amount = Money.from(row[SettlementsTable.amount]),
                        recordedAt = row[SettlementsTable.recordedAt].toInstant(),
                    )
                }
        }
}
