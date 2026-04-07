package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.mapgen.TerrainTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ElementInteractionRegistryTest {
    @Test
    fun `registry exposes the five frozen phase four terrain rules`() {
        assertEquals(1, ElementInteractionRegistry.VERSION)
        assertEquals(2, ElementInteractionRegistry.MAX_INTERACTION_DEPTH)
        assertEquals(
            listOf(
                ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN,
                ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE,
                ElementInteractionRegistry.TERRAIN_COLD_WATER_FREEZE,
                ElementInteractionRegistry.TERRAIN_FIRE_ICE_MELT,
                ElementInteractionRegistry.TERRAIN_PHYSICAL_ICE_SLIP,
            ),
            ElementInteractionRegistry.all().map(ElementInteractionRule::id),
        )
    }

    @Test
    fun `lightning water chain adds bonus trace and only chains into adjacent water targets`() {
        val targetId = EntityId(9)
        val adjacentWater = EntityId(10)
        val adjacentOil = EntityId(11)
        val resolution =
            requireNotNull(
                ElementInteractionRegistry.resolve(
                    request = request(traceId = "trace-lightning", targetId = targetId, damageType = DamageType.LIGHTNING),
                    finalDamage = 12,
                    terrainContext =
                        TerrainInteractionContext(
                            targetTerrainTags = setOf(TerrainTag.WATER),
                            adjacentTargets =
                                listOf(
                                    TerrainInteractionTarget(adjacentWater, setOf(TerrainTag.WATER)),
                                    TerrainInteractionTarget(adjacentOil, setOf(TerrainTag.OIL)),
                                ),
                            interactionDepth = 1,
                            sourceTags = setOf("mutation:elite.emberblood"),
                        ),
                ),
            )

        assertEquals(ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN, resolution.ruleId)
        assertEquals(DamageType.LIGHTNING, resolution.triggerDamageType)
        assertEquals(1, resolution.interactionDepth)
        assertEquals("trace-lightning:${ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN}:bonus:${targetId.value}", requireNotNull(resolution.bonusTargetTrace).traceId)
        assertEquals(3, requireNotNull(resolution.bonusTargetTrace).rawDamage)
        assertEquals(listOf(adjacentWater), resolution.chainTargets.map(TerrainInteractionChildTrace::targetId))
        assertEquals(listOf("trace-lightning:${ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN}:bonus:${targetId.value}", "trace-lightning:${ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN}:chain:${adjacentWater.value}"), resolution.childTraceIds)
    }

    @Test
    fun `terrain transforms and slip resolution preserve frozen phase four behavior`() {
        val ignite =
            requireNotNull(
                ElementInteractionRegistry.resolve(
                    request = request(traceId = "trace-oil", damageType = DamageType.FIRE),
                    finalDamage = 9,
                    terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.OIL)),
                ),
            )
        assertEquals(ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE, ignite.ruleId)
        assertEquals(setOf(TerrainTag.OIL), requireNotNull(ignite.terrainTransform).targetTerrainTags)
        assertEquals(3, requireNotNull(ignite.terrainTransform).durationTurns)
        assertEquals(DamageType.FIRE, requireNotNull(ignite.terrainTransform).tickDamageType)
        assertEquals(4, requireNotNull(ignite.terrainTransform).tickDamage)

        val freeze =
            requireNotNull(
                ElementInteractionRegistry.resolve(
                    request = request(traceId = "trace-freeze", damageType = DamageType.COLD),
                    finalDamage = 8,
                    terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.WATER)),
                ),
            )
        assertEquals(ElementInteractionRegistry.TERRAIN_COLD_WATER_FREEZE, freeze.ruleId)
        assertEquals(setOf(TerrainTag.ICE), requireNotNull(freeze.terrainTransform).targetTerrainTags)
        assertEquals(3, requireNotNull(freeze.terrainTransform).durationTurns)
        assertFalse(requireNotNull(freeze.terrainTransform).conductsLightning)

        val melt =
            requireNotNull(
                ElementInteractionRegistry.resolve(
                    request = request(traceId = "trace-melt", damageType = DamageType.FIRE),
                    finalDamage = 7,
                    terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.ICE)),
                ),
            )
        assertEquals(ElementInteractionRegistry.TERRAIN_FIRE_ICE_MELT, melt.ruleId)
        assertEquals(listOf("FREEZE"), melt.removedStatusIds)
        assertEquals(setOf(TerrainTag.WATER), requireNotNull(melt.terrainTransform).targetTerrainTags)
        assertTrue(requireNotNull(melt.terrainTransform).conductsLightning)

        val slip =
            requireNotNull(
                ElementInteractionRegistry.resolve(
                    request = request(traceId = "trace-slip", damageType = DamageType.PHYSICAL),
                    finalDamage = 6,
                    terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.ICE)),
                ),
            )
        assertEquals(ElementInteractionRegistry.TERRAIN_PHYSICAL_ICE_SLIP, slip.ruleId)
        assertEquals(listOf("STUN"), slip.appliedStatusIds)
        assertEquals("STUN", requireNotNull(slip.slip).statusId)
        assertEquals(1, requireNotNull(slip.slip).durationTurns)
    }

    @Test
    fun `registry returns null for zero damage depth exhaustion and unmatched terrain`() {
        assertNull(
            ElementInteractionRegistry.resolve(
                request = request(damageType = DamageType.FIRE),
                finalDamage = 0,
                terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.OIL)),
            ),
        )
        assertNull(
            ElementInteractionRegistry.resolve(
                request = request(damageType = DamageType.COLD),
                finalDamage = 5,
                terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.WATER), interactionDepth = ElementInteractionRegistry.MAX_INTERACTION_DEPTH),
            ),
        )
        assertNull(
            ElementInteractionRegistry.resolve(
                request = request(damageType = DamageType.HOLY),
                finalDamage = 5,
                terrainContext = TerrainInteractionContext(targetTerrainTags = setOf(TerrainTag.WATER)),
            ),
        )
    }

    @Test
    fun `terrain interaction contracts fail fast on invalid inputs`() {
        assertThrows(IllegalArgumentException::class.java) {
            ElementInteractionRule(
                id = "",
                triggerElement = DamageType.FIRE,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.OIL.name,
                effectType = InteractionEffect.TERRAIN_TRANSFORM,
                effectParams = mapOf("durationTurns" to "3"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ElementInteractionRule(
                id = "rule",
                triggerElement = DamageType.FIRE,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.OIL.name,
                effectType = InteractionEffect.TERRAIN_TRANSFORM,
                effectParams = mapOf("" to "3"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainInteractionContext(
                targetTerrainTags = setOf(TerrainTag.WATER),
                interactionDepth = -1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainInteractionContext(
                targetTerrainTags = setOf(TerrainTag.WATER),
                sourceTags = setOf(""),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainTransformPlan(
                targetTerrainTags = setOf(TerrainTag.OIL),
                tickDamage = 1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainInteractionChildTrace(
                traceId = "",
                targetId = EntityId(1),
                rawDamage = 1,
                damageMultiplier = 0.5,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TerrainSlipPlan(statusId = "", durationTurns = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ElementInteractionResolution(
                ruleId = "",
                triggerDamageType = DamageType.PHYSICAL,
            )
        }
    }

    private fun request(
        traceId: String = "trace",
        targetId: EntityId = EntityId(2),
        damageType: DamageType = DamageType.PHYSICAL,
    ): DamageRequest =
        DamageRequest(
            attackerId = EntityId(1),
            targetId = targetId,
            abilityId = "test_ability",
            traceId = traceId,
            damageType = damageType,
            baseDamage = 12,
            targetCurrentHp = 30,
        )
}
