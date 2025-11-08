package ru.msk.xls.tui.builders.animals.predators

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Predator
import ru.msk.xls.domain.animals.predators.Wolf
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.predators.WolfValidator

object WolfConsoleBuilder {
    fun askUserForWolfParams(predatorParams: Predator.Params): Wolf.Params {
        val biteStrength = getOrRetry {
            print("Input its bite strength: ")
            val v = readln().toDoubleOrNull()
            if (v != null) {
                when (val r = WolfValidator.validateBiteStrength(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid strength")
        }

        return object : Wolf.Params, Predator.Params by predatorParams {
            override val biteStrength: Double = biteStrength
        }
    }

    fun askUserForWolf(predatorParams: Predator.Params): Wolf {
        val wolfParams = askUserForWolfParams(predatorParams)
        return Wolf(wolfParams)
    }
}

