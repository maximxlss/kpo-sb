package ru.msk.xls.printers.animals.predators

import ru.msk.xls.domain.animals.predators.Tiger

object TigerPrinter {
    fun stringify(tiger: Tiger): String {
        return buildString {
            appendLine("Stripe count: ${tiger.stripeCount}")
        }
    }
}
