package ru.msk.xls.validators.animals.herbos

import ru.msk.xls.validators.ValidationResult

object RabbitValidator {
    fun validateJumpHeight(jumpHeight: Double): ValidationResult {
        if (jumpHeight < 0.0) return ValidationResult.Invalid("Jump height must be non-negative")
        return ValidationResult.Valid
    }
}