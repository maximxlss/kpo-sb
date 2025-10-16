package ru.msk.xls.validators.things

import ru.msk.xls.validators.ValidationResult

object TableValidator {
    fun validateMaterial(material: String): ValidationResult {
        if (material.isBlank()) {
            return ValidationResult.Invalid("Material can't be blank.")
        }
        return ValidationResult.Valid
    }
}