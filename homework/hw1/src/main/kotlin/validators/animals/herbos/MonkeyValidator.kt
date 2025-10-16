package ru.msk.xls.validators.animals.herbos

import ru.msk.xls.validators.ValidationResult

object MonkeyValidator {
    fun validateFurAmount(furAmount: Double): ValidationResult {
        if (furAmount < 0.0) return ValidationResult.Invalid("Fur amount must be non-negative")
        return ValidationResult.Valid
    }
}