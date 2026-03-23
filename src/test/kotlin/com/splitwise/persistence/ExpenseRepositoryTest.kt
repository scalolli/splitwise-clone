package com.splitwise.persistence

import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import java.time.LocalDate
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseRepositoryTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)

    private fun saveUser(username: String) =
        userRepository.save(username = username, email = "$username@example.com", passwordHash = "hash")

    private fun saveGroup(creator: com.splitwise.domain.User) =
        groupRepository.create(name = "Test Group", description = null, creatorId = creator.id)

    @Test
    fun `create expense with shares and retrieve by id`() {
        val payer = saveUser("alice")
        val member = saveUser("bob")
        val group = saveGroup(payer)
        groupRepository.addMember(group.id, member.id)

        val shares = listOf(
            ExpenseShare(userId = payer.id, amount = Money("50.00")),
            ExpenseShare(userId = member.id, amount = Money("50.00")),
        )
        val expense = expenseRepository.create(
            groupId = group.id,
            description = "Dinner",
            amount = Money("100.00"),
            payerId = payer.id,
            shares = shares,
        )

        val found = expenseRepository.findById(expense.id)
        assertNotNull(found)
        assertEquals("Dinner", found.description)
        assertEquals(Money("100.00"), found.amount)
        assertEquals(payer.id, found.payerId)
        assertEquals(2, found.shares.size)
        assertTrue(found.shares.any { it.userId == payer.id && it.amount == Money("50.00") })
        assertTrue(found.shares.any { it.userId == member.id && it.amount == Money("50.00") })
    }

    @Test
    fun `find by id returns null for non-existent expense`() {
        val found = expenseRepository.findById(com.splitwise.domain.ExpenseId(999999L))
        assertNull(found)
    }

    @Test
    fun `update expense replaces old shares with new ones`() {
        val payer = saveUser("carol")
        val memberA = saveUser("dave")
        val memberB = saveUser("eve")
        val group = saveGroup(payer)
        groupRepository.addMember(group.id, memberA.id)
        groupRepository.addMember(group.id, memberB.id)

        val original = expenseRepository.create(
            groupId = group.id,
            description = "Lunch",
            amount = Money("90.00"),
            payerId = payer.id,
            shares = listOf(
                ExpenseShare(payer.id, Money("30.00")),
                ExpenseShare(memberA.id, Money("30.00")),
                ExpenseShare(memberB.id, Money("30.00")),
            ),
        )

        expenseRepository.update(
            id = original.id,
            description = "Lunch (updated)",
            amount = Money("60.00"),
            payerId = payer.id,
            shares = listOf(
                ExpenseShare(payer.id, Money("30.00")),
                ExpenseShare(memberA.id, Money("30.00")),
            ),
            incurredAt = LocalDate.now(),
        )

        val updated = expenseRepository.findById(original.id)!!
        assertEquals("Lunch (updated)", updated.description)
        assertEquals(Money("60.00"), updated.amount)
        assertEquals(2, updated.shares.size)
        assertTrue(updated.shares.none { it.userId == memberB.id }, "Old share for memberB should be gone")
    }

    @Test
    fun `delete expense cascade-deletes shares`() {
        val payer = saveUser("frank")
        val group = saveGroup(payer)

        val expense = expenseRepository.create(
            groupId = group.id,
            description = "Coffee",
            amount = Money("10.00"),
            payerId = payer.id,
            shares = listOf(ExpenseShare(payer.id, Money("10.00"))),
        )

        expenseRepository.delete(expense.id)

        assertNull(expenseRepository.findById(expense.id))
    }

    @Test
    fun `find all expenses for a group returns only that group's expenses`() {
        val payer = saveUser("grace")
        val group1 = saveGroup(payer)
        val group2 = groupRepository.create(name = "Other Group", description = null, creatorId = payer.id)

        expenseRepository.create(
            groupId = group1.id,
            description = "Expense A",
            amount = Money("20.00"),
            payerId = payer.id,
            shares = listOf(ExpenseShare(payer.id, Money("20.00"))),
        )
        expenseRepository.create(
            groupId = group2.id,
            description = "Expense B",
            amount = Money("30.00"),
            payerId = payer.id,
            shares = listOf(ExpenseShare(payer.id, Money("30.00"))),
        )

        val forGroup1 = expenseRepository.findByGroup(group1.id)

        assertEquals(1, forGroup1.size)
        assertEquals("Expense A", forGroup1.single().description)
    }
}
