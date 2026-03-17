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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val app = buildApp(userRepository, groupRepository, expenseRepository, settlementRepository)

    // --- Helpers ---

    private fun formBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }

    /**
     * Fetch the CSRF nonce from a GET response: returns (cookieValue, formFieldValue).
     * Both should be identical; this helper verifies the form contains the hidden field.
     */
    private fun getCsrfToken(path: String): Pair<String, String> {
        val getResponse = app(Request(GET, path))
        val csrfCookie = getResponse.cookies().find { it.name == "csrf" }
            ?: error("No csrf cookie on GET $path")
        val body = getResponse.bodyString()
        val tokenInForm = Regex("""name="_csrf"\s+value="([^"]+)"""")
            .find(body)?.groupValues?.get(1)
            ?: error("No _csrf hidden field in GET $path response body")
        return Pair(csrfCookie.value, tokenInForm)
    }

    /**
     * POST with a valid CSRF token pair obtained from the given GET path.
     */
    private fun postWithCsrf(
        getPath: String,
        postPath: String,
        vararg extraFields: Pair<String, String>,
        extraCookies: List<Cookie> = emptyList(),
    ): org.http4k.core.Response {
        val (csrfCookieValue, csrfFormValue) = getCsrfToken(getPath)
        val allFields = (extraFields.toList() + ("_csrf" to csrfFormValue)).toTypedArray()
        var req = Request(POST, postPath)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .cookie(Cookie("csrf", csrfCookieValue))
            .body(formBody(*allFields))
        for (c in extraCookies) req = req.cookie(c)
        return app(req)
    }

    /** Register a user via the full CSRF flow. */
    private fun registerUser(username: String, email: String, password: String = "secret123") {
        postWithCsrf("/register", "/register",
            "username" to username,
            "email" to email,
            "password" to password,
            "confirm_password" to password,
        )
    }

    /** Login a user via the full CSRF flow; returns the session cookie value. */
    private fun loginUser(username: String, password: String = "secret123"): String {
        val response = postWithCsrf("/login", "/login",
            "username" to username,
            "password" to password,
        )
        return response.cookies().find { it.name == "session" }?.value
            ?: error("No session cookie after login for $username")
    }

    // --- Register ---

    @Test
    fun `GET register returns 200`() {
        val response = app(Request(GET, "/register"))
        assertEquals(200, response.status.code)
    }

    @Test
    fun `POST register valid redirects to login with flash`() {
        val response = postWithCsrf("/register", "/register",
            "username" to "alice",
            "email" to "alice@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        )
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
        assertTrue(response.header("Set-Cookie")?.contains("flash") == true)
    }

    @Test
    fun `POST register missing fields returns 400 with errors`() {
        val response = postWithCsrf("/register", "/register",
            "username" to "",
            "email" to "",
            "password" to "",
            "confirm_password" to "",
        )
        assertEquals(400, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("required") || body.contains("error") || body.contains("field"),
            "Expected validation errors in body but got: $body")
    }

    @Test
    fun `POST register duplicate username returns error`() {
        registerUser("bob", "bob@example.com")

        val response = postWithCsrf("/register", "/register",
            "username" to "bob",
            "email" to "bob2@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        )
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Username already exists"))
    }

    @Test
    fun `POST register duplicate email returns error`() {
        registerUser("carol", "carol@example.com")

        val response = postWithCsrf("/register", "/register",
            "username" to "carol2",
            "email" to "carol@example.com",
            "password" to "secret123",
            "confirm_password" to "secret123",
        )
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Email already exists"))
    }

    @Test
    fun `POST register passwords do not match returns error`() {
        val response = postWithCsrf("/register", "/register",
            "username" to "dave",
            "email" to "dave@example.com",
            "password" to "secret123",
            "confirm_password" to "different",
        )
        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Passwords do not match"))
    }

    @Test
    fun `POST register stores BCrypt hash not plain text`() {
        registerUser("eve", "eve@example.com", "plaintext1")
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
        registerUser("frank", "frank@example.com")

        val response = postWithCsrf("/login", "/login",
            "username" to "frank",
            "password" to "secret123",
        )
        assertEquals(302, response.status.code)
        assertEquals("/", response.header("Location"))
        val sessionCookie = response.cookies().find { it.name == "session" }
        assertNotNull(sessionCookie, "Expected session cookie to be set")
        assertTrue(sessionCookie.secure == true, "Expected session cookie to have Secure flag")
    }

    @Test
    fun `POST login invalid credentials re-renders form with error`() {
        val response = postWithCsrf("/login", "/login",
            "username" to "nobody",
            "password" to "wrong",
        )
        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("Invalid username or password"))
    }

    @Test
    fun `POST logout clears session and redirects to login`() {
        registerUser("grace", "grace@example.com")
        val sessionValue = loginUser("grace")

        val (csrfCookieValue, csrfFormValue) = getCsrfToken("/login") // any GET page issues a csrf cookie
        val response = app(
            Request(POST, "/logout")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie(Cookie("session", sessionValue))
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(formBody("_csrf" to csrfFormValue))
        )
        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
        val clearedCookie = response.cookies().find { it.name == "session" }
        assertNotNull(clearedCookie, "Expected cleared session cookie to be present")
        val setCookie = response.header("Set-Cookie") ?: ""
        assertTrue(
            setCookie.contains("session=;") || setCookie.contains("Max-Age=0") || setCookie.contains("session=\"\""),
            "Expected session to be cleared but Set-Cookie was: $setCookie"
        )
        assertTrue(clearedCookie.secure == true, "Expected cleared session cookie to retain Secure flag")
        assertTrue(clearedCookie.httpOnly == true, "Expected cleared session cookie to retain HttpOnly flag")
        assertTrue(clearedCookie.sameSite?.name == "Strict", "Expected cleared session cookie to retain SameSite=Strict")
    }

    // --- CSRF ---

    @Test
    fun `GET register sets csrf cookie and embeds token in form`() {
        val response = app(Request(GET, "/register"))
        assertNotNull(response.cookies().find { it.name == "csrf" }, "Expected csrf cookie on GET /register")
        assertTrue(response.bodyString().contains("""name="_csrf""""), "Expected _csrf hidden field in register form")
    }

    @Test
    fun `GET login sets csrf cookie and embeds token in form`() {
        val response = app(Request(GET, "/login"))
        assertNotNull(response.cookies().find { it.name == "csrf" }, "Expected csrf cookie on GET /login")
        assertTrue(response.bodyString().contains("""name="_csrf""""), "Expected _csrf hidden field in login form")
    }

    @Test
    fun `POST register without csrf token returns 403`() {
        val response = app(
            Request(POST, "/register")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(formBody(
                    "username" to "alice",
                    "email" to "alice@example.com",
                    "password" to "secret123",
                    "confirm_password" to "secret123",
                ))
        )
        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST login without csrf token returns 403`() {
        val response = app(
            Request(POST, "/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(formBody("username" to "frank", "password" to "secret123"))
        )
        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST logout without csrf token returns 403`() {
        val response = app(
            Request(POST, "/logout")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("")
        )
        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST register with mismatched csrf token returns 403`() {
        val (cookieValue, _) = getCsrfToken("/register")
        val response = app(
            Request(POST, "/register")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie(Cookie("csrf", cookieValue))
                .body(formBody(
                    "username" to "mallory",
                    "email" to "mallory@example.com",
                    "password" to "secret123",
                    "confirm_password" to "secret123",
                    "_csrf" to "wrong-token",
                ))
        )
        assertEquals(403, response.status.code)
    }
}
