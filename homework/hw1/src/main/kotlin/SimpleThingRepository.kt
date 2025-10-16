package ru.msk.xls

import ru.msk.xls.domain.Thing
import ru.msk.xls.interfaces.ThingRepository

class SimpleThingRepository : ThingRepository {
    private val _things = mutableListOf<Thing>()
    override val things: List<Thing> = _things

    override fun addThing(thing: Thing): ThingRepository.CreationResult {
        if (isInventoryIdTaken(thing.inventoryId)) {
            return ThingRepository.CreationResult.Fail("Inventory id taken")
        }
        _things.add(thing)
        return ThingRepository.CreationResult.Success
    }

    override fun isInventoryIdTaken(inventoryId: UInt): Boolean =
        things.any { thing -> thing.inventoryId == inventoryId }
}
