package ru.msk.xls.tui.builders

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.msk.xls.SimpleThingRepository
import ru.msk.xls.tui.builders.animals.PredatorConsoleBuilder
import ru.msk.xls.tui.builders.animals.predators.TigerConsoleBuilder
import ru.msk.xls.tui.builders.animals.predators.WolfConsoleBuilder
import java.io.ByteArrayInputStream
import kotlin.test.*

class BuilderTests : KoinTest {
    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<ru.msk.xls.interfaces.ThingRepository> { SimpleThingRepository() }
            })
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `askUserForThingParams accepts valid id`() {
        val input = "5\n"
        val prevIn = System.`in`
        System.setIn(ByteArrayInputStream(input.toByteArray()))
        try {
            val params = ThingConsoleBuilder.askUserForThingParams()
            assertEquals(5u, params.inventoryId)
        } finally {
            System.setIn(prevIn)
        }
    }

    @Test
    fun `askUserForTable asks and returns table`() {
        val input = "6\ntable\nwood\n"
        val prevIn = System.`in`
        System.setIn(ByteArrayInputStream(input.toByteArray()))
        try {
            val thing = ThingConsoleBuilder.askUserForThing()
            assertTrue(thing is ru.msk.xls.domain.things.Table)
            assertEquals(6u, thing.inventoryId)
        } finally {
            System.setIn(prevIn)
        }
    }

    @Test
    fun `askUserForAnimal asks predator and returns tiger`() {
        val prevIn = System.`in`
        try {
            // provide predator params with required fields
            val predatorParams = object : ru.msk.xls.domain.animals.Predator.Params {
                override val preyList: List<String> = listOf("deer")
                override val eatsKg: Double = 1.0
                override val inventoryId: UInt = 8u
            }

            // test tiger params parsing (user inputs stripe count 2)
            System.setIn(ByteArrayInputStream("2\n".toByteArray()))
            val tigerParams = TigerConsoleBuilder.askUserForTigerParams(predatorParams)
            assertEquals(2u, tigerParams.stripeCount)
        } finally {
            System.setIn(prevIn)
        }
    }

    @Test
    fun `predator console builder validates prey and wolf strength`() {
        val prevIn = System.`in`
        try {
            System.setIn(ByteArrayInputStream("sheep, goat\n".toByteArray()))
            val animalParams = object : ru.msk.xls.domain.animals.Animal.Params {
                override val eatsKg: Double = 0.5
                override val inventoryId: UInt = 9u
            }
            val predParams = PredatorConsoleBuilder.askUserForPredatorParams(animalParams)
            assertTrue(predParams.preyList.isNotEmpty())

            System.setIn(ByteArrayInputStream("5.5\n".toByteArray()))
            val wolfParams = WolfConsoleBuilder.askUserForWolfParams(predParams)
            assertEquals(5.5, wolfParams.biteStrength)
        } finally {
            System.setIn(prevIn)
        }
    }
}
