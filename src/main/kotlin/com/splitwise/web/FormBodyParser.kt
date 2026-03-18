package com.splitwise.web

import java.net.URLDecoder

/**
 * Parses a URL-encoded form body into a map of field name → list of values.
 * Handles repeated keys (e.g. split_user_id=1&split_user_id=2).
 */
fun parseFormBody(body: String): Map<String, List<String>> {
    if (body.isBlank()) return emptyMap()
    return body.split("&").fold(mutableMapOf<String, MutableList<String>>()) { acc, param ->
        val eq = param.indexOf('=')
        if (eq >= 0) {
            val key = URLDecoder.decode(param.substring(0, eq), "UTF-8")
            val value = URLDecoder.decode(param.substring(eq + 1), "UTF-8")
            acc.getOrPut(key) { mutableListOf() }.add(value)
        }
        acc
    }
}
