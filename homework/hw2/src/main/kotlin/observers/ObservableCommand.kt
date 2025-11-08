package ru.msk.xls.kpo.bank.observers

// PATTERN: Observer - observable subject that notifies observers of command execution
class ObservableCommand<T>(
    private val commandName: String,
    private val executeFunction: () -> T
) {
    private val observers = mutableListOf<CommandObserver>()

    fun addObserver(observer: CommandObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: CommandObserver) {
        observers.remove(observer)
    }

    fun execute(): T {
        val startTime = System.currentTimeMillis()
        var success = true

        return try {
            executeFunction()
        } catch (e: Exception) {
            success = false
            throw e
        } finally {
            val executionTime = System.currentTimeMillis() - startTime
            notifyObservers(commandName, executionTime, success)
        }
    }

    private fun notifyObservers(commandName: String, executionTimeMs: Long, success: Boolean) {
        observers.forEach { observer ->
            try {
                observer.onCommandExecuted(commandName, executionTimeMs, success)
            } catch (e: Exception) {
                System.err.println("Observer notification failed: ${e.message}")
            }
        }
    }
}