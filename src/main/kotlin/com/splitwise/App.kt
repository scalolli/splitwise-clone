package com.splitwise

import com.splitwise.persistence.Database
import com.splitwise.persistence.DatabaseConfig
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.web.buildApp
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val config = DatabaseConfig(
        jdbcUrl   = System.getenv("JDBC_URL")    ?: error("JDBC_URL environment variable must be set"),
        username  = System.getenv("DB_USERNAME") ?: error("DB_USERNAME environment variable must be set"),
        password  = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD environment variable must be set"),
    )

    val sessionSecret = System.getenv("SESSION_SECRET")
        ?: error("SESSION_SECRET environment variable must be set")

    val database = Database.connect(config)
    database.migrate()

    val userRepository = UserRepository(database)
    val groupRepository = GroupRepository(database)

    val app = buildApp(userRepository, groupRepository, sessionSecret)
    app.asServer(Jetty(port)).start().block()
}
