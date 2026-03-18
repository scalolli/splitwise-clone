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
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettlementHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-settlement-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private var groupId: GroupId = GroupId(0)
    private lateinit var aliceSession: String
    private lateinit var bobSession: String
    private lateinit var outsiderSession: String

    @BeforeEach
    fun setUp() {
        // alice creates the group
        aliceSession = TestHelpers.registerAndLogin(app, "alice", "alice@example.com")
        val alice = userRepository.findByUsername("alice")!!
        val group = groupRepository.create("Alice's Group", null, alice.id)
        groupId = group.id

        // bob is a member
        bobSession = TestHelpers.registerAndLogin(app, "bob", "bob@example.com")
        val bob = userRepository.findByUsername("bob")!!
        groupRepository.addMember(groupId, bob.id)

        // carol is an outsider (not a member)
        outsiderSession = TestHelpers.registerAndLogin(app, "carol", "carol@example.com")

        // alice paid 60 — bob owes alice 30
        expenseRepository.create(
            groupId = groupId,
            description = "Dinner",
            amount = Money("60.00"),
            payerId = alice.id,
            shares = listOf(
                ExpenseShare(alice.id, Money("30.00")),
                ExpenseShare(bob.id, Money("30.00")),
            ),
        )
    }

    // --- POST /group/{id}/settle ---

    @Test
    fun `POST settle valid records settlement and redirects with flash`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", bobSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", bobSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "30.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(flash.value.contains("Settlement recorded"), "Got flash: ${flash.value}")

        val settlements = settlementRepository.findByGroup(groupId)
        assertEquals(1, settlements.size)
        assertEquals(bob.id, settlements[0].fromUserId)
        assertEquals(alice.id, settlements[0].toUserId)
        assertEquals("30.00", settlements[0].amount.value.toPlainString())
    }

    @Test
    fun `POST settle by non-member returns 403`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", bobSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", outsiderSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "30.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST settle with amount zero returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", aliceSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "0.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Amount must be greater than zero"), "Got: ${response.bodyString()}")
    }

    @Test
    fun `POST settle with negative amount returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", aliceSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "-5.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Amount must be greater than zero"), "Got: ${response.bodyString()}")
    }

    @Test
    fun `POST settle with same from and to user returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", aliceSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to alice.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "10.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Cannot settle with yourself"), "Got: ${response.bodyString()}")
    }

    @Test
    fun `POST settle where to_user is not a group member returns 400 with error`() {
        val alice = userRepository.findByUsername("alice")!!
        val carol = userRepository.findByUsername("carol")!! // outsider
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", aliceSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", aliceSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to alice.id.value.toString(),
                    "to_user_id" to carol.id.value.toString(),
                    "amount" to "10.00",
                    "_csrf" to csrfForm,
                ))
        )

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("Both users must be group members"), "Got: ${response.bodyString()}")
    }

    @Test
    fun `GET group page unauthenticated redirects to login`() {
        val response = app(Request(GET, "/group/${groupId.value}"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    // --- GET /group/{id} settlement history ---

    @Test
    fun `GET group page shows settlement history section`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        settlementRepository.create(groupId, bob.id, alice.id, Money("30.00"))

        val response = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("Settlement History") || body.contains("Settlements"), "Expected settlements section, got: $body")
        assertTrue(body.contains("30.00"), "Expected settlement amount 30.00 in body")
    }

    @Test
    fun `GET group page shows settlement history in descending date order`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        settlementRepository.create(groupId, bob.id, alice.id, Money("10.00"))
        Thread.sleep(10)
        settlementRepository.create(groupId, bob.id, alice.id, Money("20.00"))

        val response = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        val idx10 = body.indexOf("10.00")
        val idx20 = body.indexOf("20.00")
        assertTrue(idx20 < idx10, "Expected most recent settlement (20.00) to appear before older (10.00)")
    }

    @Test
    fun `GET group page shows settle form with member dropdowns`() {
        val response = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("action=\"/group/${groupId.value}/settle\""), "Expected settle form action, got: $body")
        assertTrue(body.contains("from_user_id") && body.contains("to_user_id"), "Expected from/to user fields")
    }

    @Test
    fun `recording a settlement reduces the displayed balance`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!

        // Before settlement — bob owes alice 30.00
        val before = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))
        assertTrue(before.bodyString().contains("30.00"), "Expected 30.00 balance before settlement")

        // Record partial settlement of 15.00
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", bobSession)
        app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", bobSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "15.00",
                    "_csrf" to csrfForm,
                ))
        )

        // After — remaining balance should be 15.00
        val after = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))
        assertTrue(after.bodyString().contains("15.00"), "Expected 15.00 remaining balance after partial settlement")
    }

    @Test
    fun `full settlement removes balance entry from group page`() {
        val alice = userRepository.findByUsername("alice")!!
        val bob = userRepository.findByUsername("bob")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}", bobSession)

        app(
            Request(POST, "/group/${groupId.value}/settle")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", bobSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody(
                    "from_user_id" to bob.id.value.toString(),
                    "to_user_id" to alice.id.value.toString(),
                    "amount" to "30.00",
                    "_csrf" to csrfForm,
                ))
        )

        val response = app(Request(GET, "/group/${groupId.value}").cookie("session", aliceSession))
        val body = response.bodyString()
        assertTrue(body.contains("All settled up!"), "Expected 'All settled up!' after full settlement, got: $body")
    }
}
