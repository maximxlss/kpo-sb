package ru.msk.xls.printers.animals

import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.domain.animals.Predator

object AnimalPrinter {
    fun stringify(animal: Animal): String {
        return buildString {
            appendLine("Eats per day (kg): ${animal.eatsKg}")

            when (animal) {
                is Herbo -> append(HerboPrinter.stringify(animal))
                is Predator -> append(PredatorPrinter.stringify(animal))
                else -> appendLine("No printer available for ${animal.javaClass.name}, please report this error.")
            }
        }
    }
}
