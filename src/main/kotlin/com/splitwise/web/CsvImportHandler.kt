package com.splitwise.web

import com.splitwise.domain.CsvParser
import com.splitwise.domain.CsvParseResult
import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.Group
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import com.splitwise.persistence.CsvImportRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.ExpenseService
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.core.with
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class CsvImportViewModel(
    val groupId: Long,
    val errors: List<String> = emptyList(),
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "import"
}

data class CsvReviewViewModel(
    val groupId: Long,
    val importId: String,
    val rows: List<Map<String, Any?>>,
    val members: List<Map<String, Any?>>,
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "import_review"
}

fun csvImportHandler(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    csvImportRepository: CsvImportRepository,
    expenseService: ExpenseService,
    sessionToken: SessionToken,
): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    fun importSessionCookie(importId: UUID) = Cookie(
        name = "import_session",
        value = importId.toString(),
        maxAge = 3600,
        path = "/",
        httpOnly = true,
        secure = true,
        sameSite = SameSite.Strict,
    )

    fun membersOf(group: Group): List<Map<String, Any?>> =
        group.memberIds.mapNotNull { uid ->
            userRepository.findById(uid)?.let {
                mapOf("id" to uid.value, "username" to it.username)
            }
        }

    return routes(

        // ── GET /group/{id}/import ────────────────────────────────────────
        "/group/{id}/import" bind GET to { request ->
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)
            if (currentUserId !in group.memberIds) return@to Response(Status.FORBIDDEN)

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of CsvImportViewModel(groupId = idParam, csrfToken = nonce))
        },

        // ── POST /group/{id}/import ───────────────────────────────────────
        "/group/{id}/import" bind POST to { request ->
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)
            if (currentUserId !in group.memberIds) return@to Response(Status.FORBIDDEN)

            val csvContent = extractCsvFromMultipart(request)
            val nonce = CsrfToken.generate()

            when (val result = CsvParser.parse(csvContent)) {
                is CsvParseResult.Failure -> {
                    Response(Status.BAD_REQUEST)
                        .cookie(csrfCookie(nonce))
                        .with(htmlLens of CsvImportViewModel(
                            groupId = idParam,
                            errors = result.errors,
                            csrfToken = nonce,
                        ))
                }
                is CsvParseResult.Success -> {
                    val importId = UUID.randomUUID()
                    csvImportRepository.saveRows(importId, result.rows)
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam/import/review")
                        .cookie(importSessionCookie(importId))
                }
            }
        },

        // ── GET /group/{id}/import/review ────────────────────────────────
        "/group/{id}/import/review" bind GET to { request ->
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)
            if (currentUserId !in group.memberIds) return@to Response(Status.FORBIDDEN)

            val importIdStr = request.cookie("import_session")?.value
                ?: return@to Response(Status.BAD_REQUEST)
            val importId = runCatching { UUID.fromString(importIdStr) }.getOrNull()
                ?: return@to Response(Status.BAD_REQUEST)

            val stagedRows = csvImportRepository.findRows(importId)
            if (stagedRows.isEmpty()) return@to Response(Status.BAD_REQUEST)

            val members = membersOf(group)
            val rows = stagedRows.map { row ->
                mapOf(
                    "rowIndex" to row.rowIndex,
                    "date" to row.date.toString(),
                    "description" to row.description,
                    "amount" to row.amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                )
            }

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of CsvReviewViewModel(
                    groupId = idParam,
                    importId = importId.toString(),
                    rows = rows,
                    members = members,
                    csrfToken = nonce,
                ))
        },

        // ── POST /group/{id}/import/confirm ──────────────────────────────
        "/group/{id}/import/confirm" bind POST to { request ->
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)
            if (currentUserId !in group.memberIds) return@to Response(Status.FORBIDDEN)

            val importIdStr = request.cookie("import_session")?.value
                ?: return@to Response(Status.BAD_REQUEST)
            val importId = runCatching { UUID.fromString(importIdStr) }.getOrNull()
                ?: return@to Response(Status.BAD_REQUEST)

            val stagedRows = csvImportRepository.findRows(importId)
            if (stagedRows.isEmpty()) return@to Response(Status.BAD_REQUEST)

            val params = parseFormBody(request.bodyString())
            val memberIds = group.memberIds

            stagedRows.forEach { row ->
                val payerIdValue = params["payer_${row.rowIndex}"]?.firstOrNull()?.toLongOrNull()
                    ?: currentUserId.value
                val payerId = UserId(payerIdValue)
                val totalAmount = Money(row.amount)
                val splitAmount = totalAmount.value
                    .divide(BigDecimal(memberIds.size), 2, RoundingMode.HALF_UP)
                val shares = memberIds.map { uid -> ExpenseShare(uid, Money(splitAmount)) }

                expenseService.addExpense(
                    groupId = GroupId(idParam),
                    description = row.description,
                    amount = totalAmount,
                    payerId = payerId,
                    splits = shares,
                    memberIds = memberIds,
                    incurredAt = row.date,
                )
            }

            csvImportRepository.deleteRows(importId)

            Response(Status.FOUND).header("Location", "/group/$idParam")
        },
    )
}

private fun csrfCookie(nonce: String) = Cookie(
    name = "csrf",
    value = nonce,
    maxAge = 3600,
    path = "/",
    httpOnly = true,
    secure = true,
    sameSite = SameSite.Strict,
)

/**
 * Extracts the text body of the first file field named "csv_file" from a
 * multipart/form-data request.  Falls back to the raw body if no boundary is found
 * (useful for simple test cases).
 */
private fun extractCsvFromMultipart(request: Request): String {
    val contentType = request.header("Content-Type") ?: ""
    val boundary = contentType.substringAfter("boundary=", "").trim()
    if (boundary.isEmpty()) return request.bodyString()

    val body = request.bodyString()
    val delimiter = "--$boundary"

    return body.split(delimiter)
        .drop(1) // skip preamble
        .firstOrNull { part ->
            part.contains("""name="csv_file"""")
        }
        ?.let { part ->
            // Part format: headers\r\n\r\nbody\r\n
            val headerBodySep = "\r\n\r\n"
            val sepIdx = part.indexOf(headerBodySep)
            if (sepIdx == -1) return@let ""
            part.substring(sepIdx + headerBodySep.length).trimEnd('\r', '\n', '-')
        } ?: ""
}
