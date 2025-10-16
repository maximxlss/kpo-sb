package ru.msk.xls

import org.koin.core.context.startKoin
import org.koin.dsl.module
import ru.msk.xls.interfaces.ThingRepository
import ru.msk.xls.interfaces.VetClinic
import ru.msk.xls.tui.CommandDispatcher


fun main() {
    val appModule = module {
        single<ThingRepository> { SimpleThingRepository() }
        single<VetClinic> { ExampleVetClinic }
    }
    startKoin { modules(appModule) }

    CommandDispatcher.run()
}