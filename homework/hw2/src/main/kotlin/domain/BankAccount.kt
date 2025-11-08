package ru.msk.xls.kpo.bank.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object BankAccounts : IntIdTable("bank_accounts") {
    val name = varchar("name", 128)
    val balance = double("balance")
}

class BankAccount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankAccount>(BankAccounts)

    var name by BankAccounts.name
    var balance by BankAccounts.balance
}

