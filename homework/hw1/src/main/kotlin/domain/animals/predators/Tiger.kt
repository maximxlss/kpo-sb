package ru.msk.xls.domain.animals.predators

import ru.msk.xls.domain.animals.Predator


class Tiger(params: Params) : Predator(params) {
    interface Params : Predator.Params {
        val stripeCount: UInt
    }

    val stripeCount = params.stripeCount

    override val inventoryName: String = "Tiger"
}