package ru.msk.xls.validators.animals.predators

import ru.msk.xls.validators.ValidationResult

object TigerValidator {
    fun validateStripeCount(stripeCount: UInt): ValidationResult {
        if (stripeCount == 0u) return ValidationResult.Invalid("Stripe count must be greater than zero")
        return ValidationResult.Valid
    }
}