package ru.msk.xls.printers.animals

import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.domain.animals.herbos.Monkey
import ru.msk.xls.domain.animals.herbos.Rabbit
import ru.msk.xls.printers.animals.herbos.MonkeyPrinter
import ru.msk.xls.printers.animals.herbos.RabbitPrinter

object HerboPrinter {
    fun stringify(herbo: Herbo): String {
        return buildString {
            appendLine("Kindness: ${herbo.kindness}")

            when (herbo) {
                is Monkey -> append(MonkeyPrinter.stringify(herbo))
                is Rabbit -> append(RabbitPrinter.stringify(herbo))
                else -> appendLine("No printer available for ${herbo.javaClass.name}, please report this error.")
            }
        }
    }
}
