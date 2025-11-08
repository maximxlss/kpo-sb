package ru.msk.xls.kpo.bank.validation

// PATTERN: Chain of Responsibility
class NoContainsStringValidator(
    private val forbiddenSubstring: String,
    private val fieldName: String = "Field"
) : Validator<String>() {
    override fun validate(value: String): ValidationResult {
        return if (!value.contains(forbiddenSubstring, ignoreCase = true)) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("$fieldName must not contain '$forbiddenSubstring'")
        }
    }
}