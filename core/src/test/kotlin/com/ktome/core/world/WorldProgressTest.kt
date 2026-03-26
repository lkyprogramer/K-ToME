package com.ktome.core.world

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldProgressTest {
    @Test
    fun `gate requiring completed quest only passes after quest completion`() {
        val worldProgress =
            WorldProgressDef(
                questStates =
                    mapOf(
                        "quest.underground_river" to
                            QuestProgress(
                                questId = "quest.underground_river",
                                objectiveStates = mapOf("crossing" to ObjectiveState.AVAILABLE),
                                completionFlags = setOf("quest.underground_river.cleared"),
                            ),
                    ),
            )

        assertFalse(worldProgress.satisfies(GateCondition(requiredQuestId = "quest.underground_river")))

        val completed =
            worldProgress.withQuestProgress(
                questId = "quest.underground_river",
                progress =
                    QuestProgress(
                        questId = "quest.underground_river",
                        objectiveStates = mapOf("crossing" to ObjectiveState.COMPLETED),
                        completionFlags = setOf("quest.underground_river.cleared"),
                    ),
            )

        assertTrue(completed.satisfies(GateCondition(requiredQuestId = "quest.underground_river")))
    }

    @Test
    fun `in progress quest does not satisfy completion gate`() {
        val worldProgress =
            WorldProgressDef(
                questStates =
                    mapOf(
                        "quest.underground_river" to
                            QuestProgress(
                                questId = "quest.underground_river",
                                objectiveStates = mapOf("crossing" to ObjectiveState.IN_PROGRESS),
                                completionFlags = setOf("quest.underground_river.cleared"),
                            ),
                    ),
            )

        assertFalse(worldProgress.satisfies(GateCondition(requiredQuestId = "quest.underground_river")))
    }

    @Test
    fun `gate also checks world flag and boss kill`() {
        val worldProgress =
            WorldProgressDef()
                .withWorldFlag("quest.abyssal_temple.cleared")
                .withDefeatedBoss("orc.molten_giant")

        assertTrue(
            worldProgress.satisfies(
                GateCondition(
                    requiredWorldFlag = "quest.abyssal_temple.cleared",
                    requiredBossKill = "orc.molten_giant",
                ),
            ),
        )
    }
}
