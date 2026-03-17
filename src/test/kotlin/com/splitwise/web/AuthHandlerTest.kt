package com.splitwise.web

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.service.UserService
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.toBody
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val app = buildApp(userRepository, groupRepository)

    private fun formBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }

    private fun formRequest(method: org.http4k.core.Method, path: String, vararg pairs: Pair<String, String>) =
        Request(method, path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(formBody(*pairs))

    // --- Register ---

    @Test
    fun `GET register returns 200`() {
        val response = app(Request(GET, "/register"))
        assertEquals(200, response.status.code)
    }

    @Test
    fun `POST register valid redirects to login with flash`() {
        val response = app(formRequest(POST, "/register",
            "username" to "alice",
            "email" to "alice@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
        assertTrue(response.header("Set-Cookie")?.contains("flash") == true)
    }

    @Test
    fun `POST register missing fields returns 400 with errors`() {
        val response = app(formRequest(POST, "/register",
            "username" to "",
            "email" to "",
            "password" to "",
            "confirm_password" to "",
        ))
        assertEquals(400, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("required") || body.contains("error") || body.contains("field"),
            "Expected validation errors in body but got: $body")
    }

    @Test
    fun `POST register duplicate username returns error`() {
        app(formRequest(POST, "/register",
            "username" to "bob",
            "email" to "bob@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))

        val response = app(formRequest(POST, "/register",
            "username" to "bob",
            "email" to "bob2@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Username already exists"))
    }

    @Test
    fun `POST register duplicate email returns error`() {
        app(formRequest(POST, "/register",
            "username" to "carol",
            "email" to "carol@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))

        val response = app(formRequest(POST, "/register",
            "username" to "carol2",
            "email" to "carol@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Email already exists"))
    }

    @Test
    fun `POST register passwords do not match returns error`() {
        val response = app(formRequest(POST, "/register",
            "username" to "dave",
            "email" to "dave@example.com",
            "password" to "secret123",
            "confirm_password" to "different",
        ))
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Passwords do not match"))
    }

    @Test
    fun `POST register stores BCrypt hash not plain text`() {
        app(formRequest(POST, "/register",
            "username" to "eve",
            "email" to "eve@example.com",
            "password" to "plaintext1",
            "confirm_password" to "plaintext1",
        ))

        val credentials = userRepository.findForAuth("eve")
        assertNotNull(credentials)
        assertTrue(credentials.passwordHash.startsWith("\$2"), "Expected BCrypt hash but got: ${credentials.passwordHash}")
    }

    // --- Login / logout ---

    @Test
    fun `GET login returns 200`() {
        val response = app(Request(GET, "/login"))
        assertEquals(200, response.status.code)
    }

    @Test
    fun `POST login valid sets session cookie and redirects to home`() {
        app(formRequest(POST, "/register",
            "username" to "frank",
            "email" to "frank@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))

        val response = app(formRequest(POST, "/login",
            "username" to "frank",
            "password" to "secret123",
        ))
        assertEquals(302, response.status.code)
        assertEquals("/", response.header("Location"))
        val sessionCookie = response.cookies().find { it.name == "session" }
        assertNotNull(sessionCookie, "Expected session cookie to be set")
        assertTrue(sessionCookie.secure == true, "Expected session cookie to have Secure flag")
    }

    @Test
    fun `POST login invalid credentials re-renders form with error`() {
        val response = app(formRequest(POST, "/login",
            "username" to "nobody",
            "password" to "wrong",
        ))
        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("Invalid username or password"))
    }

    @Test
    fun `POST logout clears session and redirects to home`() {
        // register + login to get a session
        app(formRequest(POST, "/register",
            "username" to "grace",
            "email" to "grace@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        ))
        val loginResponse = app(formRequest(POST, "/login",
            "username" to "grace",
            "password" to "secret123",
        ))
        val sessionCookie = loginResponse.cookies().find { it.name == "session" }!!

        val response = app(Request(POST, "/logout").cookie("session", sessionCookie.value))
        assertEquals(302, response.status.code)
        assertEquals("/", response.header("Location"))
        // session cookie should be cleared (max-age=0 or empty value)
        val setCookie = response.header("Set-Cookie") ?: ""
        assertTrue(
            setCookie.contains("session=;") || setCookie.contains("Max-Age=0") || setCookie.contains("session=\"\""),
            "Expected session to be cleared but Set-Cookie was: $setCookie"
        )
    }
}
