package ru.msk.xls.printers.animals.predators

import ru.msk.xls.domain.animals.predators.Wolf

object WolfPrinter {
    fun stringify(wolf: Wolf): String {
        return buildString {
            appendLine("Bite strength: ${wolf.biteStrength}")
        }
    }
}