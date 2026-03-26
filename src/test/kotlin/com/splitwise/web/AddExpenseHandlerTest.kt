package com.splitwise.web

import com.splitwise.domain.GroupId
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
import kotlin.test.assertTrue

class AddExpenseHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-add-expense-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private lateinit var aliceSession: String
    private var groupId: GroupId = GroupId(0)

    @BeforeEach
    fun setup() {
        aliceSession = TestHelpers.registerAndLogin(app, "alice", "alice@example.com")
        val alice = userRepository.findByUsername("alice")!!
        val group = groupRepository.create("Alice's Group", null, alice.id)
        groupId = group.id
    }

    @Test
    fun `GET add expense returns 200 for group member`() {
        val response = app(
            Request(GET, "/group/${groupId.value}/add_expense")
                .cookie("session", aliceSession)
        )

        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET add expense returns 403 for non-member`() {
        val strangerSession = TestHelpers.registerAndLogin(app, "stranger", "stranger@example.com")

        val response = app(
            Request(GET, "/group/${groupId.value}/add_expense")
                .cookie("session", strangerSession)
        )

        assertEquals(403, response.status.code)
    }

    @Test
    fun `GET add expense unauthenticated redirects to login`() {
        val response = app(Request(GET, "/group/${groupId.value}/add_expense"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    @Test
    fun `GET add expense embeds csrf token`() {
        val response = app(
            Request(GET, "/group/${groupId.value}/add_expense")
                .cookie("session", aliceSession)
        )

        assertTrue(response.cookies().any { it.name == "csrf" }, "Expected csrf cookie")
        assertTrue(response.bodyString().contains("_csrf"), "Expected _csrf hidden field")
    }

    @Test
    fun `POST add expense valid redirects to group page with flash`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        val body = TestHelpers.formBody(
            "description" to "Lunch",
            "amount" to "30.00",
            "payer_id" to alice.id.value.toString(),
            "_csrf" to csrfFormValue,
        )

        val response = app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(body)
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flashCookie = response.cookies().find { it.name == "flash" }
        assertNotNull(flashCookie, "Expected flash cookie")
        assertTrue(
            flashCookie.value.contains("Expense added successfully"),
            "Expected 'Expense added successfully', got: ${flashCookie.value}"
        )
    }

    @Test
    fun `POST add expense saves expense and shares to the database`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        val body = TestHelpers.formBody(
            "description" to "Coffee",
            "amount" to "10.00",
            "payer_id" to alice.id.value.toString(),
            "_csrf" to csrfFormValue,
        )

        app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(body)
        )

        val expenses = expenseRepository.findByGroup(groupId)
        assertEquals(1, expenses.size, "Expected one expense to be saved")
        assertEquals("Coffee", expenses[0].description)
        assertEquals("10.00", expenses[0].amount.value.toPlainString())
    }

    @Test
    fun `POST add expense non-member returns 403`() {
        val strangerSession = TestHelpers.registerAndLogin(app, "bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/login", null
        ) // use a public page for csrf since bob can't access the form

        // We re-use alice's group; bob is not a member
        // Bob can get a csrf token from login page and attempt the POST directly
        val bobLoginSession = TestHelpers.registerAndLogin(app, "bob2", "bob2@example.com")
        val (csrf2CookieValue, csrf2FormValue) = TestHelpers.getCsrfToken(app, "/login")

        val response = app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", strangerSession)
                .cookie(Cookie("csrf", csrf2CookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Hack",
                    "amount" to "10.00",
                    "payer_id" to bob.id.value.toString(),
                    "_csrf" to csrf2FormValue,
                ))
        )

        assertEquals(403, response.status.code)
    }

    // --- Validation rule tests ---

    @Test
    fun `POST add expense with empty description returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        val response = app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "",
                    "amount" to "30.00",
                    "payer_id" to alice.id.value.toString(),
                    "_csrf" to csrfFormValue,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Description is required"))
    }

    @Test
    fun `POST add expense with zero amount returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        val response = app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Lunch",
                    "amount" to "0.00",
                    "payer_id" to alice.id.value.toString(),
                    "_csrf" to csrfFormValue,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Amount must be greater than zero"))
    }

    @Test
    fun `POST add expense with payer not in group returns 400 with error`() {
        val nonMemberSession = TestHelpers.registerAndLogin(app, "carol", "carol@example.com")
        val carol = userRepository.findByUsername("carol")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        val response = app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Lunch",
                    "amount" to "30.00",
                    "payer_id" to carol.id.value.toString(), // carol is not a member
                    "_csrf" to csrfFormValue,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Payer is not a member of this group"))
    }

    @Test
    fun `GET add expense contains date input`() {
        val response = app(
            Request(GET, "/group/${groupId.value}/add_expense")
                .cookie("session", aliceSession)
        )

        assertEquals(200, response.status.code)
        assertTrue(response.bodyString().contains("""name="incurred_at""""),
            "Expected date input on add expense page")
    }

    @Test
    fun `POST add expense with explicit date saves that date`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Old dinner",
                    "amount" to "20.00",
                    "payer_id" to alice.id.value.toString(),
                    "incurred_at" to "2024-06-15",
                    "_csrf" to csrfFormValue,
                ))
        )

        val expenses = expenseRepository.findByGroup(groupId)
        assertEquals(1, expenses.size)
        val savedDate = expenses[0].incurredAt.atZone(java.time.ZoneOffset.UTC).toLocalDate()
        assertEquals(java.time.LocalDate.of(2024, 6, 15), savedDate)
    }

    @Test
    fun `POST add expense without date defaults to today`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Coffee",
                    "amount" to "5.00",
                    "payer_id" to alice.id.value.toString(),
                    "_csrf" to csrfFormValue,
                ))
        )

        val expenses = expenseRepository.findByGroup(groupId)
        assertEquals(1, expenses.size)
        val today = java.time.LocalDate.now()
        val savedDate = expenses[0].incurredAt.atZone(java.time.ZoneOffset.UTC).toLocalDate()
        assertEquals(today, savedDate)
    }

    @Test
    fun `POST add expense splits equally among all group members`() {
        val alice = userRepository.findByUsername("alice")!!
        val bobSession = TestHelpers.registerAndLogin(app, "bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        groupRepository.addMember(groupId, bob.id)

        val (csrfCookieValue, csrfFormValue) = TestHelpers.getCsrfToken(
            app, "/group/${groupId.value}/add_expense", aliceSession
        )

        app(
            Request(POST, "/group/${groupId.value}/add_expense")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookieValue))
                .body(TestHelpers.formBody(
                    "description" to "Dinner",
                    "amount" to "30.00",
                    "payer_id" to alice.id.value.toString(),
                    "_csrf" to csrfFormValue,
                ))
        )

        val expenses = expenseRepository.findByGroup(groupId)
        assertEquals(1, expenses.size)
        val shares = expenses[0].shares
        assertEquals(2, shares.size, "Expected one share per member")
        shares.forEach { share ->
            assertEquals("15.00", share.amount.value.toPlainString(),
                "Expected equal split of 15.00 for user ${share.userId}")
        }
    }
}
