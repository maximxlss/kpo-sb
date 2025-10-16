package ru.msk.xls.domain.things

import ru.msk.xls.domain.Thing

class Table(params: Params) : Thing(params) {
    interface Params : Thing.Params {
        val material: String
    }

    val material = params.material

    override val inventoryName: String = "Table"
}