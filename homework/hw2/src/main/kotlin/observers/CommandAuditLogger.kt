package ru.msk.xls.kpo.bank.observers

import org.koin.core.annotation.Single
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// PATTERN: Observer - concrete observer that logs command executions to file
@Single
class CommandAuditLogger(
    private val logFilePath: String = "command_audit.log"
) : CommandObserver {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun onCommandExecuted(commandName: String, executionTimeMs: Long, success: Boolean) {
        try {
            val timestamp = LocalDateTime.now().format(timeFormatter)
            val status = if (success) "SUCCESS" else "FAILED"
            val logEntry = "[$timestamp] $status - $commandName (${executionTimeMs}ms)\n"
            File(logFilePath).appendText(logEntry)
        } catch (e: Exception) {
            System.err.println("Failed to write audit log: ${e.message}")
        }
    }
}