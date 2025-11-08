package ru.msk.xls.interfaces

import ru.msk.xls.domain.animals.Animal

interface VetClinic {
    sealed class Conclusion(val report: String) {
        class Accept(report: String) : Conclusion(report)
        class Reject(report: String, val reason: String) : Conclusion(report)
    }

    fun checkup(animal: Animal): Conclusion
}