package ru.msk.xls.kpo.bank.analytics

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.commands.Command
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.domain.Operations

data class CalculateTotalsResult(
    val total: Total,
    val byCategory: Map<Category, Total>,
)

// PATTERN: Command - encapsulates totals calculation as executable object
data class CalculateTotalsCommand(
    val accountId: Int,
    val periodBegin: LocalDateTime? = null,
    val periodEnd: LocalDateTime? = null,
    val categoryIds: List<Int>? = null,
) : Command<CalculateTotalsResult>, KoinComponent {
    private val database: Database by inject()

    override fun execute(): CalculateTotalsResult =
        transaction(database) {
            val account = BankAccount[accountId]
            var condition = Operations.bankAccount eq account.id

            periodBegin?.let { condition = condition and Operations.date.greaterEq(it) }
            periodEnd?.let { condition = condition and Operations.date.lessEq(it) }
            categoryIds?.let { condition = condition and Operations.category.inList(it) }

            val operations = Operation.find(condition)
            var total = Total(0.0, 0.0)
            val byCategory = mutableMapOf<Category, Total>()

            for (operation in operations) {
                total = total.addOperation(operation)
                val category = operation.category
                val categoryTotal = byCategory[category] ?: Total(0.0, 0.0)
                byCategory[category] = categoryTotal.addOperation(operation)
            }

            CalculateTotalsResult(total, byCategory)
        }
}