package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.*
import ru.msk.xls.kpo.bank.factories.OperationFactory

class AddOperationCommand : CliktCommand("add"), KoinComponent {
    override fun help(context: Context) = "Add a new operation"
    private val operationFactory: OperationFactory by inject()

    private val accountId by option("--account", "-a", help = "Account ID").int().required()
    private val categoryId by option("--category", "-c", help = "Category ID").int().required()
    private val amount by option("--amount", "-m", help = "Operation amount").double().required()
    private val description by option("--description", "-d", help = "Description").required()
    private val type by option("--type", "-t", help = "Type (income/spending)").enum<OperationType>().required()

    override fun run() {
        operationFactory.create(type, accountId, amount, description, categoryId).fold(
            onSuccess = { operation ->
                echo("✓ Operation added successfully!")
                echo("  ID: ${operation.id.value}")
                echo("  Account: ${operation.bankAccount.name}")
                echo("  Category: ${operation.category.name} (${operation.category.type})")
                echo("  Type: ${operation.type}")
                echo("  Amount: ${operation.absoluteValue}")
                echo("  Description: ${operation.description}")
                echo("  New balance: ${operation.bankAccount.balance}")
            },
            onFailure = { error ->
                echo("✗ Failed to add operation: ${error.message}", err = true)
            }
        )
    }
}

class ListOperationsCommand : CliktCommand("list"), KoinComponent {
    override fun help(context: Context) = "List all operations"
    private val database: Database by inject()

    private val accountId by option("--account", "-a", help = "Filter by account ID").int()

    override fun run() {
        transaction(database) {
            val operations = if (accountId != null) {
                val account = try {
                    BankAccount[accountId!!]
                } catch (e: EntityNotFoundException) {
                    echo("✗ Account with ID $accountId not found", err = true)
                    return@transaction
                }
                Operation.find { Operations.bankAccount eq account.id }
            } else {
                Operation.all()
            }

            if (operations.empty()) {
                echo("No operations found. Add one with 'operation add'")
                return@transaction
            }

            echo("Operations:")
            echo("─".repeat(80))
            operations.forEach { op ->
                echo("ID: ${op.id.value} | ${op.bankAccount.name} | ${op.category.name} (${op.category.type}) | ${op.absoluteValue} | ${op.description}")
            }
            echo("─".repeat(80))
            echo("Total: ${operations.count()} operations")
        }
    }
}

class EditOperationCommand : CliktCommand("edit"), KoinComponent {
    override fun help(context: Context) = "Edit an existing operation"
    private val database: Database by inject()

    private val operationId by option("--id", help = "Operation ID").int().required()
    private val description by option("--description", "-d", help = "New description")
    private val amount by option("--amount", "-m", help = "New amount").double()
    private val categoryId by option("--category", "-c", help = "New category ID").int()

    override fun run() {
        if (description == null && amount == null && categoryId == null) {
            echo("✗ Provide --description, --amount, or --category to update", err = true)
            return
        }

        transaction(database) {
            val operation = try {
                Operation[operationId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Operation with ID $operationId not found", err = true)
                return@transaction
            }

            val oldAmount = operation.absoluteValue
            val account = operation.bankAccount

            description?.let { operation.description = it }

            amount?.let {
                if (it != oldAmount) {
                    val difference = it - oldAmount
                    when (operation.type) {
                        OperationType.INCOME -> account.balance += difference
                        OperationType.SPENDING -> account.balance -= difference
                    }
                    operation.absoluteValue = it
                }
            }

            categoryId?.let {
                val category = try {
                    Category[it]
                } catch (e: EntityNotFoundException) {
                    echo("✗ Category with ID $it not found", err = true)
                    return@transaction
                }
                operation.category = category
            }

            echo("✓ Operation updated successfully!")
            echo("  ID: ${operation.id.value}")
            echo("  Account: ${operation.bankAccount.name}")
            echo("  Category: ${operation.category.name}")
            echo("  Type: ${operation.type}")
            echo("  Amount: ${operation.absoluteValue}")
            echo("  Description: ${operation.description}")
            echo("  Account balance: ${account.balance}")
        }
    }
}

class DeleteOperationCommand : CliktCommand("delete"), KoinComponent {
    override fun help(context: Context) = "Delete an operation"
    private val database: Database by inject()

    private val operationId by option("--id", help = "Operation ID").int().required()

    override fun run() {
        transaction(database) {
            val operation = try {
                Operation[operationId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Operation with ID $operationId not found", err = true)
                return@transaction
            }

            val account = operation.bankAccount
            when (operation.type) {
                OperationType.INCOME -> account.balance -= operation.absoluteValue
                OperationType.SPENDING -> account.balance += operation.absoluteValue
            }

            operation.delete()

            echo("✓ Operation (ID: $operationId) deleted successfully")
            echo("  Account '${account.name}' new balance: ${account.balance}")
        }
    }
}

