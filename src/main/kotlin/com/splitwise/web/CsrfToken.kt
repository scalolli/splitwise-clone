package com.splitwise.web

import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import java.security.SecureRandom
import java.util.Base64

object CsrfToken {

    private val rng = SecureRandom()

    /** Generate a new URL-safe base64 nonce (32 random bytes). */
    fun generate(): String {
        val bytes = ByteArray(32)
        rng.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Validate a POST request: the `_csrf` form field must match the `csrf` cookie.
     * Returns true if valid, false if missing or mismatched.
     */
    fun isValid(request: Request): Boolean {
        val cookieValue = request.cookie("csrf")?.value ?: return false
        val formValue = extractFormField(request, "_csrf") ?: return false
        return constantTimeEquals(cookieValue, formValue)
    }

    private fun extractFormField(request: Request, name: String): String? {
        val contentType = request.header("Content-Type") ?: ""
        return if (contentType.contains("multipart/form-data")) {
            extractMultipartField(request.bodyString(), contentType, name)
        } else {
            extractUrlEncodedField(request.bodyString(), name)
        }
    }

    private fun extractUrlEncodedField(body: String, name: String): String? =
        body.split("&")
            .mapNotNull { param ->
                val eq = param.indexOf('=')
                if (eq < 0) null
                else {
                    val k = java.net.URLDecoder.decode(param.substring(0, eq), "UTF-8")
                    val v = java.net.URLDecoder.decode(param.substring(eq + 1), "UTF-8")
                    if (k == name) v else null
                }
            }
            .firstOrNull()

    private fun extractMultipartField(body: String, contentType: String, name: String): String? {
        val boundary = contentType.substringAfter("boundary=", "").trim()
        if (boundary.isEmpty()) return null
        return body.split("--$boundary")
            .drop(1)
            .firstOrNull { part ->
                part.contains("""name="$name"""")
            }
            ?.let { part ->
                val sep = "\r\n\r\n"
                val idx = part.indexOf(sep)
                if (idx == -1) null
                else part.substring(idx + sep.length).trimEnd('\r', '\n', '-')
            }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
