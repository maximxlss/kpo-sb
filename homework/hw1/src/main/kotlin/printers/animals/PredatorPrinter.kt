package ru.msk.xls.printers.animals

import ru.msk.xls.domain.animals.Predator
import ru.msk.xls.domain.animals.predators.Tiger
import ru.msk.xls.domain.animals.predators.Wolf
import ru.msk.xls.printers.animals.predators.TigerPrinter
import ru.msk.xls.printers.animals.predators.WolfPrinter

object PredatorPrinter {
    fun stringify(predator: Predator): String {
        return buildString {
            appendLine("Prey list: ${predator.preyList.joinToString(", ")}")

            when (predator) {
                is Tiger -> append(TigerPrinter.stringify(predator))
                is Wolf -> append(WolfPrinter.stringify(predator))
                else -> appendLine("No printer available for ${predator.javaClass.name}, please report this error.")
            }
        }
    }
}
