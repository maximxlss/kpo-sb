package ru.msk.xls.printers.animals.herbos

import ru.msk.xls.domain.animals.herbos.Monkey

object MonkeyPrinter {
    fun stringify(monkey: Monkey): String {
        return buildString {
            appendLine("Fur amount: ${monkey.furAmount}")
        }
    }
}
