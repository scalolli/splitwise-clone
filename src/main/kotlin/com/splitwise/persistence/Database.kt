package com.splitwise.persistence

import java.sql.Connection
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

class Database private constructor(
    val exposed: ExposedDatabase,
    private val flyway: Flyway,
) {
    fun migrate(): MigrateResult = flyway.migrate()

    fun <T> transaction(block: (Connection) -> T): T = transaction(exposed) {
        block(TransactionManager.current().connection.connection as Connection)
    }

    companion object {
        fun connect(config: DatabaseConfig): Database {
            val exposed = ExposedDatabase.connect(
                url = config.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = config.username,
                password = config.password,
            )
            val flyway = Flyway.configure()
                .dataSource(config.jdbcUrl, config.username, config.password)
                .locations("classpath:db/migration")
                .load()

            return Database(exposed = exposed, flyway = flyway)
        }
    }
}
