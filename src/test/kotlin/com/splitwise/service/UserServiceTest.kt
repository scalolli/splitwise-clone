package com.splitwise.service

import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.UserRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserServiceTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val userService = UserService(userRepository)

    // --- Password validation ---

    @Test
    fun `register with password shorter than 8 chars returns error`() {
        val result = userService.register("alice", "alice@example.com", "short", "short")
        assertIs<RegistrationResult.Failure>(result)
        assertTrue(result.errors.any { it.contains("8") }, "Expected min-length error but got: ${result.errors}")
    }

    @Test
    fun `register with password longer than 72 chars returns error`() {
        val tooLong = "a".repeat(73)
        val result = userService.register("bob", "bob@example.com", tooLong, tooLong)
        assertIs<RegistrationResult.Failure>(result)
        assertTrue(result.errors.any { it.contains("72") }, "Expected max-length error but got: ${result.errors}")
    }

    @Test
    fun `register with password exactly 8 chars succeeds`() {
        val result = userService.register("carol", "carol@example.com", "exactly8", "exactly8")
        assertIs<RegistrationResult.Success>(result)
    }

    @Test
    fun `register with password exactly 72 chars succeeds`() {
        val maxValid = "a".repeat(72)
        val result = userService.register("dave", "dave@example.com", maxValid, maxValid)
        assertIs<RegistrationResult.Success>(result)
    }

    // --- Email validation ---

    @Test
    fun `register with invalid email returns error`() {
        val result = userService.register("eve", "notanemail", "password1", "password1")
        assertIs<RegistrationResult.Failure>(result)
        assertTrue(result.errors.any { it.contains("email", ignoreCase = true) },
            "Expected email format error but got: ${result.errors}")
    }

    @Test
    fun `register with valid email succeeds`() {
        val result = userService.register("frank", "frank@example.com", "password1", "password1")
        assertIs<RegistrationResult.Success>(result)
    }
}
