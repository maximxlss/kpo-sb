package ru.msk.xls.validators.animals.predators

import ru.msk.xls.validators.ValidationResult

object WolfValidator {
    fun validateBiteStrength(biteStrength: Double): ValidationResult {
        if (biteStrength < 0.0) return ValidationResult.Invalid("Bite strength must be non-negative")
        return ValidationResult.Valid
    }
}