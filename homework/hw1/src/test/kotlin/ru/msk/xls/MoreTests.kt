package ru.msk.xls

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import ru.msk.xls.domain.animals.Herbo
import ru.msk.xls.domain.animals.herbos.Monkey
import ru.msk.xls.domain.animals.herbos.Rabbit
import ru.msk.xls.domain.animals.predators.Tiger
import ru.msk.xls.domain.animals.predators.Wolf
import ru.msk.xls.domain.things.Table
import ru.msk.xls.interfaces.ThingRepository
import ru.msk.xls.printers.ThingPrinter
import ru.msk.xls.printers.animals.AnimalPrinter
import ru.msk.xls.printers.animals.HerboPrinter
import ru.msk.xls.printers.animals.PredatorPrinter
import ru.msk.xls.validators.ValidationResult
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

class MoreTests : KoinTest {
    private val repository: ThingRepository get() = get()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<ThingRepository> { SimpleThingRepository() }
            })
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `getOrRetry returns value after failures`() {
        var calls = 0
        val result = getOrRetry {
            calls++
            when (calls) {
                1 -> AskResult.fail("first")
                2 -> AskResult.fail(null)
                else -> AskResult.ok(123)
            }
        }

        assertEquals(123, result)
        assertTrue(calls >= 3)
    }

    @Test
    fun `ExampleVetClinic accepts healthy animals`() {
        val rabbit = Rabbit(object : Rabbit.Params {
            override val inventoryId: UInt = 1u
            override val eatsKg: Double = 0.5
            override val kindness: Double = 6.0
            override val jumpHeight: Double = 2.0
        })

        val tiger = Tiger(object : Tiger.Params {
            override val inventoryId: UInt = 2u
            override val eatsKg: Double = 1.0
            override val preyList: List<String> = listOf("deer")
            override val stripeCount: UInt = 5u
        })

        val res1 = ExampleVetClinic.checkup(rabbit)
        val res2 = ExampleVetClinic.checkup(tiger)

        assertTrue(res1 is ru.msk.xls.interfaces.VetClinic.Conclusion.Accept)
        assertTrue(res2 is ru.msk.xls.interfaces.VetClinic.Conclusion.Accept)
    }

    @Test
    fun `ExampleVetClinic rejects edge cases`() {
        val weakRabbit = Rabbit(object : Rabbit.Params {
            override val inventoryId: UInt = 3u
            override val eatsKg: Double = 0.2
            override val kindness: Double = 7.0
            override val jumpHeight: Double = 0.5
        })

        val weakTiger = Tiger(object : Tiger.Params {
            override val inventoryId: UInt = 4u
            override val eatsKg: Double = 0.2
            override val preyList: List<String> = emptyList()
            override val stripeCount: UInt = 1u
        })

        val r1 = ExampleVetClinic.checkup(weakRabbit)
        val r2 = ExampleVetClinic.checkup(weakTiger)

        assertTrue(r1 is ru.msk.xls.interfaces.VetClinic.Conclusion.Reject)
        assertTrue(r2 is ru.msk.xls.interfaces.VetClinic.Conclusion.Reject)
    }

    @Test
    fun `Herbo isKind threshold works`() {
        val kind = object : Herbo.Params {
            override val inventoryId: UInt = 10u
            override val eatsKg: Double = 0.1
            override val kindness: Double = 6.0
        }
        val notKind = object : Herbo.Params {
            override val inventoryId: UInt = 11u
            override val eatsKg: Double = 0.1
            override val kindness: Double = 3.0
        }

        val h1 = object : Herbo(kind) {
            override val inventoryName: String = "h1"
        }

        val h2 = object : Herbo(notKind) {
            override val inventoryName: String = "h2"
        }

        assertTrue(h1.isKind())
        assertFalse(h2.isKind())
    }

    @Test
    fun `ThingValidator returns invalid when id taken`() {
        // initially not taken
        assertEquals(ValidationResult.Valid, ru.msk.xls.validators.ThingValidator().validateInventoryId(99u))

        // add thing with id 99
        val fake = object : ru.msk.xls.domain.Thing(object : Params {
            override val inventoryId: UInt = 99u
        }) {
            override val inventoryName: String = "fake"
        }
        repository.addThing(fake)

        val res = ru.msk.xls.validators.ThingValidator().validateInventoryId(99u)
        assertTrue(res is ValidationResult.Invalid)
    }

    @Test
    fun `ThingPrinter prints table and animals and error for unknown`() {
        val table = Table(object : Table.Params {
            override val inventoryId: UInt = 20u
            override val material: String = "wood"
        })

        val tableStr = ThingPrinter.stringify(table)
        assertTrue(tableStr.contains("Thing #20: ${table.inventoryName}"))
        assertTrue(tableStr.contains("Material: wood"))

        val rabbit = Rabbit(object : Rabbit.Params {
            override val inventoryId: UInt = 21u
            override val eatsKg: Double = 0.3
            override val kindness: Double = 8.0
            override val jumpHeight: Double = 1.2
        })

        val rabbitStr = ThingPrinter.stringify(rabbit)
        assertTrue(rabbitStr.contains("Jump height: ${rabbit.jumpHeight}"))
        assertTrue(rabbitStr.contains("Eats per day (kg): ${rabbit.eatsKg}"))

        val monkey = Monkey(object : Monkey.Params {
            override val inventoryId: UInt = 22u
            override val eatsKg: Double = 0.7
            override val kindness: Double = 4.0
            override val furAmount: Double = 10.0
        })

        val monkeyStr = ThingPrinter.stringify(monkey)
        assertTrue(monkeyStr.contains("Fur amount: ${monkey.furAmount}"))

        val wolf = Wolf(object : Wolf.Params {
            override val inventoryId: UInt = 23u
            override val eatsKg: Double = 1.1
            override val preyList: List<String> = listOf("sheep")
            override val biteStrength: Double = 9.9
        })

        val wolfStr = ThingPrinter.stringify(wolf)
        assertTrue(wolfStr.contains("Bite strength: ${wolf.biteStrength}"))

        val unknown = object : ru.msk.xls.domain.Thing(object : Params {
            override val inventoryId: UInt = 999u
        }) {
            override val inventoryName: String = "unknown"
        }

        assertTrue(ThingPrinter.stringify(unknown).contains("please report this error"))
    }

    @Test
    fun `getOrRetry prints messages for failures`() {
        var calls = 0
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            val result = getOrRetry {
                calls++
                when (calls) {
                    1 -> AskResult.fail("first message")
                    2 -> AskResult.fail("second message")
                    else -> AskResult.ok(7)
                }
            }
            assertEquals(7, result)
        } finally {
            System.setOut(prev)
        }
        val printed = out.toString()
        assertTrue(printed.contains("first message"))
        assertTrue(printed.contains("second message"))
    }

    @Test
    fun `printers error on unknown runtime types`() {
        val anonAnimal = object : ru.msk.xls.domain.animals.Animal(object : Params {
            override val inventoryId: UInt = 50u
            override val eatsKg: Double = 0.5
        }) {
            override val inventoryName: String = "anon"
        }

        val anonHerbo = object : Herbo(object : Params {
            override val inventoryId: UInt = 51u
            override val eatsKg: Double = 0.2
            override val kindness: Double = 5.5
        }) {
            override val inventoryName: String = "hAnon"
        }

        val anonPred = object : ru.msk.xls.domain.animals.Predator(object : Params {
            override val inventoryId: UInt = 52u
            override val eatsKg: Double = 1.0
            override val preyList: List<String> = emptyList()
        }) {
            override val inventoryName: String = "pAnon"
        }

        assertTrue(AnimalPrinter.stringify(anonAnimal).contains("please report this error"))
        assertTrue(HerboPrinter.stringify(anonHerbo).contains("please report this error"))
        assertTrue(PredatorPrinter.stringify(anonPred).contains("please report this error"))
    }
}
