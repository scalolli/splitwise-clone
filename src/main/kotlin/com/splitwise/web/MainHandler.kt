package com.splitwise.web

import com.splitwise.domain.UserId
import com.splitwise.persistence.GroupRepository
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.core.with
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

data class IndexViewModel(
    val groups: List<Map<String, Any?>>,
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "index"
}

fun mainHandler(groupRepository: GroupRepository, sessionToken: SessionToken): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    return routes(
        "/" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")

            val groups = groupRepository.findByMember(UserId(currentUserId.value)).map { g ->
                mapOf("id" to g.id.value, "name" to g.name)
            }
            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(Cookie(
                    name = "csrf", value = nonce, maxAge = 3600, path = "/",
                    httpOnly = true, secure = true, sameSite = SameSite.Strict,
                ))
                .with(htmlLens of IndexViewModel(groups = groups, csrfToken = nonce))
        },
    )
}
