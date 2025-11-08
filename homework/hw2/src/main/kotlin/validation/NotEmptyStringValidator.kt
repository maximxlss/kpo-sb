package ru.msk.xls.kpo.bank.validation

// PATTERN: Chain of Responsibility
class NotEmptyStringValidator(
    private val fieldName: String = "Field"
) : Validator<String>() {
    override fun validate(value: String): ValidationResult {
        return if (value.isNotBlank()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("$fieldName must not be empty")
        }
    }
}