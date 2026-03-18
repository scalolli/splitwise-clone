package com.splitwise.web

import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateGroupHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-create-group-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    @Test
    fun `GET group create returns 200`() {
        val session = TestHelpers.registerAndLogin(app, "alice", "alice@example.com")

        val response = app(Request(GET, "/group/create").cookie("session", session))

        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET group create unauthenticated redirects to login`() {
        val response = app(Request(GET, "/group/create"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET group create embeds csrf token`() {
        val session = TestHelpers.registerAndLogin(app, "bob", "bob@example.com")

        val response = app(Request(GET, "/group/create").cookie("session", session))

        assertTrue(response.cookies().any { it.name == "csrf" }, "Expected csrf cookie on GET /group/create")
        assertTrue(response.bodyString().contains("_csrf"), "Expected _csrf hidden field in form")
    }

    @Test
    fun `POST group create with valid name redirects to new group page`() {
        val session = TestHelpers.registerAndLogin(app, "carol", "carol@example.com")
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(app, "/group/create", session)

        val response = app(
            Request(POST, "/group/create")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody("name" to "Carol's Group", "_csrf" to csrfFormValue))
        )

        assertEquals(302, response.status.code)
        assertTrue(
            response.header("Location")?.startsWith("/group/") == true,
            "Expected redirect to /group/{id}, got: ${response.header("Location")}"
        )
    }

    @Test
    fun `POST group create sets flash message 'Group created successfully'`() {
        val session = TestHelpers.registerAndLogin(app, "dave", "dave@example.com")
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(app, "/group/create", session)

        val response = app(
            Request(POST, "/group/create")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody("name" to "Dave's Group", "_csrf" to csrfFormValue))
        )

        val flashCookie = response.cookies().find { it.name == "flash" }
        assertNotNull(flashCookie, "Expected flash cookie after successful group creation")
        assertTrue(
            flashCookie.value.contains("Group created successfully"),
            "Expected flash 'Group created successfully', got: ${flashCookie.value}"
        )
    }

    @Test
    fun `POST group create makes creator a member of the new group`() {
        val session = TestHelpers.registerAndLogin(app, "erin", "erin@example.com")
        val erin = userRepository.findByUsername("erin")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(app, "/group/create", session)

        val response = app(
            Request(POST, "/group/create")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody("name" to "Erin's Group", "_csrf" to csrfFormValue))
        )

        val location = response.header("Location") ?: error("No Location header")
        val groupId = location.removePrefix("/group/").toLong()
        val group = groupRepository.findById(com.splitwise.domain.GroupId(groupId))
        assertNotNull(group, "Group should exist after creation")
        assertTrue(
            group.memberIds.any { it == erin.id },
            "Creator should be a member of the new group"
        )
    }

    @Test
    fun `POST group create with missing name returns 400 with error message`() {
        val session = TestHelpers.registerAndLogin(app, "frank", "frank@example.com")
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(app, "/group/create", session)

        val response = app(
            Request(POST, "/group/create")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody("name" to "", "_csrf" to csrfFormValue))
        )

        assertEquals(400, response.status.code)
        assertTrue(
            response.bodyString().contains("Group name is required"),
            "Expected 'Group name is required' in response body"
        )
    }

    @Test
    fun `POST group create without csrf token returns 403`() {
        val session = TestHelpers.registerAndLogin(app, "grace", "grace@example.com")

        val response = app(
            Request(POST, "/group/create")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .body(TestHelpers.formBody("name" to "Grace's Group"))
        )

        assertEquals(403, response.status.code)
    }
}
