package ru.msk.xls.tui.builders.animals.herbos

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.domain.animals.herbos.Monkey
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.herbos.MonkeyValidator

object MonkeyConsoleBuilder {
    fun askUserForMonkeyParams(herboParams: Herbo.Params): Monkey.Params {
        val furAmount = getOrRetry {
            print("Input its fur amount: ")
            val v = readln().toDoubleOrNull()
            if (v != null) {
                when (val r = MonkeyValidator.validateFurAmount(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid fur amount")
        }

        return object : Monkey.Params, Herbo.Params by herboParams {
            override val furAmount: Double = furAmount
        }
    }

    fun askUserForMonkey(herboParams: Herbo.Params): Monkey {
        val params = askUserForMonkeyParams(herboParams)
        return Monkey(params)
    }
}
