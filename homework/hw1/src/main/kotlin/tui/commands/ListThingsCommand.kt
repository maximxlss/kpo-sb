package ru.msk.xls.tui.commands

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.interfaces.ThingRepository

object ListThingsCommand : Command, KoinComponent {
    override val signature = null
    override val description = "List all the things and their inventory identifiers."

    private val repository: ThingRepository get() = get()

    override fun dispatch(args: String) {
        if (args.isNotBlank()) {
            println("Unexpected argument: $args")
            return
        }
        println("All the things:")
        for (thing in repository.things) {
            println("| Thing #${thing.inventoryId}: ${thing.inventoryName}")
        }
        println("Number of things: ${repository.things.size}")
    }
}