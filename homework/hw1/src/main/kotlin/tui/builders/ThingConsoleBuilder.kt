package ru.msk.xls.tui.builders

import ru.msk.xls.AskResult
import ru.msk.xls.domain.Thing
import ru.msk.xls.getOrRetry
import ru.msk.xls.tui.builders.animals.AnimalConsoleBuilder
import ru.msk.xls.tui.builders.things.ComputerConsoleBuilder
import ru.msk.xls.tui.builders.things.TableConsoleBuilder
import ru.msk.xls.validators.ThingValidator
import ru.msk.xls.validators.ValidationResult


object ThingConsoleBuilder {
    val nextBuilders = mapOf(
        "animal" to AnimalConsoleBuilder::askUserForAnimal,
        "table" to TableConsoleBuilder::askUserForTable,
        "computer" to ComputerConsoleBuilder::askUserForComputer,
    )

    fun askUserForThingParams(): Thing.Params {
        val inventoryId = getOrRetry {
            print("Input inventory id: ")
            val v = readln().toUIntOrNull()
            if (v != null) {
                when (val res = ThingValidator().validateInventoryId(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(res.reason)
                }
            }
            AskResult.fail("Invalid inventory id")
        }

        return object : Thing.Params {
            override val inventoryId: UInt = inventoryId
        }
    }

    fun askUserForThing(): Thing {
        val thingParams = askUserForThingParams()
        val kinds = nextBuilders.keys.joinToString(" ")
        val nextBuilder = getOrRetry {
            println("Choose a kind from: $kinds")
            print("Enter thing kind: ")
            val kind = readln().lowercase()
            nextBuilders[kind]?.let {
                return@getOrRetry AskResult.ok(it)
            }
            AskResult.fail("Invalid kind.")
        }
        return nextBuilder(thingParams)
    }
}