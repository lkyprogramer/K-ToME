package com.ktome.core.inscription

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InscriptionSlotTest {
    @Test
    fun `loadout rejects fifth inscription`() {
        val loadout = InscriptionLoadout()
        val defs = buildList {
            repeat(4) { index ->
                add(
                    InscriptionDef(
                        id = "movement_$index",
                        nameKey = "inscription.movement_$index.name",
                        descKey = "inscription.movement_$index.desc",
                        iconKey = "icon.movement_$index",
                        category = if (index < 2) InscriptionCategory.MOVEMENT else InscriptionCategory.PROTECTION,
                        cooldown = 10,
                        effect =
                            if (index < 2) {
                                InscriptionEffect.Teleport(range = 6)
                            } else {
                                InscriptionEffect.Shield(amount = 40, duration = 3)
                            },
                        tier = 1,
                    ),
                )
            }
        }

        defs.forEachIndexed { index, definition ->
            assertTrue(InscriptionManager.equip(loadout, defs.take(index), definition))
        }

        assertFalse(
            InscriptionManager.equip(
                loadout = loadout,
                equippedDefinitions = defs,
                candidate =
                    InscriptionDef(
                        id = "healing_4",
                        nameKey = "inscription.healing_4.name",
                        descKey = "inscription.healing_4.desc",
                        iconKey = "icon.healing_4",
                        category = InscriptionCategory.HEALING,
                        cooldown = 10,
                        effect = InscriptionEffect.Heal(percentMax = 0.2),
                        tier = 1,
                    ),
            ),
        )
    }

    @Test
    fun `loadout rejects third inscription in same category`() {
        val loadout = InscriptionLoadout()
        val first =
            InscriptionDef(
                id = "healing_light",
                nameKey = "inscription.healing_light.name",
                descKey = "inscription.healing_light.desc",
                iconKey = "icon.healing_light",
                category = InscriptionCategory.HEALING,
                cooldown = 12,
                effect = InscriptionEffect.Heal(percentMax = 0.2),
                tier = 1,
            )
        val second = first.copy(id = "healing_surge")
        val third = first.copy(id = "healing_miracle", tier = 3, cooldown = 20)

        assertTrue(InscriptionManager.equip(loadout, emptyList(), first))
        assertTrue(InscriptionManager.equip(loadout, listOf(first), second))
        assertFalse(InscriptionManager.equip(loadout, listOf(first, second), third))
    }
}
