package ru.msk.xls.tui.builders.animals.herbos

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.domain.animals.herbos.Rabbit
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.herbos.RabbitValidator

object RabbitConsoleBuilder {
    fun askUserForRabbitParams(herboParams: Herbo.Params): Rabbit.Params {
        val jumpHeight = getOrRetry {
            print("Input its jump height (meters): ")
            val v = readln().toDoubleOrNull()
            if (v != null) {
                when (val r = RabbitValidator.validateJumpHeight(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid jump height")
        }

        return object : Rabbit.Params, Herbo.Params by herboParams {
            override val jumpHeight: Double = jumpHeight
        }
    }

    fun askUserForRabbit(herboParams: Herbo.Params): Rabbit {
        val params = askUserForRabbitParams(herboParams)
        return Rabbit(params)
    }
}
