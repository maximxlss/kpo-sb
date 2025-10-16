package ru.msk.xls.domain.animals.herbos

import ru.msk.xls.domain.animals.Herbo


class Rabbit(params: Params) : Herbo(params) {
    interface Params : Herbo.Params {
        val jumpHeight: Double
    }

    val jumpHeight = params.jumpHeight

    override val inventoryName: String = "Rabbit"
}