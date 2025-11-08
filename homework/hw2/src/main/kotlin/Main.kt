package ru.msk.xls.kpo.bank

import com.github.ajalt.clikt.core.main
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.*
import org.koin.ksp.generated.startKoin
import ru.msk.xls.kpo.bank.cli.configureCliApp
import ru.msk.xls.kpo.bank.domain.BankAccounts
import ru.msk.xls.kpo.bank.domain.Categories
import ru.msk.xls.kpo.bank.domain.CommandExecutions
import ru.msk.xls.kpo.bank.domain.Operations

// PATTERN: Dependency Injection - Koin container with KSP annotations
@KoinApplication
object BankApp

@Module
@Configuration
@ComponentScan
class BankAppModule

@Single
fun provideDatabase(): Database {
    val database = Database.connect("jdbc:sqlite:test.db", "org.sqlite.JDBC")
    transaction(database) {
        SchemaUtils.create(Operations, Categories, BankAccounts, CommandExecutions)
    }
    return database
}

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR")
    BankApp.startKoin()
    configureCliApp().main(args)
}