package com.ktome.core.inscription

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InscriptionCooldownTest {
    @Test
    fun `inscription cooldowns tick independently`() {
        val healing =
            InscriptionDef(
                id = "healing_light",
                nameKey = "inscription.healing_light.name",
                descKey = "inscription.healing_light.desc",
                iconKey = "icon.healing_light",
                category = InscriptionCategory.HEALING,
                cooldown = 3,
                effect = InscriptionEffect.Heal(percentMax = 0.2),
                tier = 1,
            )
        val movement =
            InscriptionDef(
                id = "phase_door",
                nameKey = "inscription.phase_door.name",
                descKey = "inscription.phase_door.desc",
                iconKey = "icon.phase_door",
                category = InscriptionCategory.MOVEMENT,
                cooldown = 1,
                effect = InscriptionEffect.Teleport(range = 8),
                tier = 1,
            )
        val cooldowns = InscriptionCooldownState()

        InscriptionManager.startCooldown(cooldowns, healing)
        InscriptionManager.startCooldown(cooldowns, movement)

        InscriptionManager.tickCooldowns(cooldowns)
        assertTrue(InscriptionManager.isOnCooldown(cooldowns, healing.id))
        assertFalse(InscriptionManager.isOnCooldown(cooldowns, movement.id))

        InscriptionManager.tickCooldowns(cooldowns)
        InscriptionManager.tickCooldowns(cooldowns)
        assertFalse(InscriptionManager.isOnCooldown(cooldowns, healing.id))
    }
}
