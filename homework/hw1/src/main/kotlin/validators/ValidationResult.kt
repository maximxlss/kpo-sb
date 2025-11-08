package ru.msk.xls.validators

sealed class ValidationResult {
    class Invalid(val reason: String) : ValidationResult()
    object Valid : ValidationResult()
}