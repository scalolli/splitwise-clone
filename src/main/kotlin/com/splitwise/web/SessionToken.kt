package com.splitwise.web

import com.splitwise.domain.UserId
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SessionToken(secret: String) {

    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    fun sign(userId: UserId): String {
        val payload = userId.value.toString()
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        val sig = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "$payload.$sig"
    }

    fun verify(token: String): UserId? {
        val dot = token.indexOf('.')
        if (dot < 0) return null
        val payload = token.substring(0, dot)
        val userId = payload.toLongOrNull() ?: return null
        val expected = sign(UserId(userId))
        if (!constantTimeEquals(token, expected)) return null
        return UserId(userId)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
