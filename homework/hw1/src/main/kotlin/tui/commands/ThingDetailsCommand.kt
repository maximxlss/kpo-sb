package ru.msk.xls.tui.commands

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.interfaces.ThingRepository
import ru.msk.xls.printers.ThingPrinter

object ThingDetailsCommand : Command, KoinComponent {
    override val signature = "<inventory id>"
    override val description = "List detailed information about the thing with given inventory id."

    private val repository: ThingRepository get() = get()

    override fun dispatch(args: String) {
        val inventoryId = args.toUIntOrNull()
        if (inventoryId == null) {
            println("Invalid id")
            return
        }
        val things = repository.things.filter { it.inventoryId == inventoryId }
        if (things.isEmpty()) {
            println("Thing with inventory id $inventoryId doesn't exist")
            return
        } else if (things.size > 1) {
            println("Repository is invalid.")
            return
        }
        val thing = things[0]
        print(ThingPrinter.stringify(thing))
    }
}