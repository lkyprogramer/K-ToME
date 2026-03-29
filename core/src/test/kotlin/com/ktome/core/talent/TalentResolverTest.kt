package com.ktome.core.talent

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.SaveDimension
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.status.EffectCategory
import com.ktome.core.status.ReplacePolicy
import com.ktome.core.status.StackingRule
import com.ktome.core.status.StatusCatalog
import com.ktome.core.status.StatusEffectDef
import com.ktome.core.status.StatusEffectType
import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentResolverTest {
    private fun staminaCosts(cost: Int): Map<ResourceType, Int> = linkedMapOf(ResourceType.STAMINA to cost)

    private fun selfEffect(
        effectId: String,
        statusId: String,
        duration: Int,
        magnitude: Double,
    ): AssociatedStatusEffect =
        AssociatedStatusEffect(
            effectId = effectId,
            statusId = statusId,
            trigger = EffectTrigger.ON_CAST,
            targetScope = EffectTargetScope.SELF,
            applicationPolicy = ApplicationPolicy.SELF_AUTO,
            duration = duration,
            magnitude = magnitude,
        )

    private fun hostileOnHitEffect(
        effectId: String,
        statusId: String,
        duration: Int,
        saveDimension: SaveDimension,
        magnitude: Double = 0.0,
    ): AssociatedStatusEffect =
        AssociatedStatusEffect(
            effectId = effectId,
            statusId = statusId,
            trigger = EffectTrigger.ON_HIT,
            targetScope = EffectTargetScope.PRIMARY_TARGET,
            applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
            saveDimension = saveDimension,
            duration = duration,
            magnitude = magnitude,
        )

    private fun hostileSaveOnlyEffect(
        effectId: String,
        statusId: String,
        duration: Int,
        saveDimension: SaveDimension,
        magnitude: Double,
    ): AssociatedStatusEffect =
        AssociatedStatusEffect(
            effectId = effectId,
            statusId = statusId,
            trigger = EffectTrigger.ON_CAST,
            targetScope = EffectTargetScope.HOSTILES_IN_RADIUS,
            applicationPolicy = ApplicationPolicy.HOSTILE_SAVE_ONLY,
            saveDimension = saveDimension,
            duration = duration,
            magnitude = magnitude,
        )

    private fun selfCleanse(
        effectId: String,
        maxEffectsRemoved: Int = 1,
    ): CleanseEffect =
        CleanseEffect(
            effectId = effectId,
            trigger = EffectTrigger.ON_CAST,
            targetScope = EffectTargetScope.SELF,
            applicationPolicy = ApplicationPolicy.INSTANT_ACTION,
            maxEffectsRemoved = maxEffectsRemoved,
        )

    private val registry =
        TalentRegistry().apply {
            registerAll(
                listOf(
                    TalentDef(
                        id = "power_strike",
                        name = "猛击",
                        description = "",
                        resourceCosts = staminaCosts(8),
                        cooldown = 3,
                        range = 1,
                        levelEffects =
                            mapOf(
                                1 to TalentLevelEffect(damageMultiplier = 1.5),
                                5 to
                                    TalentLevelEffect(
                                        damageMultiplier = 2.5,
                                        knockback = 1,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "power_strike_armor_break",
                                                    statusId = StatusEffectType.ARMOR_BREAK.schemaId,
                                                    duration = 3,
                                                    saveDimension = SaveDimension.PHYSICAL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "charge",
                        name = "冲锋",
                        description = "",
                        resourceCosts = staminaCosts(12),
                        cooldown = 6,
                        range = 5,
                        minRange = 3,
                        levelEffects =
                            mapOf(
                                1 to TalentLevelEffect(damageMultiplier = 1.2),
                                5 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.8,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "charge_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 2,
                                                    saveDimension = SaveDimension.PHYSICAL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "shield_bash",
                        name = "盾击",
                        description = "",
                        resourceCosts = staminaCosts(10),
                        cooldown = 5,
                        range = 1,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.2,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "shield_bash_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 2,
                                                    saveDimension = SaveDimension.PHYSICAL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "war_cry",
                        name = "战吼",
                        description = "",
                        resourceCosts = staminaCosts(15),
                        cooldown = 10,
                        range = 0,
                        areaRadius = 3,
                        levelEffects =
                            mapOf(
                                5 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                selfEffect("war_cry_buff", "war_cry_empower", duration = 8, magnitude = 0.35),
                                                hostileSaveOnlyEffect(
                                                    effectId = "war_cry_debuff",
                                                    statusId = "war_cry_shaken",
                                                    duration = 5,
                                                    saveDimension = SaveDimension.MENTAL,
                                                    magnitude = 0.20,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "sweeping_strike",
                        name = "横扫",
                        description = "",
                        resourceCosts = staminaCosts(10),
                        cooldown = 4,
                        range = 1,
                        areaRadius = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.2)),
                    ),
                    TalentDef(
                        id = "sunder_armor",
                        name = "碎甲",
                        description = "",
                        resourceCosts = staminaCosts(12),
                        cooldown = 5,
                        range = 1,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.1,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "sunder_armor_break",
                                                    statusId = StatusEffectType.ARMOR_BREAK.schemaId,
                                                    duration = 2,
                                                    saveDimension = SaveDimension.PHYSICAL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "guard_stance",
                        name = "格挡姿态",
                        description = "",
                        resourceCosts = staminaCosts(8),
                        cooldown = 7,
                        range = 0,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                selfEffect("guard_stance_buff", StatusEffectType.GUARD_STANCE_BUFF.schemaId, duration = 3, magnitude = 0.10),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "intimidation",
                        name = "威压",
                        description = "",
                        resourceCosts = staminaCosts(10),
                        cooldown = 8,
                        range = 0,
                        areaRadius = 4,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                hostileSaveOnlyEffect(
                                                    effectId = "intimidation_debuff",
                                                    statusId = "war_cry_shaken",
                                                    duration = 3,
                                                    saveDimension = SaveDimension.MENTAL,
                                                    magnitude = 0.10,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "unyielding",
                        name = "不屈",
                        description = "",
                        resourceCosts = staminaCosts(14),
                        cooldown = 12,
                        range = 0,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                selfEffect("unyielding_buff", StatusEffectType.UNYIELDING_BUFF.schemaId, duration = 3, magnitude = 0.15),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "fireball",
                        name = "火球",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 10),
                        cooldown = 3,
                        range = 6,
                        minRange = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.3)),
                    ),
                    TalentDef(
                        id = "flame_wall",
                        name = "烈焰之墙",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 14),
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
                        resourceCosts = mapOf(ResourceType.MANA to 9),
                        cooldown = 3,
                        range = 6,
                        minRange = 1,
                        levelEffects =
                            mapOf(
                                5 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.8,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "ice_bolt_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 1,
                                                    saveDimension = SaveDimension.SPELL,
                                                ),
                                            ),
                                    ),
                                1 to TalentLevelEffect(damageMultiplier = 1.2),
                            ),
                    ),
                    TalentDef(
                        id = "frost_nova",
                        name = "霜冻新星",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 16),
                        cooldown = 7,
                        range = 0,
                        areaRadius = 2,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.0,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "frost_nova_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 1,
                                                    saveDimension = SaveDimension.SPELL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "ice_prison",
                        name = "冰封",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 18),
                        cooldown = 8,
                        range = 5,
                        minRange = 1,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 0.8,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "ice_prison_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 1,
                                                    saveDimension = SaveDimension.SPELL,
                                                ),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "blink",
                        name = "闪现",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 14),
                        cooldown = 6,
                        range = 5,
                        minRange = 2,
                        levelEffects = mapOf(1 to TalentLevelEffect()),
                    ),
                    TalentDef(
                        id = "arcane_shield",
                        name = "奥术护盾",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 12),
                        cooldown = 8,
                        range = 0,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                selfEffect("arcane_shield_buff", StatusEffectType.ARCANE_SHIELD_BUFF.schemaId, duration = 3, magnitude = 0.10),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "mana_surge",
                        name = "法力涌动",
                        description = "",
                        resourceCosts = mapOf(ResourceType.MANA to 0),
                        cooldown = 10,
                        range = 0,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        resourceRestoreFraction = 0.10,
                                        associatedEffects =
                                            listOf(
                                                selfEffect("mana_surge_buff", StatusEffectType.MANA_SURGE_BUFF.schemaId, duration = 2, magnitude = 0.10),
                                            ),
                                    ),
                            ),
                    ),
                    TalentDef(
                        id = "backstab",
                        name = "背刺",
                        description = "",
                        resourceCosts = mapOf(ResourceType.ENERGY to 12),
                        cooldown = 3,
                        range = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.35)),
                    ),
                    TalentDef(
                        id = "poison_blade",
                        name = "毒刃",
                        description = "",
                        resourceCosts = mapOf(ResourceType.ENERGY to 14),
                        cooldown = 4,
                        range = 1,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.05,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "poison_blade_curse",
                                                    statusId = StatusEffectType.CURSE.schemaId,
                                                    duration = 2,
                                                    saveDimension = SaveDimension.MENTAL,
                                                    magnitude = 0.10,
                                                ),
                                            ),
                                    ),
                            ),
                        damageType = com.ktome.core.combat.DamageType.SHADOW,
                    ),
                    TalentDef(
                        id = "roll",
                        name = "翻滚",
                        description = "",
                        resourceCosts = mapOf(ResourceType.ENERGY to 8),
                        cooldown = 4,
                        range = 3,
                        minRange = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect()),
                    ),
                    TalentDef(
                        id = "shadowstep",
                        name = "影袭",
                        description = "",
                        resourceCosts = mapOf(ResourceType.ENERGY to 20),
                        cooldown = 7,
                        range = 5,
                        minRange = 2,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.20)),
                        damageType = com.ktome.core.combat.DamageType.SHADOW,
                    ),
                    TalentDef(
                        id = "deathblow",
                        name = "处决",
                        description = "",
                        resourceCosts = mapOf(ResourceType.ENERGY to 24),
                        cooldown = 8,
                        range = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.60, resourceRestoreFraction = 0.10)),
                    ),
                    TalentDef(
                        id = "holy_strike",
                        name = "圣击",
                        description = "",
                        resourceCosts = mapOf(ResourceType.POSITIVE_ENERGY to 10),
                        cooldown = 3,
                        range = 1,
                        levelEffects = mapOf(1 to TalentLevelEffect(damageMultiplier = 1.30)),
                        damageType = com.ktome.core.combat.DamageType.HOLY,
                    ),
                    TalentDef(
                        id = "judgment_hammer",
                        name = "裁决之锤",
                        description = "",
                        resourceCosts = mapOf(ResourceType.POSITIVE_ENERGY to 14),
                        cooldown = 5,
                        range = 4,
                        minRange = 1,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        damageMultiplier = 1.10,
                                        associatedEffects =
                                            listOf(
                                                hostileOnHitEffect(
                                                    effectId = "judgment_hammer_stun",
                                                    statusId = StatusEffectType.STUN.schemaId,
                                                    duration = 1,
                                                    saveDimension = SaveDimension.SPELL,
                                                ),
                                            ),
                                    ),
                            ),
                        damageType = com.ktome.core.combat.DamageType.HOLY,
                    ),
                    TalentDef(
                        id = "holy_light",
                        name = "圣光术",
                        description = "",
                        resourceCosts = mapOf(ResourceType.POSITIVE_ENERGY to 12),
                        cooldown = 6,
                        range = 0,
                        levelEffects = mapOf(1 to TalentLevelEffect(healFraction = 0.18)),
                    ),
                    TalentDef(
                        id = "holy_shield",
                        name = "神圣护盾",
                        description = "",
                        resourceCosts = mapOf(ResourceType.POSITIVE_ENERGY to 12),
                        cooldown = 8,
                        range = 0,
                        levelEffects =
                            mapOf(
                                1 to
                                    TalentLevelEffect(
                                        associatedEffects =
                                            listOf(
                                                selfEffect("holy_shield_buff", StatusEffectType.HOLY_SHIELD_BUFF.schemaId, duration = 3, magnitude = 0.12),
                                            ),
                                    ),
                            ),
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

        assertEquals("power_strike is still cooling down.", reason)
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
        assertTrue(requireNotNull(world.get<Health>(monster)).current < 40)
        assertEquals(32, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current)
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

        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.STUN))
    }

    @Test
    fun `shield bash stun now respects hostile hit then save`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(2, 2))

        val result =
            resolver(TestRandomSource(doubles = listOf(0.0, 0.99, 0.99), ints = listOf(0)))
                .resolve(world, map, player, "shield_bash", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(monster)).current < 40)
        assertFalse(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.STUN))
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
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(player)).activeEffects().any { effect -> effect.schemaId == "war_cry_empower" })
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(nearby)).activeEffects().any { effect -> effect.schemaId == "war_cry_shaken" })
        assertFalse(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(farAway)).activeEffects().any { effect -> effect.schemaId == "war_cry_shaken" })
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
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(nearby)).activeEffects().any { effect -> effect.schemaId == "war_cry_shaken" })
    }

    @Test
    fun `intimidation save only debuff can be resisted`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val nearby = createMonster(world, Point(3, 2))

        val result =
            resolver(TestRandomSource(doubles = listOf(0.99), ints = listOf(0)))
                .resolve(world, map, player, "intimidation", null)

        assertTrue(result is TalentUseResult.Success)
        assertFalse(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(nearby)).activeEffects().any { effect -> effect.schemaId == "war_cry_shaken" })
    }

    @Test
    fun `fireball damages ranged target`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(4, 2))

        val result = resolver().resolve(world, map, player, "fireball", Point(4, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(monster)).current < 40)
        assertEquals(90, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.MANA)?.current)
    }

    @Test
    fun `cannot use mana talent without enough mana`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, mana = 5)
        createMonster(world, Point(4, 2))

        val reason = resolver().canUse(world, map, player, "fireball", Point(4, 2))

        assertEquals("Not enough resource.", reason)
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
    fun `poison blade applies cursed and spends energy`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(2, 2))

        val result = resolver().resolve(world, map, player, "poison_blade", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.CURSE))
        assertEquals(46, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.ENERGY)?.current)
    }

    @Test
    fun `roll repositions user with energy cost`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)

        val result = resolver().resolve(world, map, player, "roll", Point(4, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(Point(4, 2), requireNotNull(world.get<Position>(player)).toPoint())
        assertEquals(52, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.ENERGY)?.current)
    }

    @Test
    fun `shadowstep lands adjacent to target and deals shadow damage`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0)
        val monster = createMonster(world, Point(5, 2))

        val result = resolver().resolve(world, map, player, "shadowstep", Point(5, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(1, requireNotNull(world.get<Position>(player)).toPoint().chebyshevDistanceTo(Point(5, 2)))
        assertTrue(requireNotNull(world.get<Health>(monster)).current < 40)
    }

    @Test
    fun `deathblow restores energy after a successful hit`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, energy = 30)
        createMonster(world, Point(2, 2))

        val result = resolver().resolve(world, map, player, "deathblow", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(16, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.ENERGY)?.current)
    }

    @Test
    fun `holy light heals the player using positive energy`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, positiveEnergy = 40)
        requireNotNull(world.get<Health>(player)).current = 20

        val result = resolver().resolve(world, map, player, "holy_light", null)

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(player)).current > 20)
        assertEquals(28, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.POSITIVE_ENERGY)?.current)
    }

    @Test
    fun `holy shield applies templar defensive buff`() {
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, positiveEnergy = 40)

        val result = resolver().resolve(world, map, player, "holy_shield", null)

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(player)).has(StatusEffectType.HOLY_SHIELD_BUFF))
    }

    @Test
    fun `linebreaker damages knocks back and applies armor break`() {
        registry.register(
            TalentDef(
                id = "linebreaker",
                name = "破阵",
                description = "",
                resourceCosts = staminaCosts(12),
                cooldown = 6,
                range = 1,
                areaRadius = 0,
                levelEffects =
                    mapOf(
                        1 to
                            TalentLevelEffect(
                                damageMultiplier = 1.4,
                                knockback = 1,
                                associatedEffects =
                                    listOf(
                                        hostileOnHitEffect(
                                            effectId = "linebreaker_armor_break",
                                            statusId = StatusEffectType.ARMOR_BREAK.schemaId,
                                            duration = 2,
                                            saveDimension = SaveDimension.PHYSICAL,
                                        ),
                                    ),
                            ),
                    ),
            ),
        )
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, levelOverrides = mapOf("linebreaker" to 1))
        val monster = createMonster(world, Point(2, 2))

        val result = resolver().resolve(world, map, player, "linebreaker", Point(2, 2))

        assertTrue(result is TalentUseResult.Success)
        assertEquals(Point(3, 2), requireNotNull(world.get<Position>(monster)).toPoint())
        assertTrue(requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(monster)).has(StatusEffectType.ARMOR_BREAK))
        assertEquals(28, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current)
        assertEquals(6, requireNotNull(world.get<CooldownState>(player)).remainingByTalentId["linebreaker"])
    }

    @Test
    fun `inferno orb hits clustered enemies and spends mana`() {
        registry.register(
            TalentDef(
                id = "inferno_orb",
                name = "炼狱法球",
                description = "",
                resourceCosts = mapOf(ResourceType.MANA to 12),
                cooldown = 5,
                range = 5,
                minRange = 1,
                areaRadius = 1,
                levelEffects =
                    mapOf(
                        1 to
                            TalentLevelEffect(
                                damageMultiplier = 1.2,
                                knockback = 1,
                            ),
                    ),
            ),
        )
        val world = baseWorld()
        val player = createPlayer(world, cooldown = 0, levelOverrides = mapOf("inferno_orb" to 1))
        val first = createMonster(world, Point(4, 2))
        val second = createMonster(world, Point(4, 3))

        val result = resolver().resolve(world, map, player, "inferno_orb", Point(4, 2))

        assertTrue(result is TalentUseResult.Success)
        assertTrue(requireNotNull(world.get<Health>(first)).current < 40)
        assertTrue(requireNotNull(world.get<Health>(second)).current < 40)
        assertEquals(88, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.MANA)?.current)
        assertEquals(5, requireNotNull(world.get<CooldownState>(player)).remainingByTalentId["inferno_orb"])
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
                resourceCosts = staminaCosts(9),
                cooldown = 4,
                range = 0,
                levelEffects = mapOf(1 to TalentLevelEffect()),
            ),
        )
        requireNotNull(world.get<TalentLoadout>(player)).talentLevels["unsupported"] = 1

        val beforeStamina = requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current
        val beforeCooldowns = requireNotNull(world.get<CooldownState>(player)).remainingByTalentId.toMap()

        val result = resolver().resolve(world, map, player, "unsupported", null)

        assertTrue(result is TalentUseResult.Failure)
        assertEquals(beforeStamina, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current)
        assertEquals(beforeCooldowns, requireNotNull(world.get<CooldownState>(player)).remainingByTalentId)
    }

    private fun baseWorld(): World = World()

    private fun resolver(
        randomSource: TestRandomSource = TestRandomSource(doubles = listOf(0.0, 0.0, 0.0), ints = listOf(0)),
    ): TalentResolver =
        TalentResolver(
            registry = registry,
            combatResolver = CombatResolver(randomSource),
            statusCatalog =
                StatusCatalog(
                    listOf(
                        StatusEffectDef(
                            id = "war_cry_empower",
                            type = StatusEffectType.CUSTOM,
                            category = EffectCategory.BUFF,
                            nameKey = "status.war_cry_buff",
                            stackingRule = StackingRule.UNIQUE,
                            replacePolicy = ReplacePolicy.KEEP_STRONGEST,
                            uniquenessKey = "war_cry_empower",
                            sourceScopedUnique = true,
                            statModifier = StatModifier(attackMultiplierBonus = 1.0),
                        ),
                        StatusEffectDef(
                            id = "war_cry_shaken",
                            type = StatusEffectType.CUSTOM,
                            category = EffectCategory.DEBUFF,
                            nameKey = "status.war_cry_debuff",
                            stackingRule = StackingRule.UNIQUE,
                            replacePolicy = ReplacePolicy.KEEP_STRONGEST,
                            uniquenessKey = "war_cry_shaken",
                            sourceScopedUnique = true,
                            statModifier = StatModifier(defenseMultiplierBonus = -1.0),
                        ),
                    ),
                ),
        )

    private fun createPlayer(
        world: World,
        stamina: Int = 40,
        mana: Int = 100,
        energy: Int = 60,
        positiveEnergy: Int = 30,
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
                        18 to "backstab",
                        19 to "poison_blade",
                        20 to "roll",
                        21 to "shadowstep",
                        22 to "deathblow",
                        23 to "holy_strike",
                        24 to "judgment_hammer",
                        25 to "holy_light",
                        26 to "holy_shield",
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
                        "backstab" to 1,
                        "poison_blade" to 1,
                        "roll" to 1,
                        "shadowstep" to 1,
                        "deathblow" to 1,
                        "holy_strike" to 1,
                        "judgment_hammer" to 1,
                        "holy_light" to 1,
                        "holy_shield" to 1,
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
        world.add(
            player,
            ResourcePools(
                linkedMapOf(
                    ResourceType.STAMINA to ResourcePool(type = ResourceType.STAMINA, current = stamina, max = derived.maxStamina),
                    ResourceType.MANA to ResourcePool(type = ResourceType.MANA, current = mana, max = 100),
                    ResourceType.ENERGY to ResourcePool(type = ResourceType.ENERGY, current = energy, max = 100),
                    ResourceType.POSITIVE_ENERGY to ResourcePool(type = ResourceType.POSITIVE_ENERGY, current = positiveEnergy, max = 100),
                ),
            ),
        )
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
