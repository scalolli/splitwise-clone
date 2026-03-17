package com.splitwise.web

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies

/**
 * Shared test utilities for auth flows that require CSRF tokens.
 */
object TestHelpers {

    fun formBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }

    /**
     * Fetch a fresh CSRF nonce from a GET page.
     * Returns (cookieValue, formFieldValue).
     */
    fun getCsrfToken(app: HttpHandler, path: String): Pair<String, String> {
        val getResponse = app(Request(GET, path))
        val csrfCookie = getResponse.cookies().find { it.name == "csrf" }
            ?: error("No csrf cookie on GET $path")
        val body = getResponse.bodyString()
        val tokenInForm = Regex("""name="_csrf"\s+value="([^"]+)"""")
            .find(body)?.groupValues?.get(1)
            ?: error("No _csrf hidden field in GET $path response body")
        return Pair(csrfCookie.value, tokenInForm)
    }

    /** Register a user via the full CSRF flow. */
    fun registerUser(app: HttpHandler, username: String, email: String, password: String = "secret123") {
        val (csrfCookieValue, csrfFormValue) = getCsrfToken(app, "/register")
        app(
            Request(POST, "/register")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(formBody(
                    "username" to username,
                    "email" to email,
                    "password" to password,
                    "confirm_password" to password,
                    "_csrf" to csrfFormValue,
                ))
        )
    }

    /**
     * Login a user via the full CSRF flow.
     * Returns the session cookie value.
     */
    fun loginUser(app: HttpHandler, username: String, password: String = "secret123"): String {
        val (csrfCookieValue, csrfFormValue) = getCsrfToken(app, "/login")
        val response = app(
            Request(POST, "/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(formBody(
                    "username" to username,
                    "password" to password,
                    "_csrf" to csrfFormValue,
                ))
        )
        return response.cookies().find { it.name == "session" }?.value
            ?: error("No session cookie after login for $username")
    }

    /** Register + login in one call. Returns session cookie value. */
    fun registerAndLogin(
        app: HttpHandler,
        username: String,
        email: String,
        password: String = "secret123",
    ): String {
        registerUser(app, username, email, password)
        return loginUser(app, username, password)
    }
}
