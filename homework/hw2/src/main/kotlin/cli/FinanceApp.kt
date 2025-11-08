package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class FinanceApp : CliktCommand("bank") {
    override fun help(context: Context) = "Personal finance tracking application"
    override fun run() {
        echo("Welcome to HSE-Bank Finance Tracker!")
        echo("Use --help to see available commands")
    }
}

class AccountCommands : CliktCommand("account") {
    override fun help(context: Context) = "Manage bank accounts"
    override fun run() = Unit

    init {
        subcommands(
            CreateAccountCommand(),
            ListAccountsCommand(),
            EditAccountCommand(),
            DeleteAccountCommand(),
            RecalculateBalanceCommandCli().subcommands(
                ManualRecalculateCommand(),
                AutoRecalculateCommand()
            )
        )
    }
}

class CategoryCommands : CliktCommand("category") {
    override fun help(context: Context) = "Manage categories"
    override fun run() = Unit

    init {
        subcommands(
            CreateCategoryCommand(),
            ListCategoriesCommand(),
            EditCategoryCommand(),
            DeleteCategoryCommand()
        )
    }
}

class OperationCommands : CliktCommand("operation") {
    override fun help(context: Context) = "Manage income and spending operations"
    override fun run() = Unit

    init {
        subcommands(
            AddOperationCommand(),
            ListOperationsCommand(),
            EditOperationCommand(),
            DeleteOperationCommand()
        )
    }
}

class AnalyticsCommands : CliktCommand("analytics") {
    override fun help(context: Context) = "View financial analytics"
    override fun run() = Unit

    init {
        subcommands(TotalsCommand(), StatsCommand(), ClearStatsCommand())
    }
}

class DataCommands : CliktCommand("data") {
    override fun help(context: Context) = "Import and export data"
    override fun run() = Unit

    init {
        subcommands(ImportCommand(), ExportCommand())
    }
}

fun configureCliApp(): FinanceApp {
    return FinanceApp().subcommands(
        AccountCommands(),
        CategoryCommands(),
        OperationCommands(),
        AnalyticsCommands(),
        DataCommands()
    )
}
