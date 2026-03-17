package com.splitwise.persistence

import com.splitwise.domain.Group
import com.splitwise.domain.GroupId
import com.splitwise.domain.UserId
import java.time.OffsetDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

class GroupRepository(private val database: Database) {

    fun create(name: String, description: String?, creatorId: UserId): Group =
        transaction(database.exposed) {
            val now = OffsetDateTime.now()
            val groupId = GroupsTable.insert {
                it[GroupsTable.name] = name
                it[GroupsTable.description] = description
                it[GroupsTable.creatorId] = creatorId.value
                it[GroupsTable.createdAt] = now
            } get GroupsTable.id

            GroupMembersTable.insert {
                it[GroupMembersTable.groupId] = groupId
                it[GroupMembersTable.userId] = creatorId.value
            }

            Group(
                id = GroupId(groupId),
                name = name,
                description = description,
                creatorId = creatorId,
                memberIds = listOf(creatorId),
                createdAt = now.toInstant(),
            )
        }

    fun findById(id: GroupId): Group? =
        transaction(database.exposed) {
            val row = GroupsTable.selectAll().where(GroupsTable.id eq id.value).singleOrNull()
                ?: return@transaction null

            val memberIds = GroupMembersTable.selectAll()
                .where(GroupMembersTable.groupId eq id.value)
                .map { UserId(it[GroupMembersTable.userId]) }

            Group(
                id = GroupId(row[GroupsTable.id]),
                name = row[GroupsTable.name],
                description = row[GroupsTable.description],
                creatorId = UserId(row[GroupsTable.creatorId]),
                memberIds = memberIds,
                createdAt = row[GroupsTable.createdAt].toInstant(),
            )
        }

    fun findAll(): List<Group> =
        transaction(database.exposed) {
            val allRows = GroupsTable.selectAll().toList()
            val allMembers = GroupMembersTable.selectAll().toList()

            allRows.map { row ->
                val gid = row[GroupsTable.id]
                val memberIds = allMembers
                    .filter { it[GroupMembersTable.groupId] == gid }
                    .map { UserId(it[GroupMembersTable.userId]) }

                Group(
                    id = GroupId(gid),
                    name = row[GroupsTable.name],
                    description = row[GroupsTable.description],
                    creatorId = UserId(row[GroupsTable.creatorId]),
                    memberIds = memberIds,
                    createdAt = row[GroupsTable.createdAt].toInstant(),
                )
            }
        }

    fun findByMember(userId: UserId): List<Group> =
        transaction(database.exposed) {
            val rows = GroupsTable
                .innerJoin(GroupMembersTable)
                .selectAll()
                .where(GroupMembersTable.userId eq userId.value)
                .toList()

            val groupIds = rows.map { it[GroupsTable.id] }
            val allMembers = if (groupIds.isEmpty()) {
                emptyList()
            } else {
                GroupMembersTable.selectAll()
                    .where(GroupMembersTable.groupId inList groupIds)
                    .toList()
            }

            rows.map { row ->
                val gid = row[GroupsTable.id]
                val memberIds = allMembers
                    .filter { it[GroupMembersTable.groupId] == gid }
                    .map { UserId(it[GroupMembersTable.userId]) }

                Group(
                    id = GroupId(gid),
                    name = row[GroupsTable.name],
                    description = row[GroupsTable.description],
                    creatorId = UserId(row[GroupsTable.creatorId]),
                    memberIds = memberIds,
                    createdAt = row[GroupsTable.createdAt].toInstant(),
                )
            }
        }

    fun addMember(groupId: GroupId, userId: UserId) {
        transaction(database.exposed) {
            GroupMembersTable.insertIgnore {
                it[GroupMembersTable.groupId] = groupId.value
                it[GroupMembersTable.userId] = userId.value
            }
        }
    }

    fun removeMember(groupId: GroupId, userId: UserId) {
        transaction(database.exposed) {
            GroupMembersTable.deleteWhere {
                (GroupMembersTable.groupId eq groupId.value) and
                    (GroupMembersTable.userId eq userId.value)
            }
        }
    }
}
