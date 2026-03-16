package com.splitwise.persistence

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}

object GroupsTable : Table("groups") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val creatorId = long("creator_id").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object GroupMembersTable : Table("group_members") {
    val groupId = long("group_id").references(GroupsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = long("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(groupId, userId)
}

object ExpensesTable : Table("expenses") {
    val id = long("id").autoIncrement()
    val groupId = long("group_id").references(GroupsTable.id, onDelete = ReferenceOption.CASCADE)
    val description = varchar("description", 255)
    val amount = decimal("amount", 10, 2)
    val payerId = long("payer_id").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val incurredAt = timestampWithTimeZone("incurred_at")

    override val primaryKey = PrimaryKey(id)
}

object ExpenseSharesTable : Table("expense_shares") {
    val expenseId = long("expense_id").references(ExpensesTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = long("user_id").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val amount = decimal("amount", 10, 2)

    override val primaryKey = PrimaryKey(expenseId, userId)
}

object SettlementsTable : Table("settlements") {
    val id = long("id").autoIncrement()
    val groupId = long("group_id").references(GroupsTable.id, onDelete = ReferenceOption.CASCADE)
    val fromUserId = long("from_user_id").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val toUserId = long("to_user_id").references(UsersTable.id, onDelete = ReferenceOption.RESTRICT)
    val amount = decimal("amount", 10, 2)
    val recordedAt = timestampWithTimeZone("recorded_at")

    override val primaryKey = PrimaryKey(id)
}
