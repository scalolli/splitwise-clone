package com.splitwise.web

import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.UserService
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes

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

    return routes(
        "/health" bind GET to healthHandler,
        authHandler(userService, sessionToken),
        sessionFilter.then(
            routes(
                "/group/create" bind GET to { Response(Status.OK).body("Create Group") },
                mainHandler(groupRepository, sessionToken),
                groupHandler(groupRepository, userRepository, expenseRepository, settlementRepository, sessionToken),
            )
        ),
    )
}
