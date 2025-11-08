package ru.msk.xls.tui.commands

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.interfaces.ThingRepository

object ListAnimalsCommand : Command, KoinComponent {
    override val signature = null
    override val description = "List all the animals and how much they eat."

    private val repository: ThingRepository get() = get()

    override fun dispatch(args: String) {
        if (args.isNotBlank()) {
            println("Unexpected argument: $args")
        }
        val animals = repository.things.filterIsInstance<Animal>()
        println("All the animals:")
        for (animal in animals) {
            println("| Animal #${animal.inventoryId}: ${animal.inventoryName} (eats ${animal.eatsKg} kg)")
        }
        println("Number of animals: ${animals.size}")
        println("Total amount of food per day: ${animals.sumOf { it.eatsKg }} kg")
    }
}