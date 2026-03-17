package com.splitwise.web

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import com.splitwise.persistence.GroupRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
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
    private val app = buildApp(userRepository, groupRepository)

    private fun formRequest(method: org.http4k.core.Method, path: String, vararg pairs: Pair<String, String>) =
        Request(method, path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(pairs.joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            })

    private fun loginSession(username: String, email: String): String {
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
}
