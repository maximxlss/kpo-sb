package ru.msk.xls.tui.commands

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.msk.xls.AskResult
import ru.msk.xls.domain.animals.Animal
import ru.msk.xls.getOrRetry
import ru.msk.xls.interfaces.ThingRepository
import ru.msk.xls.interfaces.VetClinic
import ru.msk.xls.printers.ThingPrinter
import ru.msk.xls.tui.builders.ThingConsoleBuilder

object CreateCommand : Command, KoinComponent {
    override val signature: String? = null
    override val description: String = "Add a thing (animals, computers, etc.)"

    private val clinic: VetClinic get() = get()
    private val repository: ThingRepository get() = get()

    override fun dispatch(args: String) {
        if (args.isNotBlank()) {
            println("Unexpected argument: $args")
            return
        }
        val thing = ThingConsoleBuilder.askUserForThing()
        if (thing is Animal) {
            val conclusion = clinic.checkup(thing)
            println("Vet clinic says: ${conclusion.report}")
            if (conclusion is VetClinic.Conclusion.Reject) {
                println("Vet clinic says reject. Reason: ${conclusion.reason}")
                return
            }
        }
        println("Verify thing:")
        println(ThingPrinter.stringify(thing).trim().prependIndent("| "))
        val verifyResult = getOrRetry {
            print("Ok? [ok/cancel] ")
            val line = readln().trim()
            if (line == "ok" || line == "cancel") {
                return@getOrRetry AskResult.ok(line)
            }
            AskResult.Failure("Invalid option.")
        }
        if (verifyResult == "cancel") {
            println("Canceled.")
            return
        }
        when (val result = repository.addThing(thing)) {
            is ThingRepository.CreationResult.Success -> println("Success!")
            is ThingRepository.CreationResult.Fail -> println("Failed: ${result.reason}")
        }
    }
}