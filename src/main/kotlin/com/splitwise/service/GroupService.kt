package com.splitwise.service

import com.splitwise.domain.Group
import com.splitwise.domain.GroupId
import com.splitwise.domain.UserId
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository

class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) {

    fun createGroup(name: String, creatorId: UserId): Group =
        groupRepository.create(name = name, description = null, creatorId = creatorId)

    fun editGroup(id: GroupId, name: String, requesterId: UserId): Result<Group> {
        val group = groupRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Group not found"))
        if (group.creatorId != requesterId)
            return Result.failure(SecurityException("You do not have permission to edit this group"))
        groupRepository.update(id, name)
        return Result.success(groupRepository.findById(id)!!)
    }

    fun addMember(id: GroupId, username: String, requesterId: UserId): Result<Unit> {
        val group = groupRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Group not found"))
        if (group.creatorId != requesterId)
            return Result.failure(SecurityException("You do not have permission to add members to this group"))
        val user = userRepository.findByUsername(username)
            ?: return Result.failure(IllegalArgumentException("User not found"))
        if (group.memberIds.any { it == user.id })
            return Result.failure(IllegalStateException("User is already a member of this group"))
        groupRepository.addMember(id, user.id)
        return Result.success(Unit)
    }

    fun removeMember(id: GroupId, targetUserId: UserId, requesterId: UserId): Result<Unit> {
        val group = groupRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Group not found"))
        if (group.creatorId != requesterId)
            return Result.failure(SecurityException("You do not have permission to remove members from this group"))
        if (group.creatorId == targetUserId)
            return Result.failure(IllegalArgumentException("Cannot remove the group creator"))
        groupRepository.removeMember(id, targetUserId)
        return Result.success(Unit)
    }
}
