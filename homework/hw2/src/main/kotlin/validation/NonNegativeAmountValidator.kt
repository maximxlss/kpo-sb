package ru.msk.xls.kpo.bank.validation

// PATTERN: Chain of Responsibility
class NonNegativeAmountValidator : Validator<Double>() {
    override fun validate(value: Double): ValidationResult {
        return if (value >= 0) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Amount must not be negative (got $value)")
        }
    }
}