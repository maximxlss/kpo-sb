package ru.msk.xls.tui.builders.things

import ru.msk.xls.AskResult
import ru.msk.xls.domain.Thing
import ru.msk.xls.domain.things.Table
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.things.TableValidator


object TableConsoleBuilder {
    fun askUserForTableParams(thingParams: Thing.Params): Table.Params {
        val material = getOrRetry {
            print("Input the material of the table: ")
            val v = readln()
            return@getOrRetry when (val r = TableValidator.validateMaterial(v)) {
                is ValidationResult.Valid -> AskResult.ok(v)
                is ValidationResult.Invalid -> AskResult.fail(r.reason)
            }
        }

        return object : Table.Params, Thing.Params by thingParams {
            override val material = material
        }
    }

    fun askUserForTable(thingParams: Thing.Params): Table {
        val tableParams = askUserForTableParams(thingParams)
        return Table(tableParams)
    }
}