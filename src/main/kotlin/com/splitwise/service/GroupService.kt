package com.splitwise.service

import com.splitwise.domain.Group
import com.splitwise.domain.UserId
import com.splitwise.persistence.GroupRepository

class GroupService(private val groupRepository: GroupRepository) {

    fun createGroup(name: String, creatorId: UserId): Group =
        groupRepository.create(name = name, description = null, creatorId = creatorId)
}
