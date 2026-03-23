package com.splitwise.web

import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.Money
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.PostgresTestSupport
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-group-handler-tests"
    private val sessionToken = SessionToken(sessionSecret)
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private fun registerAndLogin(username: String, email: String, password: String = "secret123"): String =
        TestHelpers.registerAndLogin(app, username, email, password)

    @Test
    fun `GET group returns 404 for non-existent group`() {
        val session = registerAndLogin("alice", "alice@example.com")

        val response = app(Request(GET, "/group/99999").cookie("session", session))

        assertEquals(404, response.status.code)
    }

    @Test
    fun `unauthenticated GET group redirects to login`() {
        val response = app(Request(GET, "/group/1"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET group returns 403 for authenticated non-member`() {
        val ownerSession = registerAndLogin("owner", "owner@example.com")
        val strangerSession = registerAndLogin("stranger", "stranger@example.com")
        val owner = userRepository.findByUsername("owner")!!
        val group = groupRepository.create("Private Group", null, owner.id)

        val ownerResponse = app(Request(GET, "/group/${group.id.value}").cookie("session", ownerSession))
        assertEquals(200, ownerResponse.status.code)

        val strangerResponse = app(Request(GET, "/group/${group.id.value}").cookie("session", strangerSession))

        assertEquals(403, strangerResponse.status.code)
    }

    @Test
    fun `GET group returns 200 with members listed`() {
        val session = registerAndLogin("bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        val group = groupRepository.create("Bob's Group", null, bob.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("bob"), "Expected member 'bob' in response body")
    }

    @Test
    fun `GET group returns expenses listed with description amount and payer`() {
        val session = registerAndLogin("carol", "carol@example.com")
        val carol = userRepository.findByUsername("carol")!!
        val group = groupRepository.create("Carol's Group", null, carol.id)

        expenseRepository.create(
            groupId = group.id,
            description = "Dinner at restaurant",
            amount = Money("60.00"),
            payerId = carol.id,
            shares = listOf(ExpenseShare(carol.id, Money("60.00"))),
        )

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("Dinner at restaurant"), "Expected expense description in body")
        assertTrue(body.contains("60"), "Expected expense amount in body")
        assertTrue(body.contains("carol"), "Expected payer name in body")
    }

    @Test
    fun `GET group contains link to edit group`() {
        val session = registerAndLogin("nav_edit", "nav_edit@example.com")
        val user = userRepository.findByUsername("nav_edit")!!
        val group = groupRepository.create("Nav Group", null, user.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""href="/group/${group.id.value}/edit""""),
            "Expected link to edit group on group page")
    }

    @Test
    fun `GET group contains link to add expense`() {
        val session = registerAndLogin("nav_expense", "nav_expense@example.com")
        val user = userRepository.findByUsername("nav_expense")!!
        val group = groupRepository.create("Expense Nav Group", null, user.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""href="/group/${group.id.value}/add_expense""""),
            "Expected link to add expense on group page")
    }

    @Test
    fun `GET group embeds csrf token in logout form`() {
        val session = registerAndLogin("nav_csrf", "nav_csrf@example.com")
        val user = userRepository.findByUsername("nav_csrf")!!
        val group = groupRepository.create("CSRF Group", null, user.id)

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""name="_csrf""""),
            "Expected _csrf hidden field in logout form on group page")
    }

    @Test
    fun `GET group contains edit link per expense row`() {
        val session = registerAndLogin("nav_row", "nav_row@example.com")
        val user = userRepository.findByUsername("nav_row")!!
        val group = groupRepository.create("Row Group", null, user.id)
        val expense = expenseRepository.create(
            groupId = group.id,
            description = "Row Expense",
            amount = Money("10.00"),
            payerId = user.id,
            shares = listOf(ExpenseShare(user.id, Money("10.00"))),
        )

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""href="/expenses/${expense.id.value}/edit""""),
            "Expected per-expense edit link on group page")
    }

    @Test
    fun `GET group expense table shows incurred date`() {
        val session = registerAndLogin("date_user", "date_user@example.com")
        val user = userRepository.findByUsername("date_user")!!
        val group = groupRepository.create("Date Group", null, user.id)
        expenseRepository.create(
            groupId = group.id,
            description = "Dated Expense",
            amount = Money("15.00"),
            payerId = user.id,
            shares = listOf(ExpenseShare(user.id, Money("15.00"))),
        )

        val response = app(Request(GET, "/group/${group.id.value}").cookie("session", session))

        assertEquals(200, response.status.code)
        // The date column must appear in the table header
        assertTrue(response.bodyString().contains("Date"),
            "Expected 'Date' column header in expense table")
    }
}
