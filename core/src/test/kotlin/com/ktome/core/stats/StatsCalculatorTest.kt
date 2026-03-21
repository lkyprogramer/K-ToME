package com.ktome.core.stats

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.Equipment
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.StatusEffectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatsCalculatorTest {
    @Test
    fun `equipment modifiers feed derived stats through world calculation`() {
        val world = World()
        val actor = world.createEntity()
        val sword = world.createEntity()
        world.add(actor, Stats(str = 10, dex = 8, con = 9, wil = 7))
        world.add(actor, CombatProfile(baseAttack = 5, baseDefense = 2, baseSpeed = 100, baseHp = 40, baseStamina = 30))
        world.add(actor, Equipment(slots = linkedMapOf(EquipSlot.WEAPON to sword)))
        world.add(
            sword,
            ItemInstance(
                baseId = "steel_sword",
                name = "钢制短剑",
                type = ItemType.WEAPON,
                slot = EquipSlot.WEAPON,
                glyph = ')',
                colorHex = "#FFFFFF",
                stats = StatModifier(attack = 4, speed = 6, maxHp = 8),
            ),
        )

        val derived = StatsCalculator.calculate(world, actor)

        assertEquals(29, derived.attack)
        assertEquals(110, derived.speed)
        assertEquals(120, derived.maxHp)
    }

    @Test
    fun `effects can buff attack and debuff defense`() {
        val world = World()
        val actor = world.createEntity()
        world.add(actor, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(actor, CombatProfile(baseAttack = 5, baseDefense = 10))
        world.add(
            actor,
            EffectTracker(
                mutableListOf(
                    ActiveEffect(
                        id = "war_cry",
                        name = "War Cry",
                        type = StatusEffectType.WAR_CRY_BUFF,
                        remainingTurns = 3,
                        statModifiers = StatModifier(attackMultiplierBonus = 0.20),
                    ),
                    ActiveEffect(
                        id = "armor_break",
                        name = "Armor Break",
                        type = StatusEffectType.ARMOR_BREAK,
                        remainingTurns = 2,
                        statModifiers = StatModifier(defense = -3),
                    ),
                ),
            ),
        )

        val derived = StatsCalculator.calculate(world, actor)

        assertEquals(30, derived.attack)
        assertEquals(7, derived.defense)
    }

    @Test
    fun `recalculate updates stored derived stats and resource caps`() {
        val world = World()
        val actor = world.createEntity()
        val armor = world.createEntity()
        world.add(actor, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(actor, CombatProfile(baseAttack = 5, baseDefense = 2, baseHp = 40, baseStamina = 20))
        world.add(actor, Equipment(slots = linkedMapOf(EquipSlot.ARMOR to armor)))
        world.add(actor, Health(current = 20, max = 120))
        world.add(actor, Stamina(current = 7, max = 70))
        world.add(
            actor,
            ResourcePools(
                linkedMapOf(
                    ResourceType.STAMINA to ResourcePool(type = ResourceType.STAMINA, current = 7, max = 70),
                ),
            ),
        )
        world.add(
            armor,
            ItemInstance(
                baseId = "plate",
                name = "板甲",
                type = ItemType.ARMOR,
                slot = EquipSlot.ARMOR,
                glyph = '[',
                colorHex = "#FFFFFF",
                stats = StatModifier(defense = 5, maxHp = 20, maxStamina = 10),
            ),
        )

        val derived = StatsCalculator.recalculateAndStore(world, actor)

        assertEquals(140, derived.maxHp)
        assertEquals(80, derived.maxStamina)
        assertEquals(40, requireNotNull(world.get<Health>(actor)).current)
        assertEquals(17, requireNotNull(world.get<Stamina>(actor)).current)
        assertEquals(17, requireNotNull(world.get<ResourcePools>(actor)).pool(ResourceType.STAMINA)?.current)
        assertEquals(80, requireNotNull(world.get<ResourcePools>(actor)).pool(ResourceType.STAMINA)?.max)
        assertTrue(requireNotNull(world.get<com.ktome.core.ecs.DerivedStats>(actor)).defense >= 7)
    }
}
