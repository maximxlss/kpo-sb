package ru.msk.xls.tui.builders.animals

import ru.msk.xls.AskResult
import ru.msk.xls.domain.Thing
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.getOrRetry
import ru.msk.xls.tui.builders.animals.herbos.HerboConsoleBuilder
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.AnimalValidator


object AnimalConsoleBuilder {
    val nextBuilders = mapOf(
        "predator" to PredatorConsoleBuilder::askUserForPredator,
        "herbo" to HerboConsoleBuilder::askUserForHerbo
    )

    fun askUserForAnimalParams(thingParams: Thing.Params): Animal.Params {
        val eatsKg = getOrRetry {
            print("Input how much it eats each day, in kg: ")
            val v = readln().toDoubleOrNull()
            if (v != null) {
                when (val r = AnimalValidator.validateEatsKg(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid value.")
        }

        return object : Animal.Params, Thing.Params by thingParams {
            override val eatsKg = eatsKg
        }
    }

    fun askUserForAnimal(thingParams: Thing.Params): Animal {
        val animalParams = askUserForAnimalParams(thingParams)
        val kinds = nextBuilders.keys.joinToString(" ")
        val nextBuilder = getOrRetry {
            println("Choose an animal kind from: $kinds")
            print("Enter animal kind: ")
            val kind = readln().lowercase()
            nextBuilders[kind]?.let {
                return@getOrRetry AskResult.ok(it)
            }
            AskResult.fail("Invalid kind.")
        }
        return nextBuilder(animalParams)
    }
}