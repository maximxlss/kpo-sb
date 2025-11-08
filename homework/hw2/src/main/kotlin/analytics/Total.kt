package ru.msk.xls.kpo.bank.analytics

import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.domain.OperationType

data class Total(
    val income: Double,
    val spending: Double,
) {
    val delta: Double
        get() = income - spending

    fun addOperation(operation: Operation): Total =
        when (operation.type) {
            OperationType.INCOME -> Total(income + operation.absoluteValue, spending)
            OperationType.SPENDING -> Total(income, spending + operation.absoluteValue)
        }
}