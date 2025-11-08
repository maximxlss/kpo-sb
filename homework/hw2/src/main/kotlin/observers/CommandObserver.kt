package ru.msk.xls.kpo.bank.observers

// PATTERN: Observer - defines observer interface for command execution events
interface CommandObserver {
    fun onCommandExecuted(commandName: String, executionTimeMs: Long, success: Boolean)
}