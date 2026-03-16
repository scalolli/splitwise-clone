package com.splitwise

import com.splitwise.web.splitwiseApp
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    splitwiseApp.asServer(Jetty(port)).start().block()
}
