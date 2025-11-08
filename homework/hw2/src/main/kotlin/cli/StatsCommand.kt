package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.CommandExecution
import ru.msk.xls.kpo.bank.observers.StatisticsCollector

class StatsCommand : CliktCommand("stats"), KoinComponent {
    override fun help(context: Context) = "Show command execution statistics"

    private val statisticsCollector: StatisticsCollector by inject()

    override fun run() {
        statisticsCollector.printStatistics()
    }
}

class ClearStatsCommand : CliktCommand("clear-stats"), KoinComponent {
    override fun help(context: Context) = "Clear all command execution statistics"

    private val database: Database by inject()

    override fun run() {
        transaction(database) {
            val count = CommandExecution.all().count()
            CommandExecution.all().forEach { it.delete() }
            echo("✓ Cleared $count command execution record(s)")
        }
    }
}
