package ru.msk.xls.printers.animals.herbos

import ru.msk.xls.domain.animals.herbos.Rabbit

object RabbitPrinter {
    fun stringify(rabbit: Rabbit): String {
        return buildString {
            appendLine("Jump height: ${rabbit.jumpHeight}")
        }
    }
}