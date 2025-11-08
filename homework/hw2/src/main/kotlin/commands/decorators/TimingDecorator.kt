package ru.msk.xls.kpo.bank.commands.decorators

import ru.msk.xls.kpo.bank.commands.Command
import kotlin.time.measureTimedValue

// PATTERN: Decorator - adds timing measurement to commands
class TimingDecorator<T>(
    command: Command<T>,
    private val commandName: String = "Command",
    private val onComplete: (String, Long) -> Unit = { name, ms ->
        println("⏱ $name executed in ${ms}ms")
    }
) : CommandDecorator<T>(command) {
    override fun execute(): T {
        val (result, duration) = measureTimedValue {
            command.execute()
        }
        onComplete(commandName, duration.inWholeMilliseconds)
        return result
    }
}