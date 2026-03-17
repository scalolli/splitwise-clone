package com.splitwise.web

import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

data class IndexViewModel(
    val groups: List<Map<String, Any?>>,
    val users: List<Map<String, Any?>>,
) : ViewModel {
    override fun template() = "index"
}

fun mainHandler(userRepository: UserRepository, groupRepository: GroupRepository): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    return routes(
        "/" bind GET to {
            val groups = groupRepository.findAll().map { g ->
                mapOf("id" to g.id.value, "name" to g.name)
            }
            val users = userRepository.findAll().map { u ->
                mapOf("id" to u.id.value, "username" to u.username)
            }
            Response(Status.OK).with(htmlLens of IndexViewModel(groups = groups, users = users))
        },
    )
}
