package ru.msk.xls

import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.domain.animals.herbos.Rabbit
import ru.msk.xls.domain.animals.predators.Tiger
import ru.msk.xls.interfaces.VetClinic

object ExampleVetClinic : VetClinic {
    override fun checkup(animal: Animal): VetClinic.Conclusion {
        if (animal.eatsKg < 0.1) {
            return VetClinic.Conclusion.Reject("Overall healthy.", "${animal.eatsKg} kg per day is not healthy!")
        }
        when (animal) {
            is Tiger -> if (animal.stripeCount < 3u) {
                return VetClinic.Conclusion.Reject("Overall healthy.", "Stripe count too low!")
            }

            is Rabbit -> if (animal.jumpHeight < 1) {
                return VetClinic.Conclusion.Reject("Overall healthy.", "Rabbits should jump higher!")
            }
        }
        return VetClinic.Conclusion.Accept("Fully healthy.")
    }
}