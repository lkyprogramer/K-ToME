package com.ktome.game.data

import com.ktome.core.ai.AICondition
import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.AISelectionPolicy
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `official boss profile keeps nested not status guards`() {
        val profile = DataLoader(GameLocale.EN_US).loadSchemaCatalog().aiProfiles.first { it.id == "ai.boss.dungeon_lord.phase_full" }
        val guardAction = profile.actions.first { action -> action.id == "war_cry" }
        val condition = requireNotNull(guardAction.condition) as AICondition.And
        val notGuard = condition.conditions.last()

        assertTrue(notGuard is AICondition.Not)
        assertTrue((notGuard as AICondition.Not).condition is AICondition.HasStatus)
    }
}
