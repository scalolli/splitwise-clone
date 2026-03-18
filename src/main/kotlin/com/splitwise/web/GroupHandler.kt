package com.splitwise.web

import com.splitwise.domain.GroupId
import com.splitwise.domain.UserId
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.BalanceService
import com.splitwise.service.GroupService
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

data class GroupViewModel(
    val groupId: Long,
    val groupName: String,
    val members: List<Map<String, Any?>>,
    val expenses: List<Map<String, Any?>>,
    val balances: List<Map<String, Any?>>,
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "group"
}

data class CreateGroupViewModel(
    val errors: List<String> = emptyList(),
    val name: String = "",
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "create_group"
}

data class EditGroupViewModel(
    val groupId: Long,
    val groupName: String,
    val members: List<Map<String, Any?>>,
    val errors: List<String> = emptyList(),
    val csrfToken: String = "",
) : ViewModel {
    override fun template() = "edit_group"
}

private fun flashCookie(message: String) = Cookie(
    name = "flash",
    value = message,
    maxAge = 60,
    path = "/",
    httpOnly = true,
    secure = true,
    sameSite = SameSite.Strict,
)

private fun csrfCookie(nonce: String) = Cookie(
    name = "csrf",
    value = nonce,
    maxAge = 3600,
    path = "/",
    httpOnly = true,
    secure = true,
    sameSite = SameSite.Strict,
)

fun groupHandler(
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    expenseRepository: ExpenseRepository,
    settlementRepository: SettlementRepository,
    sessionToken: SessionToken,
): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
    val balanceService = BalanceService(expenseRepository, settlementRepository)
    val groupService = GroupService(groupRepository, userRepository)

    return routes(
        "/group/create" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of CreateGroupViewModel(csrfToken = nonce))
        },

        "/group/create" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")

            val params = parseFormBody(request.bodyString())
            val name = params["name"]?.firstOrNull()?.trim() ?: ""

            if (name.isEmpty()) {
                val nonce = CsrfToken.generate()
                return@to Response(Status.BAD_REQUEST)
                    .cookie(csrfCookie(nonce))
                    .with(htmlLens of CreateGroupViewModel(
                        errors = listOf("Group name is required"),
                        name = name,
                        csrfToken = nonce,
                    ))
            }

            val group = groupService.createGroup(name = name, creatorId = currentUserId)
            Response(Status.FOUND)
                .header("Location", "/group/${group.id.value}")
                .cookie(flashCookie("Group created successfully"))
        },

        "/group/{id}/edit" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)

            if (group.creatorId != currentUserId) {
                return@to Response(Status.FOUND)
                    .header("Location", "/group/$idParam")
                    .cookie(flashCookie("You do not have permission to edit this group"))
            }

            val userMap = group.memberIds
                .mapNotNull { uid -> userRepository.findById(uid)?.let { uid to it } }
                .toMap()
            val members = group.memberIds.map { uid ->
                mapOf("id" to uid.value, "username" to (userMap[uid]?.username ?: uid.value.toString()))
            }

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(htmlLens of EditGroupViewModel(
                    groupId = group.id.value,
                    groupName = group.name,
                    members = members,
                    csrfToken = nonce,
                ))
        },

        "/group/{id}/edit" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val params = parseFormBody(request.bodyString())
            val name = params["name"]?.firstOrNull()?.trim() ?: ""

            groupService.editGroup(GroupId(idParam), name, currentUserId).fold(
                onSuccess = {
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie("Group updated successfully"))
                },
                onFailure = { error ->
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie(error.message ?: "Could not update group"))
                },
            )
        },

        "/group/{id}/add_member" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val params = parseFormBody(request.bodyString())
            val username = params["username"]?.firstOrNull()?.trim() ?: ""

            groupService.addMember(GroupId(idParam), username, currentUserId).fold(
                onSuccess = {
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie("Member added successfully"))
                },
                onFailure = { error ->
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie(error.message ?: "Could not add member"))
                },
            )
        },

        "/group/{id}/remove_member/{userId}" bind POST to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)
            val targetUserId = request.path("userId")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            groupService.removeMember(GroupId(idParam), UserId(targetUserId), currentUserId).fold(
                onSuccess = {
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie("Member removed successfully"))
                },
                onFailure = { error ->
                    Response(Status.FOUND)
                        .header("Location", "/group/$idParam")
                        .cookie(flashCookie(error.message ?: "Could not remove member"))
                },
            )
        },

        "/group/{id}" bind GET to { request ->
            val currentUserId = request.sessionUserId(sessionToken)
                ?: return@to Response(Status.FOUND).header("Location", "/login")
            val idParam = request.path("id")?.toLongOrNull()
                ?: return@to Response(Status.NOT_FOUND)

            val group = groupRepository.findById(GroupId(idParam))
                ?: return@to Response(Status.NOT_FOUND)

            if (group.memberIds.none { it == currentUserId }) {
                return@to Response(Status.FORBIDDEN)
            }

            val userMap = group.memberIds
                .mapNotNull { userId -> userRepository.findById(userId)?.let { userId to it } }
                .toMap()

            val members = group.memberIds.map { uid ->
                mapOf("id" to uid.value, "username" to (userMap[uid]?.username ?: uid.value.toString()))
            }

            val expenses = expenseRepository.findByGroup(group.id).map { expense ->
                val payerName = userMap[expense.payerId]?.username ?: expense.payerId.value.toString()
                mapOf(
                    "id" to expense.id.value,
                    "description" to expense.description,
                    "amount" to expense.amount.value.toPlainString(),
                    "payer" to payerName,
                )
            }

            val balances = balanceService.balancesForGroup(group.id).map { balance ->
                val debtorName = userMap[balance.debtorId]?.username ?: balance.debtorId.value.toString()
                val creditorName = userMap[balance.creditorId]?.username ?: balance.creditorId.value.toString()
                mapOf(
                    "debtor" to debtorName,
                    "creditor" to creditorName,
                    "amount" to balance.amount.value.toPlainString(),
                )
            }

            val nonce = CsrfToken.generate()
            Response(Status.OK)
                .cookie(csrfCookie(nonce))
                .with(
                    htmlLens of GroupViewModel(
                        groupId = group.id.value,
                        groupName = group.name,
                        members = members,
                        expenses = expenses,
                        balances = balances,
                        csrfToken = nonce,
                    )
                )
        },
    )
}
