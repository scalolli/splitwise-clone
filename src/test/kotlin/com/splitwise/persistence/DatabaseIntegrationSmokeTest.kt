package com.splitwise.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseIntegrationSmokeTest {
    @Test
    fun `app can connect to a containerized PostgreSQL instance`() {
        val config = PostgresTestSupport.freshConfig()

        val database = Database.connect(config)

        val currentDatabase = database.transaction { connection ->
            connection.metaData.databaseProductName
        }

        assertEquals("PostgreSQL", currentDatabase)
    }

    @Test
    fun `Flyway initializes a fresh PostgreSQL database and remains idempotent`() {
        val config = PostgresTestSupport.freshConfig()
        val database = Database.connect(config)

        val firstMigrationResult = database.migrate()
        val secondMigrationResult = database.migrate()
        val appliedVersion = database.transaction { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1").use { resultSet ->
                    if (resultSet.next()) resultSet.getString("version") else null
                }
            }
        }
        val usersTableExists = database.transaction { connection ->
            connection.metaData.getTables(null, null, "users", null).use { resultSet -> resultSet.next() }
        }

        assertEquals(1, firstMigrationResult.migrationsExecuted)
        assertEquals(0, secondMigrationResult.migrationsExecuted)
        assertEquals("1", appliedVersion)
        assertTrue(usersTableExists)
        assertNotNull(database.exposed)
    }
}
