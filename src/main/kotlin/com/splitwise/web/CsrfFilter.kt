package com.splitwise.web

import org.http4k.core.Filter
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status

/**
 * CSRF protection filter.
 *
 * Applied to any route that processes POST requests. Passes GET requests through
 * unchanged. For POST requests, validates that the `_csrf` form field matches the
 * `csrf` cookie using constant-time comparison (see CsrfToken). Returns HTTP 403
 * if the token is absent or does not match.
 */
val csrfFilter = Filter { next ->
    { request ->
        if (request.method == POST && !CsrfToken.isValid(request)) {
            Response(Status.FORBIDDEN).body("CSRF token invalid or missing")
        } else {
            next(request)
        }
    }
}
