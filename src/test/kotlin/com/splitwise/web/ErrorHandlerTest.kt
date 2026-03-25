package com.splitwise.web

import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-error-handler-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    @Test
    fun `GET unknown route returns 404 HTML page`() {
        val response = app(Request(GET, "/nonexistent-route-xyz"))

        assertEquals(404, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("404") || body.contains("Not Found"), "Expected 404 page, got: $body")
        assertTrue(body.contains("<html") || body.contains("<!DOCTYPE"), "Expected HTML response, got: $body")
    }

    @Test
    fun `GET group as non-member returns 403 HTML page`() {
        val ownerSession = TestHelpers.registerAndLogin(app, "owner2", "owner2@example.com")
        val strangerSession = TestHelpers.registerAndLogin(app, "stranger2", "stranger2@example.com")
        val owner = userRepository.findByUsername("owner2")!!
        val group = groupRepository.create("Private Group", null, owner.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", strangerSession))

        assertEquals(403, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("403") || body.contains("Forbidden"), "Expected 403 page, got: $body")
        assertTrue(body.contains("<html") || body.contains("<!DOCTYPE"), "Expected HTML response, got: $body")
    }

    @Test
    fun `error page does not expose stack traces`() {
        val session = TestHelpers.registerAndLogin(app, "alice2", "alice2@example.com")

        val response = app(Request(GET, "/group/99999").cookie("session", session))

        val body = response.bodyString()
        assertFalse(body.contains("at com.splitwise"), "Stack trace must not appear in error page body")
        assertFalse(body.contains("Exception"), "Exception class name must not appear in error page body")
    }
}
