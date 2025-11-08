package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.analytics.CalculateTotalsCommand
import ru.msk.xls.kpo.bank.commands.decorators.TimingDecorator
import ru.msk.xls.kpo.bank.observers.CommandAuditLogger
import ru.msk.xls.kpo.bank.observers.ObservableCommand
import ru.msk.xls.kpo.bank.observers.StatisticsCollector

class TotalsCommand : CliktCommand("totals"), KoinComponent {
    override fun help(context: Context) = "Calculate total income, spending, and current balance"

    private val auditLogger: CommandAuditLogger by inject()
    private val statisticsCollector: StatisticsCollector by inject()

    private val accountId by option("--account", "-a", help = "Filter by account ID").int().required()
    private val timing by option("--timing", help = "Show execution time").flag(default = false)

    override fun run() {
        val baseCommand = CalculateTotalsCommand(accountId)
        val command = if (timing) {
            TimingDecorator(baseCommand, "Calculate Totals") { name, ms ->
                echo("⏱ $name executed in ${ms}ms")
            }
        } else {
            baseCommand
        }

        val observableCommand = ObservableCommand("CalculateTotals") { command.execute() }
        observableCommand.addObserver(auditLogger)
        observableCommand.addObserver(statisticsCollector)

        val totals = try {
            observableCommand.execute()
        } catch (e: EntityNotFoundException) {
            echo("✗ Failed: ${e.entity} with id ${e.id} not found", err = true)
            return
        }

        echo("Financial Summary:")
        echo("─".repeat(60))
        echo("Total Income:    ${String.format("%10.2f", totals.total.income)}")
        echo("Total Spending:  ${String.format("%10.2f", totals.total.spending)}")
        echo("Total delta:     ${String.format("%10.2f", totals.total.delta)}")
        echo("─".repeat(60))
    }
}
