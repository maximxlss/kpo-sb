package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.factories.BankAccountFactory

class CreateAccountCommand : CliktCommand("create"), KoinComponent {
    override fun help(context: Context) = "Create a new bank account"
    private val accountFactory: BankAccountFactory by inject()

    private val name by option("--name", "-n", help = "Account name").required()
    private val balance by option("--balance", "-b", help = "Initial balance").double().required()

    override fun run() {
        accountFactory.create(name, balance).fold(
            onSuccess = { account ->
                echo("✓ Account created successfully!")
                echo("  ID: ${account.id.value}")
                echo("  Name: ${account.name}")
                echo("  Balance: ${account.balance}")
            },
            onFailure = { error ->
                echo("✗ Failed to create account: ${error.message}", err = true)
            }
        )
    }
}

class ListAccountsCommand : CliktCommand("list"), KoinComponent {
    override fun help(context: Context) = "List all bank accounts"
    private val database: Database by inject()

    override fun run() {
        transaction(database) {
            val accounts = BankAccount.all()

            if (accounts.empty()) {
                echo("No accounts found. Create one with 'account create'")
                return@transaction
            }

            echo("Bank Accounts:")
            echo("─".repeat(60))
            accounts.forEach { account ->
                echo("ID: ${account.id.value} | ${account.name} | Balance: ${account.balance}")
            }
            echo("─".repeat(60))
            echo("Total: ${accounts.count()} accounts")
        }
    }
}

class EditAccountCommand : CliktCommand("edit"), KoinComponent {
    override fun help(context: Context) = "Edit an existing bank account"
    private val database: Database by inject()

    private val accountId by option("--id", help = "Account ID").int().required()
    private val name by option("--name", "-n", help = "New account name")
    private val balance by option("--balance", "-b", help = "New balance").double()

    override fun run() {
        if (name == null && balance == null) {
            echo("✗ Provide --name or --balance to update", err = true)
            return
        }

        transaction(database) {
            val account = try {
                BankAccount[accountId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Account with ID $accountId not found", err = true)
                return@transaction
            }

            name?.let { account.name = it }
            balance?.let { account.balance = it }

            echo("✓ Account updated successfully!")
            echo("  ID: ${account.id.value}")
            echo("  Name: ${account.name}")
            echo("  Balance: ${account.balance}")
        }
    }
}

class DeleteAccountCommand : CliktCommand("delete"), KoinComponent {
    override fun help(context: Context) = "Delete a bank account"
    private val database: Database by inject()

    private val accountId by option("--id", help = "Account ID").int().required()

    override fun run() {
        transaction(database) {
            val account = try {
                BankAccount[accountId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Account with ID $accountId not found", err = true)
                return@transaction
            }

            val accountName = account.name
            account.delete()

            echo("✓ Account '$accountName' (ID: $accountId) deleted successfully")
        }
    }
}

