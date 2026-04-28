package com.ktome.core.inscription

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun `replacement keeps hotkey removes old cooldown and applies half candidate cooldown`() {
        val healing = inscription(id = "healing_light", category = InscriptionCategory.HEALING, cooldown = 12)
        val phase = inscription(id = "phase_door", category = InscriptionCategory.MOVEMENT, cooldown = 9)
        val controlledPhase = phase.copy(id = "controlled_phase", cooldown = 7)
        val loadout =
            InscriptionLoadout(
                mutableListOf(
                    InscriptionSlot(hotkey = 5, inscriptionId = healing.id),
                    InscriptionSlot(hotkey = 6, inscriptionId = phase.id),
                ),
            )
        val cooldowns = InscriptionCooldownState(mutableMapOf(phase.id to 3))

        val outcome =
            InscriptionManager.replace(
                InscriptionReplaceRequest(
                    loadout = loadout,
                    cooldowns = cooldowns,
                    equippedDefinitions = listOf(healing, phase),
                    candidate = controlledPhase,
                    targetHotkey = 6,
                ),
            )

        val applied = outcome as InscriptionReplaceOutcome.Applied
        assertEquals(listOf(5 to healing.id, 6 to controlledPhase.id), applied.newLoadout.slots.map { slot -> slot.hotkey to slot.inscriptionId })
        assertFalse(applied.newCooldownState.remainingByInscriptionId.containsKey(phase.id))
        assertEquals(4, applied.newCooldownState.remainingByInscriptionId[controlledPhase.id])
        assertEquals(6, applied.event.hotkey)
    }

    @Test
    fun `replacement rejects already equipped candidate in another slot`() {
        val healing = inscription(id = "healing_light", category = InscriptionCategory.HEALING)
        val phase = inscription(id = "phase_door", category = InscriptionCategory.MOVEMENT, cooldown = 9)
        val purge = inscription(id = "purge", category = InscriptionCategory.CLEANSING)
        val loadout =
            InscriptionLoadout(
                mutableListOf(
                    InscriptionSlot(hotkey = 5, inscriptionId = healing.id),
                    InscriptionSlot(hotkey = 6, inscriptionId = phase.id),
                    InscriptionSlot(hotkey = 7, inscriptionId = purge.id),
                ),
            )
        val cooldowns = InscriptionCooldownState(mutableMapOf(phase.id to 6))

        val outcome =
            InscriptionManager.replace(
                InscriptionReplaceRequest(
                    loadout = loadout,
                    cooldowns = cooldowns,
                    equippedDefinitions = listOf(healing, phase, purge),
                    candidate = phase,
                    targetHotkey = 7,
                ),
            )

        assertEquals(InscriptionReplaceOutcome.Rejected(InscriptionEquipFailure.SAME_INSCRIPTION), outcome)
        assertEquals(mapOf(phase.id to 6), cooldowns.remainingByInscriptionId)
    }

    @Test
    fun `replacement fails fast when target definition is missing`() {
        val healing = inscription(id = "healing_light", category = InscriptionCategory.HEALING)
        val phase = inscription(id = "phase_door", category = InscriptionCategory.MOVEMENT)
        val loadout =
            InscriptionLoadout(
                mutableListOf(
                    InscriptionSlot(hotkey = 5, inscriptionId = healing.id),
                    InscriptionSlot(hotkey = 6, inscriptionId = phase.id),
                ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            InscriptionManager.canReplace(
                loadout = loadout,
                equippedDefinitions = listOf(healing),
                candidate = phase.copy(id = "controlled_phase"),
                targetHotkey = 6,
            )
        }
    }

    @Test
    fun `replacement validates target same inscription and category limit`() {
        val healing = inscription(id = "healing_light", category = InscriptionCategory.HEALING)
        val healingSurge = inscription(id = "healing_surge", category = InscriptionCategory.HEALING)
        val phase = inscription(id = "phase_door", category = InscriptionCategory.MOVEMENT)
        val loadout =
            InscriptionLoadout(
                mutableListOf(
                    InscriptionSlot(hotkey = 5, inscriptionId = healing.id),
                    InscriptionSlot(hotkey = 6, inscriptionId = healingSurge.id),
                    InscriptionSlot(hotkey = 7, inscriptionId = phase.id),
                ),
            )

        assertEquals(
            InscriptionEquipCheck.Rejected(InscriptionEquipFailure.SAME_INSCRIPTION),
            InscriptionManager.canReplace(loadout, listOf(healing, healingSurge, phase), healing, targetHotkey = 5),
        )
        assertEquals(
            InscriptionEquipCheck.Rejected(InscriptionEquipFailure.CATEGORY_LIMIT),
            InscriptionManager.canReplace(
                loadout = loadout,
                equippedDefinitions = listOf(healing, healingSurge, phase),
                candidate = inscription(id = "healing_miracle", category = InscriptionCategory.HEALING),
                targetHotkey = 7,
            ),
        )
    }

    private fun inscription(
        id: String,
        category: InscriptionCategory,
        cooldown: Int = 10,
    ): InscriptionDef =
        InscriptionDef(
            id = id,
            nameKey = "inscription.$id.name",
            descKey = "inscription.$id.desc",
            iconKey = "icon.$id",
            category = category,
            cooldown = cooldown,
            effect = InscriptionEffect.Heal(percentMax = 0.2),
            tier = 1,
        )
}
