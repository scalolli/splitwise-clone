package com.splitwise.web

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvImportHandlerTest {

    private val database = PostgresTestSupport.freshDatabase()
    private val userRepository = UserRepository(database)
    private val groupRepository = GroupRepository(database)
    private val expenseRepository = ExpenseRepository(database)
    private val settlementRepository = SettlementRepository(database)
    private val sessionSecret = "test-secret-for-csv-import-tests"
    private val app = buildApp(
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        sessionSecret = sessionSecret,
    )

    private lateinit var session: String
    private var groupId: Long = 0

    @BeforeEach
    fun setUp() {
        session = TestHelpers.registerAndLogin(app, "alice", "alice@example.com")
        val alice = userRepository.findByUsername("alice")!!
        val group = groupRepository.create("Trip", null, alice.id)
        groupId = group.id.value
    }

    // ── SLICE-CSV1: Upload form ──────────────────────────────────────────────

    @Test
    fun `GET import page returns 200 with file upload form`() {
        val response = app(Request(GET, "/group/$groupId/import").cookie("session", session))

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("<form"), "Expected a form")
        assertTrue(body.contains("""type="file""""), "Expected a file input")
    }

    @Test
    fun `GET import page returns 403 for non-member`() {
        TestHelpers.registerAndLogin(app, "bob", "bob@example.com")
        val bobSession = TestHelpers.loginUser(app, "bob")

        val response = app(Request(GET, "/group/$groupId/import").cookie("session", bobSession))

        assertEquals(403, response.status.code)
    }

    @Test
    fun `POST import with valid CSV stores rows and redirects to review`() {
        val csv = "2026-01-01,Dinner,42.50\n2026-01-02,Taxi,15.00\n"
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)

        val response = postCsv(csv, csrfCookie, csrfField)

        assertEquals(302, response.status.code)
        assertTrue(
            response.header("Location")?.contains("/import/review") == true,
            "Expected redirect to review, got: ${response.header("Location")}"
        )
        assertTrue(
            response.cookies().any { it.name == "import_session" },
            "Expected import_session cookie"
        )
    }

    @Test
    fun `POST import with invalid date returns 400 with error`() {
        val csv = "not-a-date,Dinner,42.50\n"
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)

        val response = postCsv(csv, csrfCookie, csrfField)

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("date"), "Expected date error message")
    }

    @Test
    fun `POST import with non-numeric amount returns 400 with error`() {
        val csv = "2026-01-01,Dinner,abc\n"
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)

        val response = postCsv(csv, csrfCookie, csrfField)

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("amount"), "Expected amount error message")
    }

    @Test
    fun `POST import with empty description returns 400 with error`() {
        val csv = "2026-01-01,,42.50\n"
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)

        val response = postCsv(csv, csrfCookie, csrfField)

        assertEquals(400, response.status.code)
        assertTrue(response.bodyString().contains("description"), "Expected description error message")
    }

    @Test
    fun `POST import with empty file returns 400 with error`() {
        val csv = ""
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)

        val response = postCsv(csv, csrfCookie, csrfField)

        assertEquals(400, response.status.code)
    }

    // ── SLICE-CSV2: Review page ──────────────────────────────────────────────

    @Test
    fun `GET review page shows parsed rows with payer dropdown`() {
        val csv = "2026-01-01,Dinner,42.50\n2026-01-02,Taxi,15.00\n"
        val (csrfCookie, csrfField) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)
        val uploadResponse = postCsv(csv, csrfCookie, csrfField)
        val importCookie = uploadResponse.cookies().first { it.name == "import_session" }.value

        val response = app(
            Request(GET, "/group/$groupId/import/review")
                .cookie("session", session)
                .cookie("import_session", importCookie)
        )

        assertEquals(200, response.status.code)
        val body = response.bodyString()
        assertTrue(body.contains("Dinner"), "Expected row description 'Dinner'")
        assertTrue(body.contains("Taxi"), "Expected row description 'Taxi'")
        assertTrue(body.contains("42.50") || body.contains("42,50"), "Expected amount")
        assertTrue(body.contains("<select"), "Expected payer dropdown")
    }

    @Test
    fun `GET review page without import_session cookie returns 400`() {
        val response = app(
            Request(GET, "/group/$groupId/import/review").cookie("session", session)
        )

        assertEquals(400, response.status.code)
    }

    // ── SLICE-CSV3: Batch create ─────────────────────────────────────────────

    @Test
    fun `POST confirm creates expenses and redirects to group page`() {
        val csv = "2026-01-01,Dinner,42.50\n2026-01-02,Taxi,15.00\n"
        val (csrfCookie1, csrfField1) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)
        val uploadResponse = postCsv(csv, csrfCookie1, csrfField1)
        val importCookie = uploadResponse.cookies().first { it.name == "import_session" }.value

        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie2, csrfField2) = TestHelpers.getCsrfToken(
            app, "/group/$groupId/import", session
        )
        val confirmResponse = app(
            Request(POST, "/group/$groupId/import/confirm")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie("import_session", importCookie)
                .cookie(Cookie("csrf", csrfCookie2))
                .body(TestHelpers.formBody(
                    "payer_0" to alice.id.value.toString(),
                    "payer_1" to alice.id.value.toString(),
                    "_csrf" to csrfField2,
                ))
        )

        assertEquals(302, confirmResponse.status.code)
        assertTrue(
            confirmResponse.header("Location") == "/group/$groupId",
            "Expected redirect to group page, got: ${confirmResponse.header("Location")}"
        )

        val expenses = expenseRepository.findByGroup(com.splitwise.domain.GroupId(groupId))
        assertEquals(2, expenses.size, "Expected 2 expenses to be created")
        assertTrue(expenses.any { it.description == "Dinner" })
        assertTrue(expenses.any { it.description == "Taxi" })
    }

    @Test
    fun `POST confirm deletes staging rows after creating expenses`() {
        val csv = "2026-01-01,Lunch,20.00\n"
        val (csrfCookie1, csrfField1) = TestHelpers.getCsrfToken(app, "/group/$groupId/import", session)
        val uploadResponse = postCsv(csv, csrfCookie1, csrfField1)
        val importCookie = uploadResponse.cookies().first { it.name == "import_session" }.value

        val alice = userRepository.findByUsername("alice")!!
        val (csrfCookie2, csrfField2) = TestHelpers.getCsrfToken(
            app, "/group/$groupId/import", session
        )
        app(
            Request(POST, "/group/$groupId/import/confirm")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .cookie("session", session)
                .cookie("import_session", importCookie)
                .cookie(Cookie("csrf", csrfCookie2))
                .body(TestHelpers.formBody(
                    "payer_0" to alice.id.value.toString(),
                    "_csrf" to csrfField2,
                ))
        )

        // Attempting to review with same import_session should now return 400 (rows gone)
        val reviewResponse = app(
            Request(GET, "/group/$groupId/import/review")
                .cookie("session", session)
                .cookie("import_session", importCookie)
        )
        assertEquals(400, reviewResponse.status.code)
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun postCsv(csv: String, csrfCookie: String, csrfField: String) =
        app(
            Request(POST, "/group/$groupId/import")
                .cookie("session", session)
                .cookie(Cookie("csrf", csrfCookie))
                .header("Content-Type", "multipart/form-data; boundary=boundary123")
                .body(buildMultipart(csv, csrfField))
        )

    private fun buildMultipart(csvContent: String, csrfField: String): String {
        val sb = StringBuilder()
        sb.append("--boundary123\r\n")
        sb.append("Content-Disposition: form-data; name=\"_csrf\"\r\n\r\n")
        sb.append("$csrfField\r\n")
        sb.append("--boundary123\r\n")
        sb.append("Content-Disposition: form-data; name=\"csv_file\"; filename=\"transactions.csv\"\r\n")
        sb.append("Content-Type: text/csv\r\n\r\n")
        sb.append("$csvContent\r\n")
        sb.append("--boundary123--\r\n")
        return sb.toString()
    }
}
