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

class EditGroupHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-edit-group-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private var groupId: GroupId = GroupId(0)
    private lateinit var creatorSession: String
    private lateinit var otherSession: String

    @BeforeEach
    fun setUp() {
        creatorSession = TestHelpers.registerAndLogin(app, "creator", "creator@example.com")
        otherSession = TestHelpers.registerAndLogin(app, "other", "other@example.com")
        val creator = userRepository.findByUsername("creator")!!
        groupId = groupRepository.create("Original Name", null, creator.id).id
    }

    // --- GET /group/{id}/edit ---

    @Test
    fun `GET edit returns 200 for group creator`() {
        val response = app(Request(GET, "/group/${groupId.value}/edit").cookie("session", creatorSession))

        assertEquals(200, response.status.code)
    }

    @Test
    fun `GET edit redirects non-creator with flash`() {
        val response = app(Request(GET, "/group/${groupId.value}/edit").cookie("session", otherSession))

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash, "Expected flash cookie for non-creator")
        assertTrue(
            flash.value.contains("do not have permission"),
            "Expected permission error flash, got: ${flash.value}"
        )
    }

    @Test
    fun `GET edit unauthenticated redirects to login`() {
        val response = app(Request(GET, "/group/${groupId.value}/edit"))

        assertEquals(302, response.status.code)
        assertEquals("/login", response.header("Location"))
    }

    // --- POST /group/{id}/edit ---

    @Test
    fun `POST edit valid updates name and redirects with flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/edit")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("name" to "Updated Name", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("Group updated successfully"),
            "Expected 'Group updated successfully', got: ${flash.value}"
        )
        val updated = groupRepository.findById(groupId)!!
        assertEquals("Updated Name", updated.name)
    }

    @Test
    fun `POST edit by non-creator redirects with permission error flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/edit")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", otherSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("name" to "Hacked Name", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("do not have permission"),
            "Expected permission error flash, got: ${flash.value}"
        )
    }

    // --- POST /group/{id}/add_member ---

    @Test
    fun `POST add_member valid adds member and redirects with flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/add_member")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("username" to "other", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("Member added successfully"),
            "Expected 'Member added successfully', got: ${flash.value}"
        )
        val other = userRepository.findByUsername("other")!!
        val group = groupRepository.findById(groupId)!!
        assertTrue(group.memberIds.any { it == other.id }, "Expected 'other' to be a member")
    }

    @Test
    fun `POST add_member non-existent user redirects with flash 'User not found'`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/add_member")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("username" to "ghost", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("User not found"),
            "Expected 'User not found', got: ${flash.value}"
        )
    }

    @Test
    fun `POST add_member already a member redirects with appropriate flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/add_member")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("username" to "creator", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("already a member"),
            "Expected 'already a member' flash, got: ${flash.value}"
        )
    }

    @Test
    fun `POST add_member by non-creator redirects with permission error flash`() {
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/add_member")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", otherSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("username" to "creator", "_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("do not have permission"),
            "Expected permission error flash, got: ${flash.value}"
        )
    }

    // --- POST /group/{id}/remove_member/{userId} ---

    @Test
    fun `POST remove_member removes member and redirects with flash`() {
        val other = userRepository.findByUsername("other")!!
        groupRepository.addMember(groupId, other.id)
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/remove_member/${other.id.value}")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        assertEquals("/group/${groupId.value}", response.header("Location"))
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("Member removed successfully"),
            "Expected 'Member removed successfully', got: ${flash.value}"
        )
        val group = groupRepository.findById(groupId)!!
        assertTrue(group.memberIds.none { it == other.id }, "Expected 'other' to no longer be a member")
    }

    @Test
    fun `POST remove_member cannot remove group creator`() {
        val creator = userRepository.findByUsername("creator")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/remove_member/${creator.id.value}")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", creatorSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("Cannot remove the group creator"),
            "Expected 'Cannot remove the group creator', got: ${flash.value}"
        )
    }

    @Test
    fun `POST remove_member by non-creator redirects with permission error flash`() {
        val creator = userRepository.findByUsername("creator")!!
        val (csrfCookie, csrfForm) = TestHelpers.getCsrfToken(app, "/group/${groupId.value}/edit", creatorSession)

        val response = app(
            Request(POST, "/group/${groupId.value}/remove_member/${creator.id.value}")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", otherSession)
                .cookie(Cookie("csrf", csrfCookie))
                .body(TestHelpers.formBody("_csrf" to csrfForm))
        )

        assertEquals(302, response.status.code)
        val flash = response.cookies().find { it.name == "flash" }
        assertNotNull(flash)
        assertTrue(
            flash.value.contains("do not have permission"),
            "Expected permission error flash, got: ${flash.value}"
        )
    }
}
