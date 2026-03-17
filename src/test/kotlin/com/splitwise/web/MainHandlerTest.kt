package com.splitwise.web

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.SettlementRepository
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val app = buildApp(userRepository, groupRepository, expenseRepository, settlementRepository)

    private fun loginSession(username: String, email: String): String =
        TestHelpers.registerAndLogin(app, username, email)

    @Test
    fun `GET home without session redirects to login`() {
        val response = app(Request(GET, "/"))
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET home with valid session returns 200`() {
        val session = loginSession("homer", "homer@example.com")
        val response = app(Request(GET, "/").cookie("session", session))
        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET home lists group names`() {
        val session = loginSession("homer2", "homer2@example.com")
        val creator = userRepository.findByUsername("homer2")!!
        groupRepository.create("Test Group Alpha", null, creator.id)

        val response = app(Request(GET, "/").cookie("session", session))
        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("Test Group Alpha"),
            "Expected group name in home page body")
    }

    @Test
    fun `GET home shows only groups the current user belongs to`() {
        val aliceSession = loginSession("alice_home", "alice_home@example.com")
        loginSession("bob_home", "bob_home@example.com")
        val alice = userRepository.findByUsername("alice_home")!!
        val bob = userRepository.findByUsername("bob_home")!!

        groupRepository.create("Alice Group", null, alice.id)
        groupRepository.create("Bob Private Group", null, bob.id)

        val response = app(Request(GET, "/").cookie("session", aliceSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("Alice Group"), "Expected Alice's group in home page body")
        assertTrue(!body.contains("Bob Private Group"), "Did not expect Bob's group in Alice's home page body")
    }

    @Test
    fun `GET home does not list all registered users`() {
        val session = loginSession("viewer_home", "viewer_home@example.com")
        loginSession("hidden_user", "hidden_user@example.com")

        val response = app(Request(GET, "/").cookie("session", session))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(!body.contains("hidden_user"), "Did not expect global user list on home page")
        assertTrue(!body.contains("<h2>Users</h2>"), "Did not expect Users section on home page")
    }
}
