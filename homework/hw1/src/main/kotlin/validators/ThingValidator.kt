package ru.msk.xls.validators

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.interfaces.ThingRepository

class ThingValidator : KoinComponent {
    private val repository: ThingRepository get() = get()

    fun validateInventoryId(inventoryId: UInt): ValidationResult {
        if (repository.isInventoryIdTaken(inventoryId)) {
            return ValidationResult.Invalid("Inventory ID is already taken")
        }
        return ValidationResult.Valid
    }
}