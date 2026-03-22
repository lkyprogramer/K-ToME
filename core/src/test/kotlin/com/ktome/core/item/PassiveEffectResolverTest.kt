package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PassiveEffectResolverTest {
    @Test
    fun `damage vs tag passive increases multiplier for matching target tags`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item =
                item(
                    baseId = "bandit_trophy",
                    passive = EquipmentPassive.DamageVsTag(tag = "bandit", bonusPercent = 0.15),
                ),
        )

        val adjustment =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = setOf("bandit", "humanoid"),
                damageType = DamageType.PHYSICAL,
            )

        assertEquals(1.15, adjustment.multiplier, 0.0001)
        assertEquals(1, adjustment.sources.size)
    }

    @Test
    fun `damage type bonus only applies to matching damage type`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item =
                item(
                    baseId = "furnace_talisman",
                    passive = EquipmentPassive.DamageTypeBonus(type = DamageType.FIRE, bonusPercent = 0.15),
                ),
        )

        val fireAdjustment =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = emptySet(),
                damageType = DamageType.FIRE,
            )
        val coldAdjustment =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = emptySet(),
                damageType = DamageType.COLD,
            )

        assertEquals(1.15, fireAdjustment.multiplier, 0.0001)
        assertEquals(1.0, coldAdjustment.multiplier, 0.0001)
    }

    @Test
    fun `hp regen per turn sums equipped regen passives`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item = item(baseId = "emerald_charm", passive = EquipmentPassive.HpRegenPerTurn(amount = 2)),
        )

        assertEquals(2, PassiveEffectResolver.hpRegenPerTurn(PassiveEffectResolver.equippedPassives(world, actor)))
    }

    @Test
    fun `resistance bonus aggregates by damage type`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item = item(baseId = "seal_reliquary", passive = EquipmentPassive.ResistanceBonus(DamageType.SHADOW, 10)),
        )
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.ARMOR,
            item = item(baseId = "shadow_cloak", passive = EquipmentPassive.ResistanceBonus(DamageType.SHADOW, 5)),
        )

        val bonuses = PassiveEffectResolver.resistanceBonuses(PassiveEffectResolver.equippedPassives(world, actor))

        assertEquals(15, bonuses[DamageType.SHADOW])
        assertTrue(DamageType.FIRE !in bonuses)
    }

    private fun equip(
        world: World,
        actor: com.ktome.core.ecs.EntityId,
        slot: EquipSlot,
        item: ItemInstance,
    ) {
        val itemId = world.createEntity()
        world.add(itemId, item)
        val equipment = world.get<Equipment>(actor) ?: Equipment().also { world.add(actor, it) }
        equipment.slots[slot] = itemId
    }

    private fun item(
        baseId: String,
        passive: EquipmentPassive,
    ): ItemInstance =
        ItemInstance(
            baseId = baseId,
            name = baseId,
            type = ItemType.ARMOR,
            slot = EquipSlot.OFF_HAND,
            glyph = ']',
            colorHex = "#FFFFFF",
            passive = passive,
        )
}
