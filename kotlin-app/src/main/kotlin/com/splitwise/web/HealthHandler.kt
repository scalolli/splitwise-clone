package com.splitwise.web

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

val healthHandler: HttpHandler = {
    Response(Status.OK)
        .header("Content-Type", "application/json")
        .body("{\"status\":\"ok\"}")
}
