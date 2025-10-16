package ru.msk.xls.tui.builders.animals

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.domain.animals.Predator
import ru.msk.xls.getOrRetry
import ru.msk.xls.tui.builders.animals.predators.TigerConsoleBuilder
import ru.msk.xls.tui.builders.animals.predators.WolfConsoleBuilder
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.PredatorValidator

object PredatorConsoleBuilder {
    val nextBuilders = mapOf<String, (Predator.Params) -> Predator>(
        "wolf" to WolfConsoleBuilder::askUserForWolf,
        "tiger" to TigerConsoleBuilder::askUserForTiger
    )

    fun askUserForPredatorParams(animalParams: Animal.Params): Predator.Params {
        val preyList = getOrRetry {
            print("Input what it preys on, delimited by commas: ")
            val list = readln().split(",").map { it.trim().lowercase() }
            when (val r = PredatorValidator.validatePreyList(list)) {
                is ValidationResult.Valid -> return@getOrRetry AskResult.ok(list)
                is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
            }
        }

        return object : Predator.Params, Animal.Params by animalParams {
            override val preyList = preyList
        }
    }

    fun askUserForPredator(animalParams: Animal.Params): Predator {
        val predatorParams = askUserForPredatorParams(animalParams)
        val kinds = nextBuilders.keys.joinToString(", ")
        val nextBuilder = getOrRetry {
            println("Choose a predator from: $kinds")
            print("Enter predator kind: ")
            val kind = readln().lowercase()
            nextBuilders[kind]?.let {
                return@getOrRetry AskResult.ok(it)
            }
            AskResult.fail("Invalid kind.")
        }
        return nextBuilder(predatorParams)
    }
}