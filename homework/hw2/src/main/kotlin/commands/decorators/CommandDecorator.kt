package ru.msk.xls.kpo.bank.commands.decorators

import ru.msk.xls.kpo.bank.commands.Command

// PATTERN: Decorator - base decorator for adding behavior to commands
abstract class CommandDecorator<T>(
    protected val command: Command<T>
) : Command<T> {
    override fun execute(): T = command.execute()
}