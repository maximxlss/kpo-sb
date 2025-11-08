package ru.msk.xls.kpo.bank.commands

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.domain.Operations
import ru.msk.xls.kpo.bank.services.RecalculationResult
import ru.msk.xls.kpo.bank.services.RecalculationStrategy

// PATTERN: Command - encapsulates balance recalculation as executable object
data class RecalculateBalanceCommand(
    val accountId: Int,
    val strategy: RecalculationStrategy,
    val applyChanges: Boolean
) : Command<RecalculationResult>, KoinComponent {
    private val database: Database by inject()

    override fun execute(): RecalculationResult {
        return transaction(database) {
            val account = try {
                BankAccount[accountId]
            } catch (e: EntityNotFoundException) {
                throw IllegalArgumentException("Account with ID $accountId not found")
            }

            val operations = Operation.find { Operations.bankAccount eq account.id }.toList()

            val result = strategy.calculateBalance(
                accountId = account.id.value,
                accountName = account.name,
                currentBalance = account.balance,
                operations = operations
            )

            if (applyChanges) {
                account.balance = result.calculatedBalance
            }

            result
        }
    }
}
