package ru.msk.xls.printers.things

import ru.msk.xls.domain.things.Table

object TablePrinter {
    fun stringify(table: Table): String {
        return buildString {
            appendLine("Material: ${table.material}")
        }
    }
}