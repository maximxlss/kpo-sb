package ru.msk.xls.kpo.bank.factories

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.domain.OperationType
import ru.msk.xls.kpo.bank.validation.NoContainsStringValidator
import ru.msk.xls.kpo.bank.validation.NotEmptyStringValidator
import ru.msk.xls.kpo.bank.validation.PositiveAmountValidator
import ru.msk.xls.kpo.bank.validation.ValidationResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// PATTERN: Factory Method - centralizes Operation creation with validation and auto-balance update
@Single
class OperationFactory : KoinComponent {
    private val database: Database by inject()
    private val amountValidator = PositiveAmountValidator()
    private val descriptionValidator = NotEmptyStringValidator(fieldName = "Description") then
            NoContainsStringValidator("fraud", fieldName = "Description")

    @OptIn(ExperimentalTime::class)
    fun create(
        type: OperationType,
        accountId: Int,
        amount: Double,
        description: String,
        categoryId: Int
    ): Result<Operation> {
        val amountValidation = amountValidator.validateAll(amount)
        if (amountValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(amountValidation.errors.joinToString("; ")))
        }

        val descriptionValidation = descriptionValidator.validateAll(description)
        if (descriptionValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(descriptionValidation.errors.joinToString("; ")))
        }

        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        return try {
            transaction(database) {
                val bankAccount = try {
                    BankAccount[accountId]
                } catch (e: EntityNotFoundException) {
                    return@transaction Result.failure(IllegalArgumentException("Account with ID $accountId not found"))
                }

                val category = try {
                    Category[categoryId]
                } catch (e: EntityNotFoundException) {
                    return@transaction Result.failure(IllegalArgumentException("Category with ID $categoryId not found"))
                }

                val operation = Operation.new {
                    this.type = type
                    this.bankAccount = bankAccount
                    this.absoluteValue = amount
                    this.date = date
                    this.description = description
                    this.category = category
                }

                when (type) {
                    OperationType.INCOME -> bankAccount.balance += amount
                    OperationType.SPENDING -> bankAccount.balance -= amount
                }

                Result.success(operation)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to create operation: ${e.message}", e))
        }
    }
}
