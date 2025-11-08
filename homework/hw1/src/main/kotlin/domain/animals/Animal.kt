package ru.msk.xls.domain.animals

import ru.msk.xls.domain.Thing
import ru.msk.xls.interfaces.Alive


abstract class Animal(params: Params) : Thing(params), Alive {
    interface Params : Thing.Params {
        val eatsKg: Double
    }

    override val eatsKg = params.eatsKg
}
