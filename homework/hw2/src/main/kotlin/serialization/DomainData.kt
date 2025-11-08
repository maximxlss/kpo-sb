package ru.msk.xls.kpo.bank.serialization

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.msk.xls.kpo.bank.domain.*

interface SaveToDatabase {
    fun saveToDatabase()
}

@Serializable
data class BankAccountData(
    val id: Int,
    val name: String,
    val balance: Double,
) : SaveToDatabase {
    constructor(dao: BankAccount) : this(
        id = dao.id.value,
        name = dao.name,
        balance = dao.balance,
    )

    override fun saveToDatabase() {
        BankAccount.new(id) {
            name = this@BankAccountData.name
            balance = this@BankAccountData.balance
        }
    }
}

@Serializable
data class CategoryData(
    val id: Int,
    val type: CategoryType,
    val name: String,
) : SaveToDatabase {
    constructor(dao: Category) : this(
        id = dao.id.value,
        type = dao.type,
        name = dao.name,
    )

    override fun saveToDatabase() {
        Category.new(id) {
            type = this@CategoryData.type
            name = this@CategoryData.name
        }
    }
}

@Serializable
data class OperationData(
    val id: Int,
    var type: OperationType,
    var bankAccountId: Int,
    var absoluteValue: Double,
    var date: LocalDateTime,
    var description: String,
    var categoryId: Int,
) : SaveToDatabase {
    constructor(dao: Operation) : this(
        id = dao.id.value,
        type = dao.type,
        bankAccountId = dao.bankAccount.id.value,
        absoluteValue = dao.absoluteValue,
        date = dao.date,
        description = dao.description,
        categoryId = dao.category.id.value,
    )

    override fun saveToDatabase() {
        Operation.new(id) {
            type = this@OperationData.type
            bankAccount = BankAccount[bankAccountId]
            absoluteValue = this@OperationData.absoluteValue
            date = this@OperationData.date
            description = this@OperationData.description
            category = Category[categoryId]
        }
    }
}

@Serializable
data class CommandExecutionData(
    val id: Int,
    val commandName: String,
    val executionTimeMs: Long,
    val success: Boolean,
    val timestamp: Long,
) : SaveToDatabase {
    constructor(dao: CommandExecution) : this(
        id = dao.id.value,
        commandName = dao.commandName,
        executionTimeMs = dao.executionTimeMs,
        success = dao.success,
        timestamp = dao.timestamp,
    )

    override fun saveToDatabase() {
        CommandExecution.new(id) {
            commandName = this@CommandExecutionData.commandName
            executionTimeMs = this@CommandExecutionData.executionTimeMs
            success = this@CommandExecutionData.success
            timestamp = this@CommandExecutionData.timestamp
        }
    }
}