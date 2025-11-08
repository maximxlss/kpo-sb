package ru.msk.xls.kpo.bank.commands.decorators

import ru.msk.xls.kpo.bank.commands.Command

// PATTERN: Decorator - adds logging behavior to commands
class LoggingDecorator<T>(
    command: Command<T>,
    private val commandName: String = "Command",
    private val onStart: (String) -> Unit = { name -> println("▶ Starting $name...") },
    private val onComplete: (String, T) -> Unit = { name, _ -> println("✓ $name completed") }
) : CommandDecorator<T>(command) {
    override fun execute(): T {
        onStart(commandName)
        val result = command.execute()
        onComplete(commandName, result)
        return result
    }
}