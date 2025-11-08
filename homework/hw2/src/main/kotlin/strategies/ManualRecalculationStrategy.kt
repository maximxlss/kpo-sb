package ru.msk.xls.kpo.bank.services

import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.domain.OperationType

// PATTERN: Strategy - custom starting balance recalculation
class CustomStartingBalanceStrategy(
    private val startingBalance: Double
) : RecalculationStrategy {
    override fun calculateBalance(
        accountId: Int,
        accountName: String,
        currentBalance: Double,
        operations: List<Operation>
    ): RecalculationResult {
        val calculatedBalance = operations.fold(startingBalance) { acc, op ->
            when (op.type) {
                OperationType.INCOME -> acc + op.absoluteValue
                OperationType.SPENDING -> acc - op.absoluteValue
            }
        }

        return RecalculationResult(
            accountId = accountId,
            accountName = accountName,
            oldBalance = currentBalance,
            calculatedBalance = calculatedBalance,
            difference = calculatedBalance - currentBalance
        )
    }
}
