package ru.msk.xls.validators.animals

import ru.msk.xls.validators.ValidationResult

object AnimalValidator {
    fun validateEatsKg(eatsKg: Double): ValidationResult {
        if (eatsKg < 0.0) return ValidationResult.Invalid("Amount eaten per day must be non-negative")
        return ValidationResult.Valid
    }
}
