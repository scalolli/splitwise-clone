package com.splitwise.web

import org.http4k.core.Method.GET
import org.http4k.core.Request
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthCheckTest {
    @Test
    fun `GET health returns ok JSON`() {
        val response = splitwiseApp(Request(GET, "/health"))

        assertEquals(200, response.status.code)
        assertEquals("{\"status\":\"ok\"}", response.bodyString())
    }
}
