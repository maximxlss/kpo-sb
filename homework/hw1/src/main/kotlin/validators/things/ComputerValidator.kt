package ru.msk.xls.validators.things

import ru.msk.xls.validators.ValidationResult

object ComputerValidator {
    fun validateModel(model: String): ValidationResult {
        if (model.isBlank()) {
            return ValidationResult.Invalid("Model can't be blank.")
        }
        return ValidationResult.Valid
    }
}