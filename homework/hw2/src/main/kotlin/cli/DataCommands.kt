package ru.msk.xls.kpo.bank.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.CommandExecution
import ru.msk.xls.kpo.bank.domain.Operation
import ru.msk.xls.kpo.bank.serialization.DatabaseDeserializer
import ru.msk.xls.kpo.bank.serialization.DatabaseSerializer

class ImportCommand : CliktCommand("import"), KoinComponent {
    override fun help(context: Context) = "Import data from JSON, YAML, or CSV files in a directory"
    private val deserializer: DatabaseDeserializer by inject()
    private val database: Database by inject()

    private val directory by argument(help = "Path to directory containing data files to import").path(
        mustExist = true,
        canBeFile = false,
        canBeDir = true
    )

    private val format by option("--format", "-f", help = "Format (json, yaml, csv)")
        .choice("json", "yaml", "yml", "csv", ignoreCase = true)
        .default("json")

    private val clear by option("--clear", help = "Clear database before importing").flag(default = false)

    override fun run() {
        try {
            if (clear) {
                echo("⚠ Clearing database...")
                transaction(database) {
                    Operation.all().forEach { it.delete() }
                    Category.all().forEach { it.delete() }
                    BankAccount.all().forEach { it.delete() }
                    CommandExecution.all().forEach { it.delete() }
                }
                echo("✓ Database cleared")
            }

            val result = when (format.lowercase()) {
                "json" -> deserializer.deserializeDatabaseFromJson(directory)
                "yaml", "yml" -> deserializer.deserializeDatabaseFromYaml(directory)
                "csv" -> deserializer.deserializeDatabaseFromCsv(directory)
                else -> {
                    echo("✗ Unsupported format: $format", err = true)
                    return
                }
            }

            result.fold(
                onSuccess = {
                    echo("✓ Successfully imported data from $directory ($format format)")
                },
                onFailure = { error ->
                    handleImportError(error)
                }
            )
        } catch (e: ExposedSQLException) {
            handleImportError(e)
        } catch (e: Exception) {
            echo("✗ Unexpected error during import: ${e.message}", err = true)
            e.printStackTrace()
        }
    }

    private fun handleImportError(error: Throwable) {
        val message = error.message ?: ""
        when {
            message.contains("PRIMARY KEY", ignoreCase = true) ||
                    message.contains("UNIQUE constraint", ignoreCase = true) ||
                    message.contains("constraint failed", ignoreCase = true) -> {
                echo("✗ Database incompatible with import data (duplicate IDs detected)", err = true)
                echo("  Try running with --clear flag to clear the database first", err = true)
            }

            else -> {
                echo("✗ Failed to import data: ${error.message}", err = true)
            }
        }
    }
}

class ExportCommand : CliktCommand("export"), KoinComponent {
    override fun help(context: Context) = "Export data to JSON, YAML, or CSV files in a directory"
    private val serializer: DatabaseSerializer by inject()

    private val directory by argument(help = "Path to directory where data files will be exported").path()

    private val format by option("--format", "-f", help = "Format (json, yaml, csv)")
        .choice("json", "yaml", "yml", "csv", ignoreCase = true)
        .default("json")

    override fun run() {
        val result = when (format.lowercase()) {
            "json" -> serializer.serializeDatabaseToJson(directory)
            "yaml", "yml" -> serializer.serializeDatabaseToYaml(directory)
            "csv" -> serializer.serializeDatabaseToCsv(directory)
            else -> {
                echo("✗ Unsupported format: $format", err = true)
                return
            }
        }

        result.fold(
            onSuccess = {
                echo("✓ Successfully exported data to $directory ($format format)")
            },
            onFailure = { error ->
                echo("✗ Failed to export data: ${error.message}", err = true)
            }
        )
    }
}
