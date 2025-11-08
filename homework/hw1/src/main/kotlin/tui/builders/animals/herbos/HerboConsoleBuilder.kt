package ru.msk.xls.tui.builders.animals.herbos

import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.getOrRetry
import ru.msk.xls.validators.ValidationResult
import ru.msk.xls.validators.animals.HerboValidator

object HerboConsoleBuilder {
    val nextBuilders = mapOf<String, (Herbo.Params) -> Herbo>(
        "rabbit" to RabbitConsoleBuilder::askUserForRabbit,
        "monkey" to MonkeyConsoleBuilder::askUserForMonkey
    )

    fun askUserForHerboParams(animalParams: Animal.Params): Herbo.Params {
        val kindness = getOrRetry {
            print("Input kindness (0.0 - 10.0): ")
            val v = readln().toDoubleOrNull()
            if (v != null) {
                when (val r = HerboValidator.validateKindness(v)) {
                    is ValidationResult.Valid -> return@getOrRetry AskResult.ok(v)
                    is ValidationResult.Invalid -> return@getOrRetry AskResult.fail(r.reason)
                }
            }
            AskResult.fail("Invalid kindness")
        }

        return object : Herbo.Params, Animal.Params by animalParams {
            override val kindness: Double = kindness
        }
    }

    fun askUserForHerbo(animalParams: Animal.Params): Herbo {
        val herboParams = askUserForHerboParams(animalParams)
        val kinds = nextBuilders.keys.joinToString(", ")
        val nextBuilder = getOrRetry {
            println("Choose a herbo from: $kinds")
            print("Enter herbo kind: ")
            val kind = readln().lowercase()
            nextBuilders[kind]?.let {
                return@getOrRetry AskResult.ok(it)
            }
            AskResult.fail("Invalid kind.")
        }
        return nextBuilder(herboParams)
    }
}
