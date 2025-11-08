package ru.msk.xls.interfaces

import ru.msk.xls.domain.Thing

interface ThingRepository {
    val things: List<Thing>

    sealed class CreationResult {
        object Success : CreationResult()
        class Fail(val reason: String) : CreationResult()
    }

    fun addThing(thing: Thing): CreationResult

    fun isInventoryIdTaken(inventoryId: UInt): Boolean
}