package ru.msk.xls.domain.animals.predators

import ru.msk.xls.domain.animals.Predator

class Wolf(params: Params) : Predator(params) {
    interface Params : Predator.Params {
        val biteStrength: Double
    }

    val biteStrength = params.biteStrength

    override val inventoryName: String = "Wolf"
}
