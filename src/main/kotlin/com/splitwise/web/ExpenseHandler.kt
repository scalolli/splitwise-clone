package com.splitwise.web

import com.splitwise.domain.ExpenseShare
import com.splitwise.domain.GroupId
import com.splitwise.domain.Money
import com.splitwise.domain.UserId
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.ExpenseService
import com.splitwise.service.ValidationException
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
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "add_expense"
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
            val splitUserIds = params["split_user_id"] ?: emptyList()
            val splitAmounts = params["split_amount"] ?: emptyList()

            val amount = runCatching { Money(amountRaw) }.getOrElse { Money("0.00") }
            val payerId = payerIdRaw.toLongOrNull()?.let { UserId(it) } ?: UserId(0)
            val splits = splitUserIds.zip(splitAmounts).mapNotNull { (uid, amt) ->
                val userId = uid.toLongOrNull() ?: return@mapNotNull null
                val shareAmount = runCatching { Money(amt) }.getOrNull() ?: return@mapNotNull null
                ExpenseShare(UserId(userId), shareAmount)
            }

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
                            csrfToken = nonce,
                        ))
                }
            )
        },
    )
}
