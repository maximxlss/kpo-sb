package ru.msk.xls.kpo.bank.services

import ru.msk.xls.kpo.bank.domain.Operation

data class RecalculationResult(
    val accountId: Int,
    val accountName: String,
    val oldBalance: Double,
    val calculatedBalance: Double,
    val difference: Double
)

// PATTERN: Strategy - defines algorithm family for balance recalculation
interface RecalculationStrategy {
    fun calculateBalance(
        accountId: Int,
        accountName: String,
        currentBalance: Double,
        operations: List<Operation>
    ): RecalculationResult
}
