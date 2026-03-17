package com.splitwise.web

import com.splitwise.service.AuthResult
import com.splitwise.service.RegistrationResult
import com.splitwise.service.UserService
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.webForm
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

data class RegisterViewModel(
    val errors: List<String> = emptyList(),
    val username: String = "",
    val email: String = "",
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "register"
}

data class LoginViewModel(
    val error: String? = null,
    val username: String = "",
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "login"
}

private val usernameLens     = FormField.defaulted("username", "")
private val emailLens        = FormField.defaulted("email", "")
private val passwordLens     = FormField.defaulted("password", "")
private val confirmPassLens  = FormField.defaulted("confirm_password", "")
private val registerForm     = Body.webForm(Validator.Feedback, usernameLens, emailLens, passwordLens, confirmPassLens).toLens()

private val loginUsernameLens = FormField.defaulted("username", "")
private val loginPasswordLens = FormField.defaulted("password", "")
private val loginForm         = Body.webForm(Validator.Feedback, loginUsernameLens, loginPasswordLens).toLens()

private fun csrfCookie(nonce: String) = Cookie(
    name = "csrf",
    value = nonce,
    maxAge = 3600,
    path = "/",
    httpOnly = true,
    secure = true,
    sameSite = SameSite.Strict,
)

fun authHandler(userService: UserService, sessionToken: SessionToken): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    return routes(
        "/register" bind GET to {
            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of RegisterViewModel(csrfToken = nonce))
        },

        "/register" bind POST to { request: Request ->
            val form = registerForm(request)
            val username = usernameLens(form)
            val email = emailLens(form)
            val password = passwordLens(form)
            val confirmPassword = confirmPassLens(form)

            when (val result = userService.register(username, email, password, confirmPassword)) {
                is RegistrationResult.Success -> {
                    val flashCookie = Cookie(
                        name = "flash",
                        value = "Registration successful. Please log in.",
                        maxAge = 60,
                        path = "/",
                        httpOnly = true,
                        secure = true,
                        sameSite = SameSite.Strict,
                    )
                    Response(Status.FOUND)
                        .header("Location", "/login")
                        .cookie(flashCookie)
                }
                is RegistrationResult.Failure -> {
                    val nonce = CsrfToken.generate()
                    Response(Status.BAD_REQUEST)
                        .cookie(csrfCookie(nonce))
                        .with(
                            htmlLens of RegisterViewModel(
                                errors = result.errors,
                                username = username,
                                email = email,
                                csrfToken = nonce,
                            )
                        )
                }
            }
        },

        "/login" bind GET to { request: Request ->
            val flash = request.cookie("flash")?.value
            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of LoginViewModel(error = flash, csrfToken = nonce))
        },

        "/login" bind POST to { request: Request ->
            val form = loginForm(request)
            val username = loginUsernameLens(form)
            val password = loginPasswordLens(form)

            when (val result = userService.authenticate(username, password)) {
                is AuthResult.Success -> {
                    val sessionCookie = Cookie(
                        name = "session",
                        value = sessionToken.sign(result.user.id),
                        maxAge = 86400,
                        path = "/",
                        httpOnly = true,
                        secure = true,
                        sameSite = SameSite.Strict,
                    )
                    Response(Status.FOUND)
                        .header("Location", "/")
                        .cookie(sessionCookie)
                }
                AuthResult.InvalidCredentials -> {
                    val nonce = CsrfToken.generate()
                    Response(Status.OK)
                        .cookie(csrfCookie(nonce))
                        .with(
                            htmlLens of LoginViewModel(
                                error = "Invalid username or password",
                                username = username,
                                csrfToken = nonce,
                            )
                        )
                }
            }
        },

        "/logout" bind POST to {
            val clearedSession = Cookie(
                name = "session",
                value = "",
                maxAge = 0,
                path = "/",
                httpOnly = true,
                secure = true,
                sameSite = SameSite.Strict,
            )
            Response(Status.FOUND)
                .header("Location", "/login")
                .cookie(clearedSession)
        },
    )
}
