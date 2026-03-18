package com.splitwise.service

import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.Settlement
import com.splitwise.domain.UserId
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.SettlementRepository
import java.math.BigDecimal

class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val groupRepository: GroupRepository,
) {
    fun record(
        groupId: GroupId,
        fromUserId: UserId,
        toUserId: UserId,
        amount: Money,
    ): Result<Settlement> {
        if (amount.value <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (fromUserId == toUserId) {
            return Result.failure(IllegalArgumentException("Cannot settle with yourself"))
        }
        val group = groupRepository.findById(groupId)
            ?: return Result.failure(IllegalArgumentException("Group not found"))
        if (group.memberIds.none { it == fromUserId } || group.memberIds.none { it == toUserId }) {
            return Result.failure(IllegalArgumentException("Both users must be group members"))
        }
        return Result.success(settlementRepository.create(groupId, fromUserId, toUserId, amount))
    }

    fun forGroup(groupId: GroupId): List<Settlement> =
        settlementRepository.findByGroup(groupId).sortedByDescending { it.recordedAt }
}
