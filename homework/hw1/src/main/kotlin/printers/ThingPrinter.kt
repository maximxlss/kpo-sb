package ru.msk.xls.printers

import ru.msk.xls.domain.Thing
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.domain.things.Computer
import ru.msk.xls.domain.things.Table
import ru.msk.xls.printers.animals.AnimalPrinter
import ru.msk.xls.printers.things.ComputerPrinter
import ru.msk.xls.printers.things.TablePrinter

object ThingPrinter {
    fun stringify(thing: Thing): String {
        return buildString {
            appendLine("Thing #${thing.inventoryId}: ${thing.inventoryName}")

            when (thing) {
                is Animal -> append(AnimalPrinter.stringify(thing))
                is Table -> append(TablePrinter.stringify(thing))
                is Computer -> append(ComputerPrinter.stringify(thing))
                else -> appendLine("No printer available for ${thing.javaClass.name}, please report this error.")
            }
        }
    }
}
