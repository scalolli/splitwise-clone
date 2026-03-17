package com.splitwise.web

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.SettlementRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionFilterTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val app = buildApp(userRepository, groupRepository, expenseRepository, settlementRepository)

    private fun formRequest(method: org.http4k.core.Method, path: String, vararg pairs: Pair<String, String>) =
        Request(method, path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(pairs.joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            })

    private fun loginSession(username: String = "testuser_sf", email: String = "sf@example.com"): String {
        app(formRequest(POST, "/register",
            "username" to username, "email" to email,
            "password" to "secret123", "confirm_password" to "secret123",
        ))
        val loginResponse = app(formRequest(POST, "/login",
            "username" to username, "password" to "secret123",
        ))
        return loginResponse.cookies().find { it.name == "session" }!!.value
    }

    @Test
    fun `protected route without session redirects to login`() {
        val response = app(Request(GET, "/group/create"))
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `protected route with valid session returns 200`() {
        val session = loginSession()
        val response = app(Request(GET, "/group/create").cookie("session", session))
        assertEquals(200, response.status.code)
    }

    @Test
    fun `protected route with raw numeric user id cookie is rejected`() {
        // Forge a session by guessing a user ID — must not be accepted
        val response = app(Request(GET, "/group/create").cookie("session", "1"))
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }
}
