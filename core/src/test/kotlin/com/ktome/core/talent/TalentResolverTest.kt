package com.ktome.core.talent

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentResolverTest {
    private val registry =
        TalentRegistry().apply {
            registerAll(
                listOf(
                    TalentDef(
                        id = "power_strike",
                        name = "猛击",
                        description = "",
                        staminaCost = 8,
                        cooldown = 3,
                        range = 1,
                        levelEffects =
                            mapOf(
                                1 to TalentLevelEffect(damageMultiplier = 1.5),
                                5 to TalentLevelEffect(damageMultiplier = 2.5, knockback = 1, armorBreakDuration = 3),
                            ),
                    ),
                    TalentDef(
                        id = "charge",
                        name = "冲锋",
                        description = "",
                        staminaCost = 12,
                        cooldown = 6,
                        range = 5,
                        minRange = 3,
                        levelEffects =
                            mapOf(
                                1 to TalentLevelEffect(damageMultiplier = 1.2),
                                5 to TalentLevelEffect(damageMultiplier = 1.8, stunDuration = 2),
                            ),
                    ),
                    TalentDef(
                        id = "shield_bash",
                        name = "盾击",
                        description = "",
                        staminaCost = 10,
                        cooldown = 5,
                        range = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.2, stunDuration = 2)),
                    ),
                    TalentDef(
                        id = "war_cry",
                        name = "战吼",
                        description = "",
                        staminaCost = 15,
                        cooldown = 10,
                        range = 0,
                        areaRadius = 3,
                        levelEffects = mapOf(5 to TalentLevelEffect(buffDuration = 8, buffMagnitude = 0.35, debuffMagnitude = 0.20, debuffDuration = 5)),
                    ),
                ),
            )
        }
    private val map =
        GameMap.fromAscii(
            rows =
                listOf(
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                    ".......",
                ),
            playerStart = Point(1, 2),
        )

    @Test
    fun `cannot use talent while on cooldown`() {
        val world = baseWorld()
        val player = createPlayer(world)

        val reason = resolver().canUse(world, map, player, "power_strike", Point(2, 2))

        assertEquals("猛击 is still cooling down.", reason)
    }

    @Test
    fun `cannot use talent without enough stamina`() {
        val world = baseWorld()
        val player = createPlayer(world, stamina = 5, cooldown = 0)
        createMonster(world, Point(2, 2))

        val reason = resolver().canUse(world, map, player, "power_strike", Point(2, 2))

        assertEquals("Not enough stamina.", reason)
    }

    @Test
    fun `power strike deals scaled damage and consumes stamina`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(2, 2))

        val result = resolver().resolve(world, map, player, "power_strike", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(6, requireNotNull(world.get<Health>(monster)).current)
        assertEquals(32, requireNotNull(world.get<Stamina>(player)).current)
        assertEquals(3, requireNotNull(world.get<CooldownState>(player)).remainingByTalentId["power_strike"])
    }

    @Test
    fun `charge requires minimum range and moves user adjacent to target`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val target = createMonster(world, Point(5, 2))

        val tooCloseReason = resolver().canUse(world, map, player, "charge", Point(2, 2))
        val result = resolver().resolve(world, map, player, "charge", Point(5, 2))

        assertEquals("Target is out of range.", tooCloseReason)
        assertTrue(result is TalentUseResult.Success)
        assertEquals(1, requireNotNull(world.get<Position>(player)).toPoint().chebyshevDistanceTo(Point(5, 2)))
        assertTrue(requireNotNull(world.get<Health>(target)).current < 40)
    }

    @Test
    fun `shield bash applies stun`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(2, 2))

        resolver().resolve(world, map, player, "shield_bash", Point(2, 2))

        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.STUNNED))
    }

    @Test
    fun `missed strike does not apply armor break or knockback`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, levelOverrides = mapOf("power_strike" to 5))
        val monster = createMonster(world, Point(2, 2))

        val result =
            resolver(TestRandomSource(doubles = listOf(0.99), ints = listOf(0)))
                .resolve(world, map, player, "power_strike", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(Point(2, 2), requireNotNull(world.get<Position>(monster)).toPoint())
        assertFalse(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.ARMOR_BREAK))
    }

    @Test
    fun `charge cannot cross walls`() {
        val sealedMap =
            GameMap.fromAscii(
                rows = listOf("#######", "#..#..#", "#######"),
                playerStart = Point(1, 1),
            )
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        createMonster(world, Point(5, 1))

        val reason = resolver().canUse(world, sealedMap, player, "charge", Point(5, 1))

        assertEquals("No path to charge target.", reason)
    }

    @Test
    fun `war cry buffs player and debuffs nearby enemies`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, levelOverrides = mapOf("war_cry" to 5))
        val nearby = createMonster(world, Point(3, 2))
        val farAway = createMonster(world, Point(6, 2))

        val result = resolver().resolve(world, map, player, "war_cry", null)

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(player)).has(StatusEffectType.WAR_CRY_BUFF))
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(nearby)).has(StatusEffectType.WAR_CRY_DEBUFF))
        assertFalse(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(farAway)).has(StatusEffectType.WAR_CRY_DEBUFF))
        assertTrue(requireNotNull(world.get<DerivedStats>(player)).attack > 25)
    }

    private fun baseWorld(): World = World()

    private fun resolver(
        randomSource: TestRandomSource = TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)),
    ): TalentResolver = TalentResolver(registry, CombatResolver(randomSource))

    private fun createPlayer(
        world: World,
        stamina: Int = 40,
        cooldown: Int = 3,
        levelOverrides: Map<String, Int> = emptyMap(),
    ): com.ktome.core.ecs.EntityId {
        val player = world.createEntity()
        world.add(player, Position(1, 2))
        world.add(player, BlocksMovement())
        world.add(player, FactionTag(Faction.PLAYER))
        world.add(player, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(player, CombatProfile(baseAttack = 5, baseDefense = 2, baseHp = 50, baseStamina = 40))
        world.add(
            player,
            TalentLoadout(
                slotToTalentId = linkedMapOf(1 to "power_strike", 2 to "charge", 3 to "shield_bash", 4 to "war_cry"),
                talentLevels = linkedMapOf("power_strike" to 1, "charge" to 1, "shield_bash" to 1, "war_cry" to 1).apply {
                    putAll(levelOverrides)
                },
            ),
        )
        world.add(
            player,
            CooldownState(
                remainingByTalentId =
                    linkedMapOf<String, Int>().apply {
                        if (cooldown > 0) {
                            this["power_strike"] = cooldown
                        }
                    },
            ),
        )
        world.add(player, com.ktome.core.talent.EffectTracker())
        val derived = StatsCalculator.calculate(world, player)
        world.add(player, derived)
        world.add(player, Health(current = derived.maxHp, max = derived.maxHp))
        world.add(player, Stamina(current = stamina, max = derived.maxStamina))
        return player
    }

    private fun createMonster(
        world: World,
        position: Point,
    ): com.ktome.core.ecs.EntityId {
        val monster = world.createEntity()
        world.add(monster, Position(position.x, position.y))
        world.add(monster, BlocksMovement())
        world.add(monster, FactionTag(Faction.MONSTER))
        world.add(monster, Stats(str = 6, dex = 6, con = 6, wil = 2))
        world.add(monster, CombatProfile(baseAttack = 4, baseDefense = 2, baseHp = 40))
        world.add(monster, com.ktome.core.talent.EffectTracker())
        val derived = StatsCalculator.calculate(world, monster)
        world.add(monster, derived)
        world.add(monster, Health(current = 40, max = derived.maxHp))
        return monster
    }
}
