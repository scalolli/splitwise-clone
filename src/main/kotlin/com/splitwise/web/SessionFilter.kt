package com.splitwise.web

import com.splitwise.domain.UserId
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie

fun Request.sessionUserId(sessionToken: SessionToken): UserId? =
    cookie("session")?.value?.let { sessionToken.verify(it) }

object SessionFilter {

    fun protect(sessionToken: SessionToken): Filter = Filter { next: HttpHandler ->
        { request: Request ->
            if (request.sessionUserId(sessionToken) != null) {
                next(request)
            } else {
                Response(Status.FOUND).header("Location", "/login")
            }
        }
    }
}
