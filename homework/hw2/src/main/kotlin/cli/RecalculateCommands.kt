package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.commands.RecalculateBalanceCommand
import ru.msk.xls.kpo.bank.observers.CommandAuditLogger
import ru.msk.xls.kpo.bank.observers.ObservableCommand
import ru.msk.xls.kpo.bank.observers.StatisticsCollector
import ru.msk.xls.kpo.bank.services.AutoRecalculationStrategy
import ru.msk.xls.kpo.bank.services.CustomStartingBalanceStrategy

class RecalculateBalanceCommandCli : CliktCommand("recalculate") {
    override fun help(context: Context) = "Recalculate account balance from operations"
    override fun run() = Unit
}

class ManualRecalculateCommand : CliktCommand("custom"), KoinComponent {
    override fun help(context: Context) = "Recalculate balance from a custom starting balance"

    private val accountId by option("--account", "-a", help = "Account ID").int().required()
    private val startingBalance by option("--starting-balance", "-b", help = "Starting balance to use").double()
        .required()
    private val apply by option("--apply", help = "Apply the calculated balance").flag(default = false)

    private val auditLogger: CommandAuditLogger by inject()
    private val statisticsCollector: StatisticsCollector by inject()

    override fun run() {
        try {
            val command = RecalculateBalanceCommand(
                accountId = accountId,
                strategy = CustomStartingBalanceStrategy(startingBalance),
                applyChanges = apply
            )

            val observableCommand = ObservableCommand("RecalculateBalance (custom)") { command.execute() }
            observableCommand.addObserver(auditLogger)
            observableCommand.addObserver(statisticsCollector)

            val result = observableCommand.execute()

            if (apply) {
                echo("Balance Recalculation Applied:")
                echo("─".repeat(60))
                echo("Account: ${result.accountName} (ID: ${result.accountId})")
                echo("Starting balance:      ${String.format("%10.2f", startingBalance)}")
                echo("+ All operations")
                echo("New balance:           ${String.format("%10.2f", result.calculatedBalance)}")
                echo("")
                echo("Previous balance:      ${String.format("%10.2f", result.oldBalance)}")
                if (result.difference != 0.0) {
                    echo("Adjustment applied:    ${String.format("%10.2f", result.difference)}")
                }
                echo("─".repeat(60))
                echo("✓ Balance updated successfully")
            } else {
                echo("Balance Calculation Preview (Not Applied):")
                echo("─".repeat(60))
                echo("Account: ${result.accountName} (ID: ${result.accountId})")
                echo("Starting balance:      ${String.format("%10.2f", startingBalance)}")
                echo("+ All operations")
                echo("Calculated balance:    ${String.format("%10.2f", result.calculatedBalance)}")
                echo("")
                echo("Current balance:       ${String.format("%10.2f", result.oldBalance)}")
                if (result.difference != 0.0) {
                    echo("Difference:            ${String.format("%10.2f", result.difference)}")
                }
                echo("─".repeat(60))
                echo("ℹ Add --apply flag to update the balance")
            }
        } catch (e: Exception) {
            echo("✗ Failed to recalculate balance: ${e.message}", err = true)
        }
    }
}

class AutoRecalculateCommand : CliktCommand("auto"), KoinComponent {
    override fun help(context: Context) = "Recalculate balance from zero plus all operations"

    private val accountId by option("--account", "-a", help = "Account ID").int().required()

    private val auditLogger: CommandAuditLogger by inject()
    private val statisticsCollector: StatisticsCollector by inject()

    override fun run() {
        try {
            val command = RecalculateBalanceCommand(
                accountId = accountId,
                strategy = AutoRecalculationStrategy(),
                applyChanges = true
            )

            val observableCommand = ObservableCommand("RecalculateBalance (auto)") { command.execute() }
            observableCommand.addObserver(auditLogger)
            observableCommand.addObserver(statisticsCollector)

            val result = observableCommand.execute()

            echo("Balance Recalculation Applied:")
            echo("─".repeat(60))
            echo("Account: ${result.accountName} (ID: ${result.accountId})")
            echo("Old balance:  ${String.format("%10.2f", result.oldBalance)}")
            echo("New balance:  ${String.format("%10.2f", result.calculatedBalance)}")
            echo("Adjustment:   ${String.format("%10.2f", result.difference)}")
            echo("─".repeat(60))
            if (result.difference != 0.0) {
                echo("✓ Balance updated successfully")
            } else {
                echo("✓ Balance was already correct")
            }
        } catch (e: Exception) {
            echo("✗ Failed to recalculate balance: ${e.message}", err = true)
        }
    }
}
