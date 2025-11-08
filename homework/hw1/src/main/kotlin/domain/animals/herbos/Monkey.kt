package ru.msk.xls.domain.animals.herbos

import ru.msk.xls.domain.animals.Herbo

class Monkey(params: Params) : Herbo(params) {
    interface Params : Herbo.Params {
        val furAmount: Double
    }

    val furAmount = params.furAmount

    override val inventoryName: String = "Monkey"
}


