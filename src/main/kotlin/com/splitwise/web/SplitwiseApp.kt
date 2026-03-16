package com.splitwise.web

import org.http4k.core.HttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

val splitwiseApp: HttpHandler = routes(
    "/health" bind org.http4k.core.Method.GET to healthHandler,
)
