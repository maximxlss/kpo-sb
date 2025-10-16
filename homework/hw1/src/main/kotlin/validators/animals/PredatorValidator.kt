package ru.msk.xls.validators.animals

import ru.msk.xls.validators.ValidationResult

object PredatorValidator {
    fun validatePreyList(preyList: List<String>): ValidationResult {
        if (preyList.isEmpty()) return ValidationResult.Invalid("Prey list must not be empty")
        if (preyList.any { it.isBlank() }) return ValidationResult.Invalid("Prey list contains blank entries")
        return ValidationResult.Valid
    }
}
