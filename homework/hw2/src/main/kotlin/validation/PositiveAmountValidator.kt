package ru.msk.xls.kpo.bank.validation

// PATTERN: Chain of Responsibility
class PositiveAmountValidator : Validator<Double>() {
    override fun validate(value: Double): ValidationResult {
        return if (value > 0) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Amount must be positive (got $value)")
        }
    }
}