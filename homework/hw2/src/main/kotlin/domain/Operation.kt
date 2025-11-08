package ru.msk.xls.kpo.bank.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.datetime

enum class OperationType {
    INCOME, SPENDING
}

object Operations : IntIdTable("operations") {
    val type = enumeration<OperationType>("type")
    val bankAccount = reference("bank_account", BankAccounts)
    val absoluteValue = double("absolute_value")
    val date = datetime("date")
    val description = text("description")
    val category = reference("category", Categories)
}

class Operation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Operation>(Operations)

    var type by Operations.type
    var bankAccount by BankAccount referencedOn Operations.bankAccount
    var absoluteValue by Operations.absoluteValue
    var date by Operations.date
    var description by Operations.description
    var category by Category referencedOn Operations.category
}