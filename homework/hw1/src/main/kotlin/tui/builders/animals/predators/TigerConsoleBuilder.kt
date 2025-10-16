package ru.msk.xls.tui.builders.animals.predators

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Predator
import ru.msk.xls.domain.animals.predators.Tiger
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.predators.TigerValidator

object TigerConsoleBuilder {
    fun askUserForTigerParams(predatorParams: Predator.Params): Tiger.Params {
        val stripeCount = getOrRetry {
            print("Input its stripe count: ")
            val v = readln().toUIntOrNull()
            if (v != null) {
                when (val r = TigerValidator.validateStripeCount(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid stripe count")
        }

        return object : Tiger.Params, Predator.Params by predatorParams {
            override val stripeCount = stripeCount
        }
    }

    fun askUserForTiger(predatorParams: Predator.Params): Tiger {
        val tigerParams = askUserForTigerParams(predatorParams)
        return Tiger(tigerParams)
    }
}