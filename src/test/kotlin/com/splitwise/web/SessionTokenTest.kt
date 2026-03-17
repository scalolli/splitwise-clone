package com.splitwise.web

import com.splitwise.domain.UserId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionTokenTest {

    private val secret = "test-secret-at-least-32-bytes-long!!"
    private val token = SessionToken(secret)

    @Test
    fun `sign and verify round-trips a user id`() {
        val userId = UserId(42)
        val signed = token.sign(userId)
        assertEquals(userId, token.verify(signed))
    }

    @Test
    fun `verify returns null for a raw numeric id without signature`() {
        assertNull(token.verify("42"))
    }

    @Test
    fun `verify returns null when signature is tampered`() {
        val userId = UserId(1)
        val signed = token.sign(userId)
        val tampered = signed.dropLast(4) + "XXXX"
        assertNull(token.verify(tampered))
    }

    @Test
    fun `verify returns null when user id is tampered`() {
        val signed = token.sign(UserId(1))
        // swap the user id portion to a different value, keep original signature
        val parts = signed.split(".")
        val tampered = "999.${parts[1]}"
        assertNull(token.verify(tampered))
    }

    @Test
    fun `verify returns null for empty string`() {
        assertNull(token.verify(""))
    }

    @Test
    fun `tokens signed with different secrets do not verify`() {
        val otherToken = SessionToken("completely-different-secret-value!!")
        val signed = token.sign(UserId(7))
        assertNull(otherToken.verify(signed))
    }
}
