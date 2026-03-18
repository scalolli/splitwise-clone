package com.splitwise.web

import com.splitwise.domain.ExpenseId
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
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditExpenseHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-edit-expense-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private var groupId: GroupId = GroupId(0)
    private var expenseId: ExpenseId = ExpenseId(0)
    private lateinit var payerSession: String   // alice — payer of the expense
    private lateinit var creatorSession: String // bob — group creator (not payer)
    private lateinit var memberSession: String  // carol — plain member (not payer, not creator)

    @BeforeEach
    fun setUp() {
        // bob creates the group
        creatorSession = TestHelpers.registerAndLogin(app, "bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        val group = groupRepository.create("Bob's Group", null, bob.id)
        groupId = group.id

        // alice is a member and will be the expense payer
        payerSession = TestHelpers.registerAndLogin(app, "alice", "alice@example.com")
        val alice = userRepository.findByUsername("alice")!!
        groupRepository.addMember(groupId, alice.id)

        // carol is a plain member
        memberSession = TestHelpers.registerAndLogin(app, "carol", "carol@example.com")
        val carol = userRepository.findByUsername("carol")!!
        groupRepository.addMember(groupId, carol.id)

        expenseId = expenseRepository.create(
            groupId = groupId,
            description = "Original Expense",
            amount = Money("60.00"),
            payerId = alice.id,
            shares = listOf(ExpenseShare(alice.id, Money("60.00"))),
        ).id
    }

    // --- GET /expenses/{id}/edit ---

    @Test
    fun `GET edit expense returns 200 for expense payer`() {
        val response = app(Request(GET, "/expenses/${expenseId.value}/edit").cookie("session", payerSession))

        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET edit expense returns 200 for group creator`() {
        val response = app(Request(GET, "/expenses/${expenseId.value}/edit").cookie("session", creatorSession))

        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET edit expense returns 403 for other member`() {
        val response = app(Request(GET, "/expenses/${expenseId.value}/edit").cookie("session", memberSession))

        assertEquals(403, response.status.code)
    }

    @Test
    fun `GET edit expense unauthenticated redirects to login`() {
        val response = app(Request(GET, "/expenses/${expenseId.value}/edit"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET edit expense returns 404 for non-existent expense`() {
        val response = app(Request(GET, "/expenses/99999/edit").cookie("session", payerSession))

        assertEquals(404, response.status.code)
    }

    // --- POST /expenses/{id}/edit ---

    @Test
    fun `POST edit expense valid updates expense and redirects with flash`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/edit")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", payerSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "description" to "Updated Expense",
                    "amount" to "50.00",
                    "payer_id" to alice.id.value.toString(),
                    "split_user_id" to alice.id.value.toString(),
                    "split_amount" to "50.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(flash.value.contains("Expense updated successfully"), "Got: ${flash.value}")

        val updated = expenseRepository.findById(expenseId)!!
        assertEquals("Updated Expense", updated.description)
        assertEquals("50.00", updated.amount.value.toPlainString())
    }

    @Test
    fun `POST edit expense by unauthorised member returns 403`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/edit")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", memberSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "description" to "Hacked",
                    "amount" to "50.00",
                    "payer_id" to alice.id.value.toString(),
                    "split_user_id" to alice.id.value.toString(),
                    "split_amount" to "50.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST edit expense with validation failure re-renders form with errors`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/edit")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", payerSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "description" to "",  // invalid: empty
                    "amount" to "50.00",
                    "payer_id" to alice.id.value.toString(),
                    "split_user_id" to alice.id.value.toString(),
                    "split_amount" to "50.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Description is required"))
    }

    // --- POST /expenses/{id}/delete ---

    @Test
    fun `POST delete expense removes expense and redirects with flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/delete")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", payerSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(flash.value.contains("Expense deleted"), "Got: ${flash.value}")
        assertNull(expenseRepository.findById(expenseId), "Expense should be deleted from DB")
    }

    @Test
    fun `POST delete expense by group creator removes expense`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/delete")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertNull(expenseRepository.findById(expenseId), "Expense should be deleted from DB")
    }

    @Test
    fun `POST delete expense by unauthorised member returns 403`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/${expenseId.value}/delete")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", memberSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST delete expense returns 404 for non-existent expense`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/expenses/${expenseId.value}/edit", payerSession)

        val response = app(
            Request(POST, "/expenses/99999/delete")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", payerSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(404, response.status.code)
    }
}
