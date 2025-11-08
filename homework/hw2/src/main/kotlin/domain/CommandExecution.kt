package ru.msk.xls.kpo.bank.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object CommandExecutions : IntIdTable("command_executions") {
    val commandName = varchar("command_name", 256)
    val executionTimeMs = long("execution_time_ms")
    val success = bool("success")
    val timestamp = long("timestamp")
}

class CommandExecution(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CommandExecution>(CommandExecutions)

    var commandName by CommandExecutions.commandName
    var executionTimeMs by CommandExecutions.executionTimeMs
    var success by CommandExecutions.success
    var timestamp by CommandExecutions.timestamp
}
