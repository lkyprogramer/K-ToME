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
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
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
                    TalentDef(
                        id = "sweeping_strike",
                        name = "横扫",
                        description = "",
                        staminaCost = 10,
                        cooldown = 4,
                        range = 1,
                        areaRadius = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.2)),
                    ),
                    TalentDef(
                        id = "sunder_armor",
                        name = "碎甲",
                        description = "",
                        staminaCost = 12,
                        cooldown = 5,
                        range = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.1, armorBreakDuration = 2)),
                    ),
                    TalentDef(
                        id = "guard_stance",
                        name = "格挡姿态",
                        description = "",
                        staminaCost = 8,
                        cooldown = 7,
                        range = 0,
                        levelEffects = mapOf(1 to TalentLevelEffect(buffDuration = 3, buffMagnitude = 0.10)),
                    ),
                    TalentDef(
                        id = "intimidation",
                        name = "威压",
                        description = "",
                        staminaCost = 10,
                        cooldown = 8,
                        range = 0,
                        areaRadius = 4,
                        levelEffects = mapOf(1 to TalentLevelEffect(debuffMagnitude = 0.10, debuffDuration = 3)),
                    ),
                    TalentDef(
                        id = "unyielding",
                        name = "不屈",
                        description = "",
                        staminaCost = 14,
                        cooldown = 12,
                        range = 0,
                        levelEffects = mapOf(1 to TalentLevelEffect(buffDuration = 3, buffMagnitude = 0.15)),
                    ),
                    TalentDef(
                        id = "fireball",
                        name = "火球",
                        description = "",
                        staminaCost = 0,
                        cooldown = 3,
                        range = 6,
                        minRange = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.3)),
                    ),
                    TalentDef(
                        id = "flame_wall",
                        name = "烈焰之墙",
                        description = "",
                        staminaCost = 0,
                        cooldown = 6,
                        range = 4,
                        minRange = 1,
                        areaRadius = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.1)),
                    ),
                    TalentDef(
                        id = "ice_bolt",
                        name = "冰箭",
                        description = "",
                        staminaCost = 0,
                        cooldown = 3,
                        range = 6,
                        minRange = 1,
                        levelEffects = mapOf(5 to TalentLevelEffect(damageMultiplier = 1.8, stunDuration = 1), 1 to TalentLevelEffect(damageMultiplier = 1.2)),
                    ),
                    TalentDef(
                        id = "frost_nova",
                        name = "霜冻新星",
                        description = "",
                        staminaCost = 0,
                        cooldown = 7,
                        range = 0,
                        areaRadius = 2,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.0, stunDuration = 1)),
                    ),
                    TalentDef(
                        id = "ice_prison",
                        name = "冰封",
                        description = "",
                        staminaCost = 0,
                        cooldown = 8,
                        range = 5,
                        minRange = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 0.8, stunDuration = 1)),
                    ),
                    TalentDef(
                        id = "blink",
                        name = "闪现",
                        description = "",
                        staminaCost = 0,
                        cooldown = 6,
                        range = 5,
                        minRange = 2,
                        levelEffects = mapOf(1 to TalentLevelEffect()),
                    ),
                    TalentDef(
                        id = "arcane_shield",
                        name = "奥术护盾",
                        description = "",
                        staminaCost = 0,
                        cooldown = 8,
                        range = 0,
                        levelEffects = mapOf(1 to TalentLevelEffect(buffDuration = 3, buffMagnitude = 0.10)),
                    ),
                    TalentDef(
                        id = "mana_surge",
                        name = "法力涌动",
                        description = "",
                        staminaCost = 0,
                        cooldown = 10,
                        range = 0,
                        levelEffects = mapOf(1 to TalentLevelEffect(buffDuration = 2, buffMagnitude = 0.10)),
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

    @Test
    fun `guard stance buffs player defense`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)

        val result = resolver().resolve(world, map, player, "guard_stance", null)

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(player)).has(StatusEffectType.GUARD_STANCE_BUFF))
    }

    @Test
    fun `sweeping strike hits multiple adjacent targets`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val primary = createMonster(world, Point(2, 2))
        val secondary = createMonster(world, Point(2, 3))

        val result =
            resolver(TestRandomSource(doubles = listOf(0.0, 0.0, 0.0), ints = listOf(0)))
                .resolve(world, map, player, "sweeping_strike", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(primary)).current < 40)
        assertTrue(requireNotNull(world.get<Health>(secondary)).current < 40)
    }

    @Test
    fun `intimidation debuffs nearby enemies`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val nearby = createMonster(world, Point(3, 2))

        val result = resolver().resolve(world, map, player, "intimidation", null)

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(nearby)).has(StatusEffectType.WAR_CRY_DEBUFF))
    }

    @Test
    fun `fireball damages ranged target`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(4, 2))

        val result = resolver().resolve(world, map, player, "fireball", Point(4, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(monster)).current < 40)
    }

    @Test
    fun `blink repositions user to requested tile`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)

        val result = resolver().resolve(world, map, player, "blink", Point(4, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(Point(4, 2), requireNotNull(world.get<Position>(player)).toPoint())
    }

    @Test
    fun `mana surge restores mana when resource pool is present`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        world.add(
            player,
            ResourcePools(
                linkedMapOf(
                    ResourceType.MANA to ResourcePool(type = ResourceType.MANA, current = 20, max = 100),
                ),
            ),
        )

        val result = resolver().resolve(world, map, player, "mana_surge", null)

        assertTrue(result is TalentUseResult.Success)
        assertEquals(30, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.MANA)?.current)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(player)).has(StatusEffectType.MANA_SURGE_BUFF))
    }

    @Test
    fun `unsupported talent fails without spending stamina or cooldown`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        registry.register(
            TalentDef(
                id = "unsupported",
                name = "未实现技能",
                description = "",
                staminaCost = 9,
                cooldown = 4,
                range = 0,
                levelEffects = mapOf(1 to TalentLevelEffect()),
            ),
        )
        requireNotNull(world.get<TalentLoadout>(player)).talentLevels["unsupported"] = 1

        val beforeStamina = requireNotNull(world.get<Stamina>(player)).current
        val beforeCooldowns = requireNotNull(world.get<CooldownState>(player)).remainingByTalentId.toMap()

        val result = resolver().resolve(world, map, player, "unsupported", null)

        assertTrue(result is TalentUseResult.Failure)
        assertEquals(beforeStamina, requireNotNull(world.get<Stamina>(player)).current)
        assertEquals(beforeCooldowns, requireNotNull(world.get<CooldownState>(player)).remainingByTalentId)
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
                slotToTalentId =
                    linkedMapOf(
                        1 to "power_strike",
                        2 to "charge",
                        3 to "shield_bash",
                        4 to "war_cry",
                        5 to "sweeping_strike",
                        6 to "sunder_armor",
                        7 to "guard_stance",
                        8 to "intimidation",
                        9 to "unyielding",
                        10 to "fireball",
                        11 to "flame_wall",
                        12 to "ice_bolt",
                        13 to "frost_nova",
                        14 to "ice_prison",
                        15 to "blink",
                        16 to "arcane_shield",
                        17 to "mana_surge",
                    ),
                talentLevels =
                    linkedMapOf(
                        "power_strike" to 1,
                        "charge" to 1,
                        "shield_bash" to 1,
                        "war_cry" to 5,
                        "sweeping_strike" to 1,
                        "sunder_armor" to 1,
                        "guard_stance" to 1,
                        "intimidation" to 1,
                        "unyielding" to 1,
                        "fireball" to 1,
                        "flame_wall" to 1,
                        "ice_bolt" to 1,
                        "frost_nova" to 1,
                        "ice_prison" to 1,
                        "blink" to 1,
                        "arcane_shield" to 1,
                        "mana_surge" to 1,
                    ).apply {
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
