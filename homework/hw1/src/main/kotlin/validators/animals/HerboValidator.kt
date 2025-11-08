package ru.msk.xls.validators.animals

import ru.msk.xls.validators.ValidationResult

object HerboValidator {
    fun validateKindness(kindness: Double): ValidationResult {
        if (kindness !in 0.0..10.0) return ValidationResult.Invalid("Kindness must be between 0.0 and 1.0")
        return ValidationResult.Valid
    }
}