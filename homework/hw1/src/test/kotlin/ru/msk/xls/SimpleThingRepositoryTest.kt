package ru.msk.xls

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import ru.msk.xls.domain.Thing
import ru.msk.xls.interfaces.ThingRepository
import kotlin.test.*

class SimpleThingRepositoryTest : KoinTest {
    private val repository: ThingRepository get() = get()

    private class FakeParams(override val inventoryId: UInt) : Thing.Params

    private class FakeThing(params: Params) : Thing(params) {
        override val inventoryName: String = "fake"
    }

    private val repositoryModule = module {
        single<ThingRepository> { SimpleThingRepository() }
    }

    @BeforeTest
    fun setUpRepository() {
        startKoin {
            modules(repositoryModule)
        }
    }

    @AfterTest
    fun tearDownRepository() {
        stopKoin()
    }

    @Test
    fun `addThing adds thing when id is unique`() {
        val thing = FakeThing(FakeParams(42u))

        val result = repository.addThing(thing)

        assertTrue(result is ThingRepository.CreationResult.Success)
        assertTrue(repository.things.contains(thing))
    }

    @Test
    fun `addThing returns Fail when id is taken`() {
        val first = FakeThing(FakeParams(7u))
        val duplicate = FakeThing(FakeParams(7u))
        repository.addThing(first)

        val failResult = when (val result = repository.addThing(duplicate)) {
            is ThingRepository.CreationResult.Fail -> result
            else -> fail("Expected fail result")
        }
        assertEquals("Inventory id taken", failResult.reason)
        assertEquals(1, repository.things.size)
    }

    @Test
    fun `isInventoryIdTaken reports presence after adding`() {
        val newThing = FakeThing(FakeParams(99u))
        assertFalse(repository.isInventoryIdTaken(99u))

        repository.addThing(newThing)

        assertTrue(repository.isInventoryIdTaken(99u))
    }
}
