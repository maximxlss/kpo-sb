package ru.msk.xls.tui

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import ru.msk.xls.SimpleThingRepository
import ru.msk.xls.domain.animals.herbos.Rabbit
import ru.msk.xls.domain.things.Table
import ru.msk.xls.tui.commands.CreateCommand
import ru.msk.xls.tui.commands.ListAnimalsCommand
import ru.msk.xls.tui.commands.ListThingsCommand
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

class TuiTests : KoinTest {
    private val repository: ru.msk.xls.interfaces.ThingRepository get() = get()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single { SimpleThingRepository() as ru.msk.xls.interfaces.ThingRepository }
                single<ru.msk.xls.interfaces.VetClinic> { ru.msk.xls.ExampleVetClinic }
            })
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `ListThingsCommand prints nothing when empty and unexpected args`() {
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ListThingsCommand.dispatch("unexpected")
            ListThingsCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Unexpected argument"))
        assertTrue(s.contains("All the things:"))
        assertTrue(s.contains("Number of things: 0"))
    }

    @Test
    fun `ListAnimalsCommand prints animal stats and totals`() {
        // add a table and an animal to repository
        val table = Table(object : Table.Params {
            override val inventoryId: UInt = 80u
            override val material: String = "metal"
        })
        repository.addThing(table)

        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ListAnimalsCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("All the animals:"))
        assertTrue(s.contains("Number of animals: 0"))
    }

    @Test
    fun `CommandDispatcher help lists commands`() {
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            CommandDispatcher.HelpCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("The following commands are supported"))
        assertTrue(s.contains("create"))
    }

    @Test
    fun `CreateCommand cancel flow does not add thing when user cancels`() {
        // prepare input: inventory id, kind 'table', material, then verify 'cancel'
        val input = listOf("1", "table", "wood", "cancel").joinToString("\n") + "\n"
        val prevIn = System.`in`
        val inStream = ByteArrayInputStream(input.toByteArray())
        System.setIn(inStream)

        val out = ByteArrayOutputStream()
        val prevOut = System.out
        System.setOut(PrintStream(out))

        try {
            CreateCommand.dispatch("")
        } finally {
            System.setIn(prevIn)
            System.setOut(prevOut)
        }

        // repository should still be empty because user cancelled
        assertTrue(repository.things.isEmpty())
    }

    @Test
    fun `CommandDispatcher processOneLine handles invalid command and exit toggles running`() {
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            // invalid
            val prevIn = System.`in`
            try {
                System.setIn(ByteArrayInputStream("invalid\n".toByteArray()))
                CommandDispatcher.processOneLine()
            } finally {
                System.setIn(prevIn)
            }

            // call exit command to toggle running
            CommandDispatcher.ExitCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Bye!") || !CommandDispatcher.running)
    }

    @Test
    fun `CreateCommand ok flow adds thing`() {
        val input = listOf("2", "table", "plastic", "ok").joinToString("\n") + "\n"
        val prevIn = System.`in`
        val inStream = ByteArrayInputStream(input.toByteArray())
        System.setIn(inStream)

        val out = ByteArrayOutputStream()
        val prevOut = System.out
        System.setOut(PrintStream(out))

        try {
            CreateCommand.dispatch("")
        } finally {
            System.setIn(prevIn)
            System.setOut(prevOut)
        }

        val s = out.toString()
        // it should have added the thing
        assertTrue(s.contains("Success!") || repository.things.isNotEmpty())
        assertFalse(repository.things.isEmpty())
    }

    @Test
    fun `ListThingsCommand prints things when non-empty`() {
        val table = Table(object : Table.Params {
            override val inventoryId: UInt = 90u
            override val material: String = "glass"
        })
        repository.addThing(table)

        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ListThingsCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Thing #90: "))
        assertTrue(s.contains("Number of things:"))
    }

    @Test
    fun `ListAnimalsCommand prints when an animal present`() {
        val rabbit = Rabbit(object : Rabbit.Params {
            override val inventoryId: UInt = 77u
            override val eatsKg: Double = 0.4
            override val kindness: Double = 8.0
            override val jumpHeight: Double = 1.1
        })
        repository.addThing(rabbit)

        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ListAnimalsCommand.dispatch("")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Animal #77"))
        assertTrue(s.contains("Total amount of food per day"))
    }

    @Test
    fun `HelpCommand handles unexpected args branch`() {
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            CommandDispatcher.HelpCommand.dispatch("something")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Unexpected arguments"))
        assertTrue(s.contains("The following commands are supported"))
    }

    @Test
    fun `processOneLine handles help command via stdin`() {
        val prevIn = System.`in`
        try {
            System.setIn(ByteArrayInputStream("help\n".toByteArray()))
            val out = ByteArrayOutputStream()
            val prev = System.out
            System.setOut(PrintStream(out))
            try {
                CommandDispatcher.processOneLine()
            } finally {
                System.setOut(prev)
            }
            val s = out.toString()
            assertTrue(s.contains("The following commands are supported"))
        } finally {
            System.setIn(prevIn)
        }
    }

    @Test
    fun `ExitCommand prints unexpected args and does not stop when args provided`() {
        CommandDispatcher.running = true
        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            CommandDispatcher.ExitCommand.dispatch("someArg")
        } finally {
            System.setOut(prev)
        }
        val s = out.toString()
        assertTrue(s.contains("Unexpected arguments"))
        // should not toggle running because it returned early
        assertTrue(CommandDispatcher.running)
    }

    @Test
    fun `processOneLine prints invalid command message listing supported commands`() {
        val prevIn = System.`in`
        try {
            System.setIn(ByteArrayInputStream("unknowncmd\n".toByteArray()))
            val out = ByteArrayOutputStream()
            val prev = System.out
            System.setOut(PrintStream(out))
            try {
                CommandDispatcher.processOneLine()
            } finally {
                System.setOut(prev)
            }
            val s = out.toString()
            assertTrue(s.contains("Invalid command. Supported commands:"))
        } finally {
            System.setIn(prevIn)
        }
    }
}
