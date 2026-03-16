package com.splitwise.persistence

import com.splitwise.domain.User
import com.splitwise.domain.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRepositoryTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val repository = UserRepository(database)

    @Test
    fun `save user and retrieve by id`() {
        val saved = repository.save(username = "alice", email = "alice@example.com", passwordHash = "hash1")

        val found = repository.findById(saved.id)

        assertEquals(saved, found)
        assertEquals("alice", found!!.username)
        assertEquals("alice@example.com", found.email)
    }

    @Test
    fun `find by username returns user when it exists`() {
        val saved = repository.save(username = "bob", email = "bob@example.com", passwordHash = "hash2")

        val found = repository.findByUsername("bob")

        assertEquals(saved, found)
    }

    @Test
    fun `find by username returns null when it does not exist`() {
        val found = repository.findByUsername("nobody")

        assertNull(found)
    }

    @Test
    fun `find by email returns user when it exists`() {
        val saved = repository.save(username = "carol", email = "carol@example.com", passwordHash = "hash3")

        val found = repository.findByEmail("carol@example.com")

        assertEquals(saved, found)
    }

    @Test
    fun `find by email returns null when it does not exist`() {
        val found = repository.findByEmail("ghost@example.com")

        assertNull(found)
    }

    @Test
    fun `duplicate username throws exception`() {
        repository.save(username = "dave", email = "dave@example.com", passwordHash = "hash4")

        assertThrows<Exception> {
            repository.save(username = "dave", email = "dave2@example.com", passwordHash = "hash5")
        }
    }
}
