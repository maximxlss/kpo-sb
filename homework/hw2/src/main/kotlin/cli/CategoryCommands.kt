package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.CategoryType
import ru.msk.xls.kpo.bank.factories.CategoryFactory

class CreateCategoryCommand : CliktCommand("create"), KoinComponent {
    override fun help(context: Context) = "Create a new category"
    private val categoryFactory: CategoryFactory by inject()

    private val name by option("--name", "-n", help = "Category name").required()
    private val type by option("--type", "-t", help = "Category type (income/spending)")
        .enum<CategoryType>()
        .required()

    override fun run() {
        categoryFactory.create(name, type).fold(
            onSuccess = { category ->
                echo("✓ Category created successfully!")
                echo("  ID: ${category.id.value}")
                echo("  Name: ${category.name}")
                echo("  Type: ${category.type}")
            },
            onFailure = { error ->
                echo("✗ Failed to create category: ${error.message}", err = true)
            }
        )
    }
}

class ListCategoriesCommand : CliktCommand("list"), KoinComponent {
    override fun help(context: Context) = "List all categories"
    private val database: Database by inject()

    override fun run() {
        transaction(database) {
            val categories = Category.all()

            if (categories.empty()) {
                echo("No categories found. Create one with 'category create'")
                return@transaction
            }

            echo("Categories:")
            echo("─".repeat(60))

            val income = categories.filter { it.type == CategoryType.INCOME }
            val spending = categories.filter { it.type == CategoryType.SPENDING }

            echo("INCOME:")
            income.forEach { cat ->
                echo("  ${cat.id.value}: ${cat.name}")
            }

            echo("\nSPENDING:")
            spending.forEach { cat ->
                echo("  ${cat.id.value}: ${cat.name}")
            }

            echo("─".repeat(60))
            echo("Total: ${categories.count()} categories (${income.count()} income, ${spending.count()} spending)")
        }
    }
}

class EditCategoryCommand : CliktCommand("edit"), KoinComponent {
    override fun help(context: Context) = "Edit an existing category"
    private val database: Database by inject()

    private val categoryId by option("--id", help = "Category ID").int().required()
    private val name by option("--name", "-n", help = "New category name")
    private val type by option("--type", "-t", help = "New category type (income/spending)").enum<CategoryType>()

    override fun run() {
        if (name == null && type == null) {
            echo("✗ Provide --name or --type to update", err = true)
            return
        }

        transaction(database) {
            val category = try {
                Category[categoryId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Category with ID $categoryId not found", err = true)
                return@transaction
            }

            name?.let { category.name = it }
            type?.let { category.type = it }

            echo("✓ Category updated successfully!")
            echo("  ID: ${category.id.value}")
            echo("  Name: ${category.name}")
            echo("  Type: ${category.type}")
        }
    }
}

class DeleteCategoryCommand : CliktCommand("delete"), KoinComponent {
    override fun help(context: Context) = "Delete a category"
    private val database: Database by inject()

    private val categoryId by option("--id", help = "Category ID").int().required()

    override fun run() {
        transaction(database) {
            val category = try {
                Category[categoryId]
            } catch (e: EntityNotFoundException) {
                echo("✗ Category with ID $categoryId not found", err = true)
                return@transaction
            }

            val categoryName = category.name
            category.delete()

            echo("✓ Category '$categoryName' (ID: $categoryId) deleted successfully")
        }
    }
}

