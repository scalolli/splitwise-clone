package com.splitwise.web

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.SettlementRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
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

    @Test
    fun `GET home embeds csrf token in logout form`() {
        val session = loginSession("csrf_home", "csrf_home@example.com")
        val response = app(Request(GET, "/").cookie("session", session))
        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""name="_csrf""""),
            "Expected _csrf hidden field in logout form on home page")
    }

    @Test
    fun `POST logout from home page with csrf token from home page succeeds`() {
        val session = loginSession("logout_home", "logout_home@example.com")
        val homeResponse = app(Request(GET, "/").cookie("session", session))
        val csrfCookie = homeResponse.cookies().find { it.name == "csrf" }!!
        val csrfToken = Regex("""name="_csrf"\s+value="([^"]+)"""")
            .find(homeResponse.bodyString())!!.groupValues[1]

        val response = app(
            Request(POST, "/logout")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie(Cookie("session", session))
                .cookie(Cookie("csrf", csrfCookie.value))
                .body("_csrf=${java.net.URLEncoder.encode(csrfToken, "UTF-8")}")
        )
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET home contains link to create a new group`() {
        val session = loginSession("nav_home", "nav_home@example.com")
        val response = app(Request(GET, "/").cookie("session", session))
        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""href="/group/create""""),
            "Expected link to /group/create on home page")
    }
}
