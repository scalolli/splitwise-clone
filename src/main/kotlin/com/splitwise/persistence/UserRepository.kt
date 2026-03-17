package com.splitwise.persistence

import com.splitwise.domain.User
import com.splitwise.domain.UserCredentials
import com.splitwise.domain.UserId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository(private val database: Database) {

    fun save(username: String, email: String, passwordHash: String): User =
        transaction(database.exposed) {
            val id = UsersTable.insert {
                it[UsersTable.username] = username
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
            } get UsersTable.id

            User(id = UserId(id), username = username, email = email)
        }

    fun findById(id: UserId): User? =
        transaction(database.exposed) {
            UsersTable.selectAll().where(UsersTable.id eq id.value)
                .singleOrNull()
                ?.let { row ->
                    User(
                        id = UserId(row[UsersTable.id]),
                        username = row[UsersTable.username],
                        email = row[UsersTable.email],
                    )
                }
        }

    fun findByUsername(username: String): User? =
        transaction(database.exposed) {
            UsersTable.selectAll().where(UsersTable.username eq username)
                .singleOrNull()
                ?.let { row ->
                    User(
                        id = UserId(row[UsersTable.id]),
                        username = row[UsersTable.username],
                        email = row[UsersTable.email],
                    )
                }
        }

    fun findByEmail(email: String): User? =
        transaction(database.exposed) {
            UsersTable.selectAll().where(UsersTable.email eq email)
                .singleOrNull()
                ?.let { row ->
                    User(
                        id = UserId(row[UsersTable.id]),
                        username = row[UsersTable.username],
                        email = row[UsersTable.email],
                    )
                }
        }

    fun findForAuth(username: String): UserCredentials? =
        transaction(database.exposed) {
            UsersTable.selectAll().where(UsersTable.username eq username)
                .singleOrNull()
                ?.let { row ->
                    UserCredentials(
                        user = User(
                            id = UserId(row[UsersTable.id]),
                            username = row[UsersTable.username],
                            email = row[UsersTable.email],
                        ),
                        passwordHash = row[UsersTable.passwordHash],
                    )
                }
        }

    fun findAll(): List<User> =
        transaction(database.exposed) {
            UsersTable.selectAll().map { row ->
                User(
                    id = UserId(row[UsersTable.id]),
                    username = row[UsersTable.username],
                    email = row[UsersTable.email],
                )
            }
        }
}
