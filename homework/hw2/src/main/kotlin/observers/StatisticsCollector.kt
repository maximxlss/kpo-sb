package ru.msk.xls.kpo.bank.observers

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.CommandExecution

// PATTERN: Observer - concrete observer that collects command execution statistics
@Single
class StatisticsCollector : CommandObserver, KoinComponent {
    private val database: Database by inject()

    override fun onCommandExecuted(commandName: String, executionTimeMs: Long, success: Boolean) {
        try {
            transaction(database) {
                CommandExecution.new {
                    this.commandName = commandName
                    this.executionTimeMs = executionTimeMs
                    this.success = success
                    this.timestamp = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to save statistics: ${e.message}")
        }
    }

    fun getStatistics(): Map<String, CommandStats> {
        return try {
            transaction(database) {
                val executions = CommandExecution.all()

                executions.groupBy { it.commandName }.mapValues { (_, commandExecutions) ->
                    val count = commandExecutions.size.toLong()
                    val totalTime = commandExecutions.sumOf { it.executionTimeMs }
                    val failures = commandExecutions.count { !it.success }.toLong()

                    CommandStats(
                        executionCount = count,
                        totalExecutionTime = totalTime,
                        averageExecutionTime = if (count > 0) totalTime / count else 0,
                        failureCount = failures,
                        successRate = if (count > 0) ((count - failures).toDouble() / count * 100) else 0.0
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to get statistics: ${e.message}")
            emptyMap()
        }
    }

    fun printStatistics() {
        println("\n=== Command Execution Statistics ===")
        println("─".repeat(80))
        val stats = getStatistics()
        if (stats.isEmpty()) {
            println("No command executions recorded.")
        } else {
            stats.forEach { (name, stats) ->
                println("$name:")
                println("  Executions: ${stats.executionCount}")
                println("  Avg Time: ${stats.averageExecutionTime}ms")
                println("  Success Rate: ${String.format("%.1f", stats.successRate)}%")
                if (stats.failureCount > 0) {
                    println("  Failures: ${stats.failureCount}")
                }
            }
        }
        println("─".repeat(80))
    }

    fun clearStatistics() {
        try {
            transaction(database) {
                CommandExecution.all().forEach { it.delete() }
            }
        } catch (e: Exception) {
            System.err.println("Failed to clear statistics: ${e.message}")
        }
    }
}

data class CommandStats(
    val executionCount: Long,
    val totalExecutionTime: Long,
    val averageExecutionTime: Long,
    val failureCount: Long,
    val successRate: Double
)
