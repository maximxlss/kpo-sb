package ru.msk.xls.tui.builders.things

import ru.msk.xls.AskResult
import ru.msk.xls.domain.Thing
import ru.msk.xls.domain.things.Computer
import ru.msk.xls.domain.things.Computer.Params
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.things.ComputerValidator

object ComputerConsoleBuilder {
    fun askUserForComputerParams(thingParams: Thing.Params): Params {
        val model = getOrRetry {
            print("Input the model of the computer: ")
            val v = readln()
            return@getOrRetry when (val r = ComputerValidator.validateModel(v)) {
                is ValidationResult.Valid -> AskResult.ok(v)
                is ValidationResult.Invalid -> AskResult.fail(r.reason)
            }
        }

        return object : Params, Thing.Params by thingParams {
            override val model = model
        }
    }

    fun askUserForComputer(thingParams: Thing.Params): Computer {
        val computerParams = askUserForComputerParams(thingParams)
        return Computer(computerParams)
    }
}