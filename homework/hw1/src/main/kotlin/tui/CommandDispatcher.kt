package ru.msk.xls.tui

import ru.msk.xls.tui.commands.*

object CommandDispatcher {
    val commands = mapOf(
        "help" to HelpCommand,
        "exit" to ExitCommand,
        "create" to CreateCommand,
        "list_things" to ListThingsCommand,
        "list_animals" to ListAnimalsCommand,
        "list_contact_zoo" to ListContactZooCommand,
        "details" to ThingDetailsCommand,
    )

    val commandListString = commands.keys.joinToString(", ")

    object HelpCommand : Command {
        override val signature = null
        override val description: String = "Display this help message"

        override fun dispatch(args: String) {
            if (args.isNotBlank()) {
                println("Unexpected arguments: $args. Anyway:")
            }
            println("| The following commands are supported:")
            for ((name, command) in commands) {
                val signatureString = command.signature?.let { " $it" } ?: ""
                println("| - $name$signatureString")
                println(command.description.prependIndent("|   "))
            }
        }
    }

    object ExitCommand : Command {
        override val signature: String? = null
        override val description: String = "Exit the application"

        override fun dispatch(args: String) {
            if (args.isNotBlank()) {
                println("Unexpected arguments: $args")
                return
            }
            println("Bye!")
            running = false
        }
    }


    var running: Boolean = true

    fun run() {
        running = true
        println("Application started. Type 'help' to see available commands.")
        while (running) {
            processOneLine()
        }
    }

    fun processOneLine() {
        print("> ")
        val line = readln()
        val (name, args) = if (line.contains(" ")) {
            line.split(" ", limit = 2)
        } else {
            listOf(line, "")
        }
        when (val command = commands[name]) {
            null -> {
                println("Invalid command. Supported commands: help, $commandListString")
            }

            else -> {
                command.dispatch(args)
            }
        }
    }
}