package com.splitwise.persistence

import com.splitwise.domain.UserId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupRepositoryTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)

    private fun saveUser(username: String) =
        userRepository.save(username = username, email = "$username@example.com", passwordHash = "hash")

    @Test
    fun `create group and retrieve by id`() {
        val creator = saveUser("alice")

        val group = groupRepository.create(name = "Flatmates", description = "Monthly bills", creatorId = creator.id)

        val found = groupRepository.findById(group.id)
        assertNotNull(found)
        assertEquals("Flatmates", found.name)
        assertEquals("Monthly bills", found.description)
        assertEquals(creator.id, found.creatorId)
        assertTrue(found.memberIds.contains(creator.id), "Creator should be a member")
    }

    @Test
    fun `find by id returns null for non-existent group`() {
        val found = groupRepository.findById(com.splitwise.domain.GroupId(999999L))
        assertNull(found)
    }

    @Test
    fun `add member appears in group`() {
        val creator = saveUser("bob")
        val member = saveUser("carol")
        val group = groupRepository.create(name = "Trip", description = null, creatorId = creator.id)

        groupRepository.addMember(groupId = group.id, userId = member.id)

        val found = groupRepository.findById(group.id)!!
        assertTrue(found.memberIds.contains(member.id))
    }

    @Test
    fun `remove member is no longer in group`() {
        val creator = saveUser("dave")
        val member = saveUser("eve")
        val group = groupRepository.create(name = "Dinner", description = null, creatorId = creator.id)
        groupRepository.addMember(groupId = group.id, userId = member.id)

        groupRepository.removeMember(groupId = group.id, userId = member.id)

        val found = groupRepository.findById(group.id)!!
        assertTrue(!found.memberIds.contains(member.id))
    }

    @Test
    fun `add duplicate member is idempotent`() {
        val creator = saveUser("frank")
        val member = saveUser("grace")
        val group = groupRepository.create(name = "Book Club", description = null, creatorId = creator.id)
        groupRepository.addMember(groupId = group.id, userId = member.id)

        // second add should not throw
        groupRepository.addMember(groupId = group.id, userId = member.id)

        val found = groupRepository.findById(group.id)!!
        assertEquals(1, found.memberIds.count { it == member.id })
    }

    @Test
    fun `find all groups returns all created groups`() {
        val creator = saveUser("heidi")
        groupRepository.create(name = "Group A", description = null, creatorId = creator.id)
        groupRepository.create(name = "Group B", description = null, creatorId = creator.id)

        val all = groupRepository.findAll()

        val names = all.map { it.name }
        assertTrue(names.contains("Group A"))
        assertTrue(names.contains("Group B"))
    }
}
