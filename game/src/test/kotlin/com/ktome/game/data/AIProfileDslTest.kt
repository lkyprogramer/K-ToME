package com.ktome.game.data

import com.ktome.core.ai.AIActionType
import com.ktome.core.ai.AICondition
import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.AISelectionPolicy
import com.ktome.game.i18n.GameLocale
import java.lang.reflect.InvocationTargetException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIProfileDslTest {
    @Test
    fun `loader parses nested ai conditions and weighted selection metadata`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseAiProfiles", Map::class.java).apply {
                isAccessible = true
            }

        @Suppress("UNCHECKED_CAST")
        val profile =
            (parseMethod.invoke(
                loader,
                linkedMapOf(
                    "aiProfiles" to
                        listOf(
                            linkedMapOf(
                                "id" to "ai.test.weighted",
                                "schemaVersion" to 2,
                                "perceptionRange" to 9,
                                "useLastKnownPosition" to true,
                                "defaultBehavior" to "CHASE",
                                "selectionPolicy" to "WEIGHTED_RANDOM",
                                "actions" to
                                    listOf(
                                        linkedMapOf(
                                            "id" to "pressure",
                                            "type" to "USE_ABILITY",
                                            "orderKey" to 10,
                                            "weight" to 2.5,
                                            "abilityId" to "power_strike",
                                            "condition" to
                                                linkedMapOf(
                                                    "type" to "OR",
                                                    "conditions" to
                                                        listOf(
                                                            linkedMapOf(
                                                                "type" to "TARGET_VISIBLE",
                                                            ),
                                                            linkedMapOf(
                                                                "type" to "NOT",
                                                                "condition" to
                                                                    linkedMapOf(
                                                                        "type" to "HAS_STATUS",
                                                                        "scope" to "SELF",
                                                                        "statusId" to "war_cry_empower",
                                                                    ),
                                                            ),
                                                        ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ),
            ) as List<AIProfile>).single()

        assertEquals(AISelectionPolicy.WEIGHTED_RANDOM, profile.selectionPolicy)
        val action = profile.actions.single()
        assertEquals(2.5, action.weight)
        assertEquals("power_strike", action.abilityId)
        val condition = requireNotNull(action.condition)
        assertTrue(condition is AICondition.Or)
        val composite = condition as AICondition.Or
        assertTrue(composite.conditions[0] is AICondition.TargetVisible)
        assertTrue(composite.conditions[1] is AICondition.Not)
    }

    @Test
    fun `loader rejects ai profiles with stale schema version`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseAiProfiles", Map::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "aiProfiles" to
                            listOf(
                                linkedMapOf(
                                    "id" to "ai.test.stale",
                                    "schemaVersion" to 1,
                                    "perceptionRange" to 8,
                                    "defaultBehavior" to "CHASE",
                                    "selectionPolicy" to "DETERMINISTIC_PRIORITY",
                                    "actions" to listOf(linkedMapOf("id" to "wait", "type" to "WAIT")),
                                ),
                            ),
                    ),
                )
            }

        assertTrue(requireNotNull(error.cause?.message).contains("schemaVersion 2"))
    }

    @Test
    fun `official boss profile keeps nested not status guards`() {
        val profile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.boss.dungeon_lord.phase_full" }
        val guardAction = profile.actions.first { action -> action.id == "war_cry" }
        val condition = requireNotNull(guardAction.condition) as AICondition.And
        val targetVisibleGuard = condition.conditions.first()
        val notGuard = condition.conditions.last()

        assertTrue(targetVisibleGuard is AICondition.TargetVisible)
        assertTrue(notGuard is AICondition.Not)
        assertTrue((notGuard as AICondition.Not).condition is AICondition.HasStatus)
        assertTrue(profile.actions.any { action -> action.id == "strike" && action.type == AIActionType.ATTACK_TARGET })
    }

    @Test
    fun `dungeon lord enraged profile keeps melee fallback`() {
        val profile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.boss.dungeon_lord.phase_enraged" }

        assertTrue(profile.actions.any { action -> action.id == "close_quarters" && action.type == AIActionType.ATTACK_TARGET })
    }

    @Test
    fun `abyssal guardian profiles stay distinct from dungeon lord ids and action mix`() {
        val fullProfile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.boss.abyssal_guardian.phase_full" }
        val abyssalProfile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.boss.abyssal_guardian.phase_abyssal" }

        assertTrue(fullProfile.actions.any { action -> action.id == "abyssal_roar" && action.abilityId == "war_cry" })
        assertTrue(fullProfile.actions.any { action -> action.id == "abyssal_charge" && action.abilityId == "charge" })
        assertTrue(abyssalProfile.actions.any { action -> action.id == "collapse_charge" && action.abilityId == "charge" })
        assertTrue(abyssalProfile.actions.any { action -> action.id == "void_breach" && action.abilityId == "power_strike" })
    }

    @Test
    fun `forge guard battle cry only triggers on visible target`() {
        val profile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.elite.forge_guard" }
        val battleCry = profile.actions.first { action -> action.id == "battle_cry" }
        val condition = requireNotNull(battleCry.condition) as AICondition.And

        assertTrue(condition.conditions.first() is AICondition.TargetVisible)
    }
}
