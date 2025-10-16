package ru.msk.xls.printers.things

import ru.msk.xls.domain.things.Computer

object ComputerPrinter {
    fun stringify(computer: Computer): String {
        return buildString {
            appendLine("Model: ${computer.model}")
        }
    }
}