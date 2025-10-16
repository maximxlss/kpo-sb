package ru.msk.xls.domain.things

import ru.msk.xls.domain.Thing

class Computer(params: Params) : Thing(params) {
    interface Params : Thing.Params {
        val model: String
    }

    val model = params.model

    override val inventoryName: String = "Computer"
}