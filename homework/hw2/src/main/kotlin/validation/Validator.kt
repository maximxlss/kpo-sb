package ru.msk.xls.kpo.bank.validation

// PATTERN: Chain of Responsibility
abstract class Validator<T> {
    var next: Validator<T>? = null

    abstract fun validate(value: T): ValidationResult

    fun validateAll(value: T): ValidationResult {
        val result = validate(value)
        return if (result is ValidationResult.Valid) {
            next?.validateAll(value) ?: result
        } else {
            result
        }
    }

    infix fun then(nextValidator: Validator<T>): Validator<T> {
        this.next = nextValidator
        return nextValidator
    }
}

sealed class ValidationResult {
    data class Valid(val message: String = "Valid") : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(error: String) : this(listOf(error))
    }

    companion object {
        fun valid() = Valid()
        fun invalid(error: String) = Invalid(error)
        fun invalid(errors: List<String>) = Invalid(errors)
    }
}