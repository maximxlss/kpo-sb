package ru.msk.xls.kpo.bank.serialization

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.msk.xls.kpo.bank.domain.BankAccount
import ru.msk.xls.kpo.bank.domain.Category
import ru.msk.xls.kpo.bank.domain.CommandExecution
import ru.msk.xls.kpo.bank.domain.Operation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// PATTERN: Template Method - defines serialization skeleton, delegates format-specific steps
@Single
class DatabaseSerializer : KoinComponent {
    private val database: Database by inject()

    fun serializeDatabaseToJson(path: Path): Result<Unit> =
        serializeDatabase(database, path, JsonSerializer, "json")

    fun serializeDatabaseToYaml(path: Path): Result<Unit> =
        serializeDatabase(database, path, YamlSerializer, "yaml")

    @OptIn(ExperimentalSerializationApi::class)
    fun serializeDatabaseToCsv(path: Path): Result<Unit> =
        serializeDatabase(database, path, CsvSerializer, "csv")

    private fun verifyAndPrepareSerializationPath(path: Path): Result<Unit> {
        return try {
            path.createDirectories()
            if (path.toFile().listFiles()?.isNotEmpty() == true) {
                Result.failure(IllegalArgumentException("Serialization path exists and is non-empty"))
            } else {
                Result.success(Unit)
            }
        } catch (_: FileAlreadyExistsException) {
            Result.failure(IllegalArgumentException("Serialization path exists and is a file"))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to prepare serialization path: ${e.message}", e))
        }
    }

    private interface Serializer {
        fun <T> serialize(data: T, type: KType): String
    }

    private object JsonSerializer : Serializer {
        override fun <T> serialize(data: T, type: KType): String =
            Json.encodeToString(Json.serializersModule.serializer(type), data)
    }

    private object YamlSerializer : Serializer {
        override fun <T> serialize(data: T, type: KType): String =
            Yaml.default.encodeToString(Yaml.default.serializersModule.serializer(type), data)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private object CsvSerializer : Serializer {
        override fun <T> serialize(data: T, type: KType): String =
            Csv.encodeToString(Csv.serializersModule.serializer(type), data)
    }

    private fun <T, U> T.lazyLet(func: (T) -> U) = { func(this) }

    private fun serializeDatabase(
        database: Database,
        path: Path,
        serializer: Serializer,
        fileExtension: String
    ): Result<Unit> {
        verifyAndPrepareSerializationPath(path).getOrElse { error ->
            return Result.failure(error)
        }

        return try {
            val dataToWrite = transaction(database) {
                mapOf(
                    "bank_accounts" to BankAccount.all().map(::BankAccountData).lazyLet { data ->
                        serializer.serialize(data, typeOf<List<BankAccountData>>())
                    },
                    "categories" to Category.all().map(::CategoryData).lazyLet { data ->
                        serializer.serialize(data, typeOf<List<CategoryData>>())
                    },
                    "operations" to Operation.all().map(::OperationData).lazyLet { data ->
                        serializer.serialize(data, typeOf<List<OperationData>>())
                    },
                    "command_executions" to CommandExecution.all().map(::CommandExecutionData).lazyLet { data ->
                        serializer.serialize(data, typeOf<List<CommandExecutionData>>())
                    }
                )
            }

            for ((name, getter) in dataToWrite) {
                File(path.toFile(), "$name.$fileExtension").writeText(getter())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to serialize database: ${e.message}", e))
        }
    }
}