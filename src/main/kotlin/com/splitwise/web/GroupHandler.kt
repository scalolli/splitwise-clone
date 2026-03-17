package com.splitwise.web

import com.splitwise.domain.GroupId
import com.splitwise.persistence.ExpenseRepository
import com.splitwise.persistence.GroupRepository
import com.splitwise.persistence.SettlementRepository
import com.splitwise.persistence.UserRepository
import com.splitwise.service.BalanceService
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
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
) : ViewModel {
    override fun template() = "group"
}

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

    return routes(
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

            Response(Status.OK).with(
                htmlLens of GroupViewModel(
                    groupId = group.id.value,
                    groupName = group.name,
                    members = members,
                    expenses = expenses,
                    balances = balances,
                )
            )
        },
    )
}
