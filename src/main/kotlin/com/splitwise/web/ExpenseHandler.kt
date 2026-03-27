package com.splitwise.web

import com.splitwise.domain.ExpenseId
import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.ExpenseService
import com.splitwise.service.ValidationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
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

data class AddExpenseViewModel(
    val groupId: Long,
    val members: List<Map<String, Any?>>,
    val errors: List<String> = emptyList(),
    val description: String = "",
    val amount: String = "",
    val incurredAt: String = "",
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "add_expense"
}

data class EditExpenseViewModel(
    val expenseId: Long,
    val groupId: Long,
    val members: List<Map<String, Any?>>,
    val errors: List<String> = emptyList(),
    val description: String = "",
    val amount: String = "",
    val incurredAt: String = "",
    val selectedPayerId: Long = 0,
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "edit_expense"
}

fun expenseHandler(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    expenseRepository: ExpenseRepository,
    sessionToken: SessionToken,
): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
    val expenseService = ExpenseService(expenseRepository)
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE  // YYYY-MM-DD

    fun csrfCookie(nonce: String) = Cookie(
        name = "csrf", value = nonce, maxAge = 3600, path = "/",
        httpOnly = true, secure = true, sameSite = SameSite.Strict,
    )

    fun flashCookie(message: String) = Cookie(
        name = "flash", value = message, maxAge = 60, path = "/",
        httpOnly = true, secure = true, sameSite = SameSite.Strict,
    )

    return routes(
        "/group/{id}/add_expense" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)

            if (group.memberIds.none { it == currentUserId }) {
                return@to Response(Status.FORBIDDEN)
            }

            val members = group.memberIds.mapNotNull { uid ->
                val user = userRepository.findById(uid) ?: return@mapNotNull null
                mapOf("id" to uid.value, "username" to user.username)
            }

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of AddExpenseViewModel(
                    groupId = idParam,
                    members = members,
                    incurredAt = LocalDate.now().format(dateFormatter),
                    csrfToken = nonce,
                ))
        },

        "/group/{id}/add_expense" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)

            if (group.memberIds.none { it == currentUserId }) {
                return@to Response(Status.FORBIDDEN)
            }

            val params = parseFormBody(request.bodyString())
            val description = params["description"]?.firstOrNull()?.trim() ?: ""
            val amountRaw = params["amount"]?.firstOrNull()?.trim() ?: "0"
            val payerIdRaw = params["payer_id"]?.firstOrNull()?.trim() ?: ""
            val incurredAtRaw = params["incurred_at"]?.firstOrNull()?.trim() ?: ""
            val incurredAt = runCatching { LocalDate.parse(incurredAtRaw, dateFormatter) }
                .getOrElse { LocalDate.now() }

            val amount = runCatching { Money(amountRaw) }.getOrElse { Money("0.00") }
            val payerId = payerIdRaw.toLongOrNull()?.let { UserId(it) } ?: UserId(0)
            val splitAmount = amount.value.divide(BigDecimal(group.memberIds.size), 2, RoundingMode.HALF_UP)
            val splits = group.memberIds.map { uid -> ExpenseShare(uid, Money(splitAmount)) }

            val members = group.memberIds.mapNotNull { uid ->
                val user = userRepository.findById(uid) ?: return@mapNotNull null
                mapOf("id" to uid.value, "username" to user.username)
            }

            val result = expenseService.addExpense(
                groupId = group.id,
                description = description,
                amount = amount,
                payerId = payerId,
                splits = splits,
                memberIds = group.memberIds,
                incurredAt = incurredAt,
                currencySymbol = currencySymbol(group.currency),
            )

            result.fold(
                onSuccess = {
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie("Expense added successfully"))
                },
                onFailure = { ex ->
                    val errors = if (ex is ValidationException) ex.errors else listOf(ex.message ?: "Unexpected error")
                    val nonce = CsrfToken.generate()
                    Response(Status.BAD_REQUEST)
                        .cookie(csrfCookie(nonce))
                        .with(htmlLens of AddExpenseViewModel(
                            groupId = idParam,
                            members = members,
                            errors = errors,
                            description = description,
                            amount = amountRaw,
                            incurredAt = incurredAtRaw,
                            csrfToken = nonce,
                        ))
                }
            )
        },

        "/expenses/{id}/edit" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val expenseIdParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val expense = expenseRepository.findById(ExpenseId(expenseIdParam))
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(expense.groupId)
                ?: return@to Response(Status.NOT_FOUND)

            val isAuthorised = currentUserId == expense.payerId || currentUserId == group.creatorId
            if (!isAuthorised) return@to Response(Status.FORBIDDEN)

            val members = group.memberIds.mapNotNull { uid ->
                val user = userRepository.findById(uid) ?: return@mapNotNull null
                val shareAmount = expense.shares.find { it.userId == uid }?.amount?.value?.toPlainString() ?: "0.00"
                mapOf("id" to uid.value, "username" to user.username, "shareAmount" to shareAmount)
            }

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of EditExpenseViewModel(
                    expenseId = expenseIdParam,
                    groupId = group.id.value,
                    members = members,
                    description = expense.description,
                    amount = expense.amount.value.toPlainString(),
                    incurredAt = expense.incurredAt.atZone(java.time.ZoneOffset.UTC).toLocalDate().format(dateFormatter),
                    selectedPayerId = expense.payerId.value,
                    csrfToken = nonce,
                ))
        },

        "/expenses/{id}/edit" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val expenseIdParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val expense = expenseRepository.findById(ExpenseId(expenseIdParam))
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(expense.groupId)
                ?: return@to Response(Status.NOT_FOUND)

            val isAuthorised = currentUserId == expense.payerId || currentUserId == group.creatorId
            if (!isAuthorised) return@to Response(Status.FORBIDDEN)

            val params = parseFormBody(request.bodyString())
            val description = params["description"]?.firstOrNull()?.trim() ?: ""
            val amountRaw = params["amount"]?.firstOrNull()?.trim() ?: "0"
            val payerIdRaw = params["payer_id"]?.firstOrNull()?.trim() ?: ""
            val incurredAtRaw = params["incurred_at"]?.firstOrNull()?.trim() ?: ""
            val incurredAt = runCatching { LocalDate.parse(incurredAtRaw, dateFormatter) }
                .getOrElse { LocalDate.now() }

            val amount = runCatching { Money(amountRaw) }.getOrElse { Money("0.00") }
            val payerId = payerIdRaw.toLongOrNull()?.let { UserId(it) } ?: UserId(0)
            val splitAmount = amount.value.divide(BigDecimal(group.memberIds.size), 2, RoundingMode.HALF_UP)
            val splits = group.memberIds.map { uid -> ExpenseShare(uid, Money(splitAmount)) }

            val members = group.memberIds.mapNotNull { uid ->
                val user = userRepository.findById(uid) ?: return@mapNotNull null
                mapOf("id" to uid.value, "username" to user.username)
            }

            val result = expenseService.editExpense(
                id = ExpenseId(expenseIdParam),
                description = description,
                amount = amount,
                payerId = payerId,
                splits = splits,
                memberIds = group.memberIds,
                incurredAt = incurredAt,
                currencySymbol = currencySymbol(group.currency),
            )

            result.fold(
                onSuccess = {
                    Response(Status.FOUND)
                        .header("Location", "/group/${group.id.value}")
                        .cookie(flashCookie("Expense updated successfully"))
                },
                onFailure = { ex ->
                    val errors = if (ex is ValidationException) ex.errors else listOf(ex.message ?: "Unexpected error")
                    val nonce = CsrfToken.generate()
                    Response(Status.BAD_REQUEST)
                        .cookie(csrfCookie(nonce))
                        .with(htmlLens of EditExpenseViewModel(
                            expenseId = expenseIdParam,
                            groupId = group.id.value,
                            members = members,
                            errors = errors,
                            description = description,
                            amount = amountRaw,
                            incurredAt = incurredAtRaw,
                            selectedPayerId = payerId.value,
                            csrfToken = nonce,
                        ))
                }
            )
        },

        "/expenses/{id}/delete" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val expenseIdParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val expense = expenseRepository.findById(ExpenseId(expenseIdParam))
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(expense.groupId)
                ?: return@to Response(Status.NOT_FOUND)

            val isAuthorised = currentUserId == expense.payerId || currentUserId == group.creatorId
            if (!isAuthorised) return@to Response(Status.FORBIDDEN)

            expenseService.deleteExpense(ExpenseId(expenseIdParam))

            Response(Status.FOUND)
                .header("Location", "/group/${group.id.value}")
                .cookie(flashCookie("Expense deleted"))
        },
    )
}
