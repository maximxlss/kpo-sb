package ru.msk.xls.tui.commands

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.interfaces.ThingRepository

object ListContactZooCommand : Command, KoinComponent {
    override val signature = null
    override val description = "List all the animals to be put in the contact zoo."

    private val repository: ThingRepository get() = get()

    override fun dispatch(args: String) {
        if (args.isNotBlank()) {
            println("Unexpected argument: $args")
        }
        val contactAnimals =
            repository.things.filterIsInstance<Herbo>().filter { it.isKind() }.sortedBy { it.kindness }.reversed()
        println("Contact zoo eligible animals:")
        for (animal in contactAnimals) {
            println("| Animal #${animal.inventoryId}: ${animal.inventoryName} (${animal.kindness} kind)")
        }
        println("Number of animals: ${contactAnimals.size}")
    }
}