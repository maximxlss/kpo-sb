package ru.msk.xls.kpo.bank.factories

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.validation.NonNegativeAmountValidator
import ru.msk.xls.kpo.bank.validation.NotEmptyStringValidator
import ru.msk.xls.kpo.bank.validation.ValidationResult

// PATTERN: Factory Method - centralizes BankAccount creation with validation
@Single
class BankAccountFactory : KoinComponent {
    private val database: Database by inject()
    private val nameValidator = NotEmptyStringValidator(fieldName = "Account name")
    private val balanceValidator = NonNegativeAmountValidator()

    fun create(name: String, balance: Double): Result<BankAccount> {
        val nameValidation = nameValidator.validateAll(name)
        if (nameValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(nameValidation.errors.joinToString("; ")))
        }

        val balanceValidation = balanceValidator.validateAll(balance)
        if (balanceValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(balanceValidation.errors.joinToString("; ")))
        }

        return try {
            val account = transaction(database) {
                BankAccount.new {
                    this.name = name
                    this.balance = balance
                }
            }
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to create account: ${e.message}", e))
        }
    }
}