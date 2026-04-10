package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.resource.ResourceType
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
                targetStatusIds = emptySet(),
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
                targetStatusIds = emptySet(),
                damageType = DamageType.FIRE,
            )
        val coldAdjustment =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = emptySet(),
                targetStatusIds = emptySet(),
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
    fun `damage vs status passive increases multiplier for matching target status`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.WEAPON,
            item = item(baseId = "of_smite", passive = EquipmentPassive.DamageVsStatus(statusId = "BANE", bonusPercent = 0.12)),
        )

        val matching =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = emptySet(),
                targetStatusIds = setOf("BANE"),
                damageType = DamageType.HOLY,
            )
        val nonMatching =
            PassiveEffectResolver.resolveDamageAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                targetTags = emptySet(),
                targetStatusIds = setOf("MARKED"),
                damageType = DamageType.HOLY,
            )

        assertEquals(1.12, matching.multiplier, 0.0001)
        assertEquals(1.0, nonMatching.multiplier, 0.0001)
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

    @Test
    fun `equipped passives include affix granted passives on the same item`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.WEAPON,
            item =
                ItemInstance(
                    baseId = "templar_blade",
                    name = "Templar Blade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#FFFFFF",
                    affixes =
                        listOf(
                            AffixDef(
                                id = "of_smite",
                                name = "of Smite",
                                type = AffixType.SUFFIX,
                                cost = 10,
                                affixFamily = "holy_smite",
                                statModifiers = StatModifier(attack = 2),
                                passive = EquipmentPassive.DamageVsStatus(statusId = "BANE", bonusPercent = 0.15),
                            ),
                        ),
                    passive = EquipmentPassive.DamageTypeBonus(type = DamageType.HOLY, bonusPercent = 0.10),
                ),
        )

        val passives = PassiveEffectResolver.equippedPassives(world, actor)

        assertEquals(2, passives.size)
        assertTrue(passives.any { source -> source.passive is EquipmentPassive.DamageTypeBonus })
        assertTrue(passives.any { source -> source.passive is EquipmentPassive.DamageVsStatus && source.passive.statusId == "BANE" })
    }

    @Test
    fun `conditional and terrain stat bonuses aggregate when conditions match`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.ARMOR,
            item =
                item(
                    baseId = "last_stand_mail",
                    passive =
                        EquipmentPassive.ConditionalStatBonus(
                            condition = PassiveCondition.HP_BELOW_50,
                            statModifier = StatModifier(defense = 4, maxHp = 12),
                        ),
                ),
        )
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item =
                item(
                    baseId = "river_charm",
                    passive =
                        EquipmentPassive.TerrainAffinityBonus(
                            terrainTag = TerrainTag.WATER,
                            statModifier = StatModifier(accuracy = 3, speed = 2),
                        ),
                ),
        )

        val adjustment =
            PassiveEffectResolver.resolveStatAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                context =
                    PassiveStatContext(
                        healthFraction = 0.45,
                        terrainTags = setOf(TerrainTag.WATER),
                    ),
            )

        assertEquals(2, adjustment.sources.size)
        assertEquals(4, adjustment.modifier.defense)
        assertEquals(12, adjustment.modifier.maxHp)
        assertEquals(3, adjustment.modifier.accuracy)
        assertEquals(2, adjustment.modifier.speed)
    }

    @Test
    fun `conditional stat bonus self has status requires matching status id`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.ARMOR,
            item =
                item(
                    baseId = "warded_mail",
                    passive =
                        EquipmentPassive.ConditionalStatBonus(
                            condition = PassiveCondition.SELF_HAS_STATUS,
                            statusId = "GUARD",
                            statModifier = StatModifier(defense = 5),
                        ),
                ),
        )

        val matching =
            PassiveEffectResolver.resolveStatAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                context = PassiveStatContext(healthFraction = 1.0, selfStatusIds = setOf("GUARD")),
            )
        val nonMatching =
            PassiveEffectResolver.resolveStatAdjustment(
                passives = PassiveEffectResolver.equippedPassives(world, actor),
                context = PassiveStatContext(healthFraction = 1.0, selfStatusIds = setOf("BANE")),
            )

        assertEquals(5, matching.modifier.defense)
        assertEquals(0, nonMatching.modifier.defense)
    }

    @Test
    fun `on hit status proc collection preserves source metadata`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.WEAPON,
            item =
                ItemInstance(
                    baseId = "venom_blade",
                    name = "Venom Blade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#00FF66",
                    affixes =
                        listOf(
                            AffixDef(
                                id = "toxic_edge",
                                name = "Toxic Edge",
                                type = AffixType.SUFFIX,
                                cost = 10,
                                affixFamily = "toxic_edge",
                                statModifiers = StatModifier(),
                                passive = EquipmentPassive.OnHitStatusProc(statusId = "POISONED", chance = 0.35, duration = 3),
                            ),
                        ),
                ),
        )

        val trigger = PassiveEffectResolver.onHitStatusProcs(PassiveEffectResolver.equippedPassives(world, actor)).single()

        assertEquals("POISONED", trigger.statusId)
        assertEquals(0.35, trigger.chance, 0.0001)
        assertEquals(3, trigger.duration)
        assertEquals("venom_blade", trigger.source.item.baseId)
        assertEquals("toxic_edge", trigger.source.affixId)
    }

    @Test
    fun `on kill resource restore collection preserves resource type and amount`() {
        val world = World()
        val actor = world.createEntity()
        equip(
            world = world,
            actor = actor,
            slot = EquipSlot.OFF_HAND,
            item =
                item(
                    baseId = "reaper_idol",
                    passive = EquipmentPassive.OnKillResourceRestore(resourceType = ResourceType.MANA, amount = 6),
                ),
        )

        val trigger = PassiveEffectResolver.onKillResourceRestores(PassiveEffectResolver.equippedPassives(world, actor)).single()

        assertEquals(ResourceType.MANA, trigger.resourceType)
        assertEquals(6, trigger.amount)
        assertEquals("reaper_idol", trigger.source.item.baseId)
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
