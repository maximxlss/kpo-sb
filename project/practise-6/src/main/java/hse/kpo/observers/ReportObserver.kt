package hse.kpo.observers

import hse.kpo.builders.ReportBuilder
import hse.kpo.domains.Report
import hse.kpo.interfaces.SellObserver

class ReportObserver: SellObserver {
    val reportBuilder = ReportBuilder()

    override fun updateSell(operation: String) {
        reportBuilder.addOperation(operation)
    }

    fun buildReport() : Report = reportBuilder.build()
}