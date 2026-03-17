package com.splitwise.web

import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Request
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthCheckTest {
    private val database = PostgresTestSupport.freshDatabase()
    private val app = buildApp(UserRepository(database), GroupRepository(database))

    @Test
    fun `GET health returns ok JSON`() {
        val response = app(Request(GET, "/health"))

        assertEquals(200, response.status.code)
        assertEquals("{\"status\":\"ok\"}", response.bodyString())
    }
}
