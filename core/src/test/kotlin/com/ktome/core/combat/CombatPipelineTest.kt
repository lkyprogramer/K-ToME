package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatPipelineTest {
    @Test
    fun `pipeline keeps the 12 frozen steps and callback priority order`() {
        val order = mutableListOf<String>()
        val pipeline = CombatPipeline(TestRandomSource(doubles = listOf(0.0, 0.99, 0.0), ints = listOf(0)))
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    attackerId = EntityId(1),
                    targetId = EntityId(9),
                    abilityId = "test_strike",
                    traceId = "trace-formula",
                    damageType = DamageType.PHYSICAL,
                    baseDamage = 12,
                    attackerAccuracy = 20,
                    targetEvasion = 0,
                    attackerCritChance = 0.05,
                    targetArmor = 100,
                    targetCurrentHp = 20,
                    statusApplication =
                        StatusApplicationRequest(
                            statusId = "stun",
                            duration = 2,
                            applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                            saveDimension = SaveDimension.PHYSICAL,
                            power = 25,
                            save = 10,
                        ),
                    elementalInteraction = ElementalInteraction("holy_burst", "BANE_TRIGGERED", childTraceId = "child-trace-1"),
                    callbacks =
                        listOf(
                            PipelineCallback(EntityId(2), "late", CombatCallbackPhase.PRE_DAMAGE_APPLY, 100) {
                                order += "2"
                                CallbackDecision()
                            },
                            PipelineCallback(EntityId(1), "first", CombatCallbackPhase.PRE_DAMAGE_APPLY, 100) {
                                order += "1"
                                CallbackDecision()
                            },
                            PipelineCallback(EntityId(3), "stop", CombatCallbackPhase.PRE_DAMAGE_APPLY, 200) {
                                order += "3"
                                CallbackDecision(flow = CallbackFlow.CANCEL, effect = "STOP_REMAINING_PRE_DAMAGE")
                            },
                            PipelineCallback(EntityId(4), "skipped", CombatCallbackPhase.PRE_DAMAGE_APPLY, 300) {
                                order += "4"
                                CallbackDecision()
                            },
                            PipelineCallback(EntityId(5), "applied", CombatCallbackPhase.ON_DAMAGE_APPLIED, 50) {
                                order += "applied"
                                CallbackDecision()
                            },
                        ),
                ),
            )

        assertEquals(listOf("1", "2", "3", "applied"), order)
        assertEquals(
            listOf(
                "HIT_CHECK",
                "CRIT_CHECK",
                "RAW_DAMAGE_CALCULATION",
                "PRE_REDUCTION_CALLBACKS",
                "ARMOR_RESISTANCE_REDUCTION",
                "PENETRATION_APPLICATION",
                "POST_REDUCTION_CALLBACKS",
                "FINAL_DAMAGE_APPLICATION",
                "ON_DAMAGE_TAKEN_CALLBACKS",
                "DEATH_CHECK",
                "ON_KILL_CALLBACKS",
                "EXPERIENCE_AND_LOOT",
            ),
            outcome.trace.steps.map(ResolutionStep::stepName),
        )
        assertEquals(6, outcome.finalDamage)
        assertTrue(requireNotNull(outcome.statusApplication).applied)
        assertEquals(listOf("child-trace-1"), outcome.trace.childTraceIds)
        assertEquals(listOf("applied"), outcome.trace.steps[7].callbacks.map(CallbackRecord::callbackName))
    }

    @Test
    fun `absorb terminates the pipeline before reduction`() {
        val pipeline = CombatPipeline(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    abilityId = "absorbed",
                    traceId = "trace-absorb",
                    baseDamage = 10,
                    attackerAccuracy = 20,
                    targetEvasion = 0,
                    callbacks =
                        listOf(
                            PipelineCallback(EntityId(7), "absorb", CombatCallbackPhase.PRE_DAMAGE_APPLY, 100) {
                                CallbackDecision(flow = CallbackFlow.ABSORB, effect = "WARD")
                            },
                        ),
                ),
            )

        assertTrue(outcome.hit)
        assertEquals(0, outcome.finalDamage)
        assertEquals(4, outcome.trace.steps.size)
    }

    @Test
    fun `save only status still resolves when hit check misses`() {
        val pipeline = CombatPipeline(TestRandomSource(doubles = listOf(0.99, 0.0)))
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    abilityId = "intimidation",
                    traceId = "trace-save-only-miss",
                    attackerAccuracy = 0,
                    targetEvasion = 500,
                    statusApplication =
                        StatusApplicationRequest(
                            statusId = "shaken",
                            duration = 3,
                            applicationPolicy = ApplicationPolicy.HOSTILE_SAVE_ONLY,
                            saveDimension = SaveDimension.MENTAL,
                            power = 30,
                            save = 10,
                        ),
                ),
            )

        assertFalse(outcome.hit)
        assertTrue(requireNotNull(outcome.statusApplication).attempted)
        assertTrue(requireNotNull(outcome.statusApplication).applied)
        assertEquals("POWER_SAVE_SUCCESS", requireNotNull(outcome.statusApplication).reasonTag)
    }

    @Test
    fun `save only status still resolves on absorb terminal paths`() {
        val pipeline = CombatPipeline(TestRandomSource(doubles = listOf(0.0, 0.99, 0.0), ints = listOf(0)))
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    abilityId = "smoke_bomb",
                    traceId = "trace-save-only-absorb",
                    baseDamage = 10,
                    attackerAccuracy = 20,
                    targetEvasion = 0,
                    statusApplication =
                        StatusApplicationRequest(
                            statusId = "smoke_bomb_curse",
                            duration = 2,
                            applicationPolicy = ApplicationPolicy.HOSTILE_SAVE_ONLY,
                            saveDimension = SaveDimension.PHYSICAL,
                            power = 30,
                            save = 10,
                        ),
                    callbacks =
                        listOf(
                            PipelineCallback(EntityId(7), "absorb", CombatCallbackPhase.PRE_DAMAGE_APPLY, 100) {
                                CallbackDecision(flow = CallbackFlow.ABSORB, effect = "WARD")
                            },
                        ),
                ),
            )

        assertTrue(outcome.hit)
        assertEquals(0, outcome.finalDamage)
        assertTrue(requireNotNull(outcome.statusApplication).applied)
        assertEquals(4, outcome.trace.steps.size)
    }

    @Test
    fun `miss phase invokes on miss callbacks and records dedicated trace step`() {
        val order = mutableListOf<String>()
        val pipeline = CombatPipeline(TestRandomSource(doubles = listOf(0.99)))
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    abilityId = "missed_strike",
                    traceId = "trace-on-miss",
                    attackerAccuracy = 0,
                    targetEvasion = 500,
                    callbacks =
                        listOf(
                            PipelineCallback(EntityId(9), "miss-listener", CombatCallbackPhase.ON_MISS, 10) {
                                order += "miss"
                                CallbackDecision()
                            },
                        ),
                ),
            )

        assertFalse(outcome.hit)
        assertEquals(listOf("miss"), order)
        assertEquals("MISS_CALLBACKS", outcome.trace.steps.last().stepName)
        assertEquals(listOf("miss-listener"), outcome.trace.steps.last().callbacks.map(CallbackRecord::callbackName))
    }
}
