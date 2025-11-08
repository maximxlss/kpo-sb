package ru.msk.xls.kpo.bank.serialization

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// PATTERN: Template Method - defines deserialization skeleton, delegates format-specific steps
@Single
class DatabaseDeserializer : KoinComponent {
    private val database: Database by inject()

    fun deserializeDatabaseFromJson(path: Path): Result<Unit> =
        deserializeDatabase(database, path, JsonDeserializer, "json")

    fun deserializeDatabaseFromYaml(path: Path): Result<Unit> =
        deserializeDatabase(database, path, YamlDeserializer, "yaml")

    @OptIn(ExperimentalSerializationApi::class)
    fun deserializeDatabaseFromCsv(path: Path): Result<Unit> =
        deserializeDatabase(database, path, CsvDeserializer, "csv")

    private interface Deserializer {
        fun <T> deserialize(data: String, type: KType): T
    }

    private object JsonDeserializer : Deserializer {
        @Suppress("UNCHECKED_CAST")
        override fun <T> deserialize(data: String, type: KType): T =
            Json.decodeFromString(Json.serializersModule.serializer(type), data) as T
    }

    private object YamlDeserializer : Deserializer {
        @Suppress("UNCHECKED_CAST")
        override fun <T> deserialize(data: String, type: KType): T =
            Yaml.default.decodeFromString(Yaml.default.serializersModule.serializer(type), data) as T
    }

    @OptIn(ExperimentalSerializationApi::class)
    private object CsvDeserializer : Deserializer {
        @Suppress("UNCHECKED_CAST")
        override fun <T> deserialize(data: String, type: KType): T =
            Csv.decodeFromString(Csv.serializersModule.serializer(type), data) as T
    }

    private inline fun <reified T> deserializeOneListType(
        path: Path,
        deserializer: Deserializer,
        filename: String
    ): Result<List<T>> {
        val file = File(path.toFile(), filename)
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("File '$filename' does not exist"))
        }

        return try {
            val result = deserializer.deserialize<List<T>>(file.readText(), typeOf<List<T>>())
            Result.success(result)
        } catch (e: SerializationException) {
            Result.failure(IllegalArgumentException("File '$filename' contains invalid data: ${e.message}", e))
        } catch (e: IllegalArgumentException) {
            Result.failure(IllegalArgumentException("File '$filename' contains invalid data: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to deserialize '$filename': ${e.message}", e))
        }
    }

    private fun deserializeDatabase(
        database: Database,
        path: Path,
        deserializer: Deserializer,
        fileExtension: String
    ): Result<Unit> {
        val objects = mutableListOf<SaveToDatabase>()

        deserializeOneListType<BankAccountData>(path, deserializer, "bank_accounts.$fileExtension")
            .getOrElse { return Result.failure(it) }
            .also { objects.addAll(it) }

        deserializeOneListType<CategoryData>(path, deserializer, "categories.$fileExtension")
            .getOrElse { return Result.failure(it) }
            .also { objects.addAll(it) }

        deserializeOneListType<OperationData>(path, deserializer, "operations.$fileExtension")
            .getOrElse { return Result.failure(it) }
            .also { objects.addAll(it) }

        deserializeOneListType<CommandExecutionData>(path, deserializer, "command_executions.$fileExtension")
            .getOrElse { return Result.failure(it) }
            .also { objects.addAll(it) }

        return try {
            transaction(database) {
                objects.forEach { it.saveToDatabase() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to save deserialized data to database: ${e.message}", e))
        }
    }
}
