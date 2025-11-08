package ru.msk.xls.domain

import ru.msk.xls.interfaces.Inventory

abstract class Thing(params: Params) : Inventory {
    interface Params {
        val inventoryId: UInt
    }

    override val inventoryId = params.inventoryId
}
