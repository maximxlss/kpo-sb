package ru.msk.xls.kpo.bank.factories

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.CategoryType
import ru.msk.xls.kpo.bank.validation.NotEmptyStringValidator
import ru.msk.xls.kpo.bank.validation.ValidationResult

// PATTERN: Factory Method - centralizes Category creation with validation
@Single
class CategoryFactory : KoinComponent {
    private val database: Database by inject()
    private val nameValidator = NotEmptyStringValidator("Category name")

    fun create(name: String, type: CategoryType): Result<Category> {
        val nameValidation = nameValidator.validateAll(name)
        if (nameValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(nameValidation.errors.joinToString("; ")))
        }

        return try {
            transaction(database) {
                val category = Category.new {
                    this.name = name
                    this.type = type
                }
                Result.success(category)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to create category: ${e.message}", e))
        }
    }
}
