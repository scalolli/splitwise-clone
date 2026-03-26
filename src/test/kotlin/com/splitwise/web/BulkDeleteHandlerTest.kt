package com.splitwise.web

import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BulkDeleteHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-bulk-delete-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private lateinit var ownerSession: String
    private lateinit var memberSession: String
    private var groupId: Long = 0

    @BeforeEach
    fun setUp() {
        ownerSession = TestHelpers.registerAndLogin(app, "owner", "owner@example.com")
        memberSession = TestHelpers.registerAndLogin(app, "member", "member@example.com")
        val owner = userRepository.findByUsername("owner")!!
        val member = userRepository.findByUsername("member")!!
        val group = groupRepository.create("Trip", null, owner.id)
        groupRepository.addMember(group.id, member.id)
        groupId = group.id.value
    }

    private fun createExpense(description: String, amount: String = "10.00"): Long {
        val owner = userRepository.findByUsername("owner")!!
        val expense = expenseRepository.create(
            groupId = GroupId(groupId),
            description = description,
            amount = Money(amount),
            payerId = owner.id,
            shares = listOf(ExpenseShare(owner.id, Money(amount))),
        )
        return expense.id.value
    }

    private fun postDelete(session: String, csrfCookie: String, csrfField: String, vararg expenseIds: Long) =
        app(
            Request(POST, "/group/$groupId/expenses/delete")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    *expenseIds.map { "expense_id" to it.toString() }.toTypedArray(),
                    "_csrf" to csrfField,
                ))
        )

    // ── Visibility ───────────────────────────────────────────────────────────

    @Test
    fun `GET group shows checkboxes and delete button for owner`() {
        createExpense("Dinner")

        val response = app(Request(GET, "/group/$groupId").cookie("session", ownerSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("""type="checkbox""""), "Expected checkboxes for owner")
        assertTrue(body.contains("Delete selected"), "Expected delete button for owner")
    }

    @Test
    fun `GET group does not show checkboxes or delete button for non-owner member`() {
        createExpense("Dinner")

        val response = app(Request(GET, "/group/$groupId").cookie("session", memberSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertFalse(body.contains("Delete selected"), "Expected no delete button for non-owner")
    }

    // ── Bulk delete ───────────────────────────────────────────────────────────

    @Test
    fun `POST bulk delete removes selected expenses and redirects to group page`() {
        val id1 = createExpense("Dinner")
        val id2 = createExpense("Taxi")
        createExpense("Lunch") // not selected

        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId", ownerSession)
        val response = postDelete(ownerSession, csrfCookie, csrfField, id1, id2)

        assertEquals(302, response.status.code)
        assertEquals("/group/$groupId", response.header("Location"))

        val remaining = expenseRepository.findByGroup(GroupId(groupId))
        assertEquals(1, remaining.size)
        assertEquals("Lunch", remaining.first().description)
    }

    @Test
    fun `POST bulk delete with no expenses selected redirects without deleting anything`() {
        val id1 = createExpense("Dinner")

        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId", ownerSession)
        val response = postDelete(ownerSession, csrfCookie, csrfField) // no IDs

        assertEquals(302, response.status.code)
        val remaining = expenseRepository.findByGroup(GroupId(groupId))
        assertEquals(1, remaining.size)
    }

    @Test
    fun `POST bulk delete returns 403 for non-owner member`() {
        val id1 = createExpense("Dinner")

        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId", memberSession)
        val response = postDelete(memberSession, csrfCookie, csrfField, id1)

        assertEquals(403, response.status.code)
        val remaining = expenseRepository.findByGroup(GroupId(groupId))
        assertEquals(1, remaining.size, "Expense must not be deleted")
    }

    @Test
    fun `POST bulk delete ignores expense IDs that belong to a different group`() {
        val otherOwnerSession = TestHelpers.registerAndLogin(app, "other", "other@example.com")
        val other = userRepository.findByUsername("other")!!
        val otherGroup = groupRepository.create("Other Trip", null, other.id)
        val foreignExpense = expenseRepository.create(
            groupId = otherGroup.id,
            description = "Foreign Expense",
            amount = Money("50.00"),
            payerId = other.id,
            shares = listOf(ExpenseShare(other.id, Money("50.00"))),
        )

        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId", ownerSession)
        postDelete(ownerSession, csrfCookie, csrfField, foreignExpense.id.value)

        // foreign expense must be untouched
        val stillExists = expenseRepository.findById(foreignExpense.id)
        assertTrue(stillExists != null, "Foreign expense must not be deleted")
    }
}
