package com.splitwise.web

import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.Money
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-group-handler-tests"
    private val sessionToken = SessionToken(sessionSecret)
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private fun formBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }

    private fun formRequest(method: org.http4k.core.Method, path: String, vararg pairs: Pair<String, String>) =
        Request(method, path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(formBody(*pairs))

    private fun registerAndLogin(username: String, email: String, password: String = "secret123"): String {
        app(formRequest(POST, "/register",
            "username" to username,
            "email" to email,
            "password" to password,
            "confirm_password" to password,
        ))
        val loginResponse = app(formRequest(POST, "/login",
            "username" to username,
            "password" to password,
        ))
        return loginResponse.cookies().find { it.name == "session" }!!.value
    }

    @Test
    fun `GET group returns 404 for non-existent group`() {
        val session = registerAndLogin("alice", "alice@example.com")

        val response = app(Request(GET, "/group/99999").cookie("session", session))

        assertEquals(404, response.status.code)
    }

    @Test
    fun `unauthenticated GET group redirects to login`() {
        val response = app(Request(GET, "/group/1"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET group returns 200 with members listed`() {
        val session = registerAndLogin("bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        val group = groupRepository.create("Bob's Group", null, bob.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("bob"), "Expected member 'bob' in response body")
    }

    @Test
    fun `GET group returns expenses listed with description amount and payer`() {
        val session = registerAndLogin("carol", "carol@example.com")
        val carol = userRepository.findByUsername("carol")!!
        val group = groupRepository.create("Carol's Group", null, carol.id)

        expenseRepository.create(
            groupId = group.id,
            description = "Dinner at restaurant",
            amount = Money("60.00"),
            payerId = carol.id,
            shares = listOf(ExpenseShare(carol.id, Money("60.00"))),
        )

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("Dinner at restaurant"), "Expected expense description in body")
        assertTrue(body.contains("60"), "Expected expense amount in body")
        assertTrue(body.contains("carol"), "Expected payer name in body")
    }

    @Test
    fun `GET group returns correct balance figures against known fixture data`() {
        val session = registerAndLogin("dave", "dave@example.com")
        registerAndLogin("erin", "erin@example.com")
        val dave = userRepository.findByUsername("dave")!!
        val erin = userRepository.findByUsername("erin")!!
        val group = groupRepository.create("Dave and Erin", null, dave.id)
        groupRepository.addMember(group.id, erin.id)

        // Dave pays 100, split equally: dave owes 0, erin owes 50
        expenseRepository.create(
            groupId = group.id,
            description = "Groceries",
            amount = Money("100.00"),
            payerId = dave.id,
            shares = listOf(
                ExpenseShare(dave.id, Money("50.00")),
                ExpenseShare(erin.id, Money("50.00")),
            ),
        )

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        // erin owes dave 50.00
        assertTrue(body.contains("50"), "Expected balance of 50 in response body, got:\n$body")
    }
}
