package ru.msk.xls.validators

import ru.msk.xls.validators.animals.AnimalValidator
import ru.msk.xls.validators.animals.PredatorValidator
import ru.msk.xls.validators.animals.herbos.MonkeyValidator
import ru.msk.xls.validators.animals.herbos.RabbitValidator
import ru.msk.xls.validators.animals.predators.TigerValidator
import ru.msk.xls.validators.animals.predators.WolfValidator
import ru.msk.xls.validators.things.TableValidator
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidatorTests {
    @Test
    fun `animal eats validation`() {
        assertTrue(AnimalValidator.validateEatsKg(0.0) is ValidationResult.Valid)
        assertTrue(AnimalValidator.validateEatsKg(1.0) is ValidationResult.Valid)
        val v = AnimalValidator.validateEatsKg(-0.1)
        assertTrue(v is ValidationResult.Invalid)
    }

    @Test
    fun `rabbit jump validation`() {
        assertTrue(RabbitValidator.validateJumpHeight(0.0) is ValidationResult.Valid)
        assertTrue(RabbitValidator.validateJumpHeight(2.0) is ValidationResult.Valid)
        assertTrue(RabbitValidator.validateJumpHeight(-1.0) is ValidationResult.Invalid)
    }

    @Test
    fun `monkey fur validation`() {
        assertTrue(MonkeyValidator.validateFurAmount(0.0) is ValidationResult.Valid)
        assertTrue(MonkeyValidator.validateFurAmount(5.0) is ValidationResult.Valid)
        assertTrue(MonkeyValidator.validateFurAmount(-0.5) is ValidationResult.Invalid)
    }

    @Test
    fun `predator prey list validation`() {
        assertTrue(PredatorValidator.validatePreyList(listOf("deer")) is ValidationResult.Valid)
        assertTrue(PredatorValidator.validatePreyList(emptyList()) is ValidationResult.Invalid)
        assertTrue(PredatorValidator.validatePreyList(listOf(" ")) is ValidationResult.Invalid)
    }

    @Test
    fun `tiger stripe validation`() {
        assertTrue(TigerValidator.validateStripeCount(1u) is ValidationResult.Valid)
        assertTrue(TigerValidator.validateStripeCount(0u) is ValidationResult.Invalid)
    }

    @Test
    fun `wolf bite validation`() {
        assertTrue(WolfValidator.validateBiteStrength(0.0) is ValidationResult.Valid)
        assertTrue(WolfValidator.validateBiteStrength(3.3) is ValidationResult.Valid)
        assertTrue(WolfValidator.validateBiteStrength(-1.0) is ValidationResult.Invalid)
    }

    @Test
    fun `table material validation`() {
        assertTrue(TableValidator.validateMaterial("wood") is ValidationResult.Valid)
        assertTrue(TableValidator.validateMaterial("") is ValidationResult.Invalid)
        assertTrue(TableValidator.validateMaterial("   ") is ValidationResult.Invalid)
    }
}
