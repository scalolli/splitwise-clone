package com.splitwise.persistence

import java.sql.DriverManager
import java.util.UUID
import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestSupport {
    private val container: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("splitwise")
            withUsername("splitwise")
            withPassword("splitwise")
            start()
        }
    }

    fun freshConfig(): DatabaseConfig {
        val databaseName = "test_${UUID.randomUUID().toString().replace("-", "")}"
        createDatabase(databaseName)

        return DatabaseConfig(
            jdbcUrl = "jdbc:postgresql://${container.host}:${container.firstMappedPort}/$databaseName",
            username = container.username,
            password = container.password,
        )
    }

    fun freshDatabase(): Database {
        val database = Database.connect(freshConfig())
        database.migrate()
        return database
    }
    private fun createDatabase(databaseName: String) {
        DriverManager.getConnection(adminJdbcUrl(), container.username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE DATABASE \"$databaseName\"")
            }
        }
    }

    private fun adminJdbcUrl(): String =
        "jdbc:postgresql://${container.host}:${container.firstMappedPort}/postgres"
}
