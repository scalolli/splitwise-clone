package com.splitwise.web

import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.UserService
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

data class ErrorViewModel(
    val statusCode: Int,
    val message: String,
) : ViewModel {
    override fun template() = "error"
}

private fun statusMessage(status: Status): String = when (status.code) {
    400 -> "The request was invalid."
    403 -> "You do not have permission to view this page."
    404 -> "The page you are looking for could not be found."
    405 -> "That action is not allowed here."
    else -> "Something went wrong. Please try again later."
}

private fun errorFilter(): Filter {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    return Filter { next ->
        { request ->
            val response = try {
                next(request)
            } catch (t: Throwable) {
                Response(Status.INTERNAL_SERVER_ERROR)
            }

            val hasBody = response.body.length?.let { it > 0 } ?: true
            if (response.status.successful || response.status.redirection || hasBody) {
                response
            } else {
                val code = response.status.code
                val model = ErrorViewModel(statusCode = code, message = statusMessage(response.status))
                Response(response.status).with(htmlLens of model)
            }
        }
    }
}

fun buildApp(
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    expenseRepository: ExpenseRepository,
    settlementRepository: SettlementRepository,
    sessionSecret: String = "dev-secret-change-in-production!!",
): HttpHandler {
    val userService = UserService(userRepository)
    val sessionToken = SessionToken(sessionSecret)
    val sessionFilter = SessionFilter.protect(sessionToken)

    return errorFilter().then(
        routes(
            "/health" bind GET to healthHandler,
            "/public" bind static(ResourceLoader.Classpath("public")),
            csrfFilter.then(
                routes(
                    authHandler(userService, sessionToken),
                    sessionFilter.then(
                        routes(
                            mainHandler(groupRepository, sessionToken),
                            groupHandler(groupRepository, userRepository, expenseRepository, settlementRepository, sessionToken),
                            expenseHandler(groupRepository, userRepository, expenseRepository, sessionToken),
                        )
                    ),
                )
            ),
        )
    )
}
