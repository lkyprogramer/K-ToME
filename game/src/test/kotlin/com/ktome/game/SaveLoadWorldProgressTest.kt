package com.ktome.game

import com.ktome.core.save.SaveManager
import com.ktome.core.world.ObjectiveState
import com.ktome.core.world.QuestProgress
import com.ktome.core.world.WorldProgressDef
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SaveLoadWorldProgressTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `save load round trip preserves world progress route rewards and shop state`() {
        val saveManager = SaveManager(tempDir.resolve("world-progress-save"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = saveManager,
            )
        val expectedWorldProgress =
            WorldProgressDef(
                questStates =
                    mapOf(
                        "quest.underground_river" to
                            QuestProgress(
                                questId = "quest.underground_river",
                                objectiveStates =
                                    mapOf(
                                        "crossing" to ObjectiveState.COMPLETED,
                                    ),
                                completionFlags = setOf("quest.underground_river.cleared"),
                            ),
                        "quest.abyssal_temple" to
                            QuestProgress(
                                questId = "quest.abyssal_temple",
                                objectiveStates =
                                    mapOf(
                                        "sanctum" to ObjectiveState.AVAILABLE,
                                    ),
                                completionFlags = setOf("quest.abyssal_temple.started"),
                            ),
                    ),
                worldFlags = setOf("quest.shattered_outpost.cleared", "quest.underground_river.cleared"),
                unlockedRoutes =
                    setOf(
                        "route.shattered_outpost.greenwood_fringe",
                        "route.greenwood_fringe.deep_iron_pit",
                        "route.deep_iron_pit.grey_gate_depths",
                    ),
                defeatedBossIds = setOf("orc.molten_giant", "cultist.dungeon_lord"),
                claimedRouteRewards =
                    setOf(
                        "route.shattered_outpost.greenwood_fringe",
                        "route.deep_iron_pit.grey_gate_depths",
                    ),
            )
        val expectedShopStates: List<com.ktome.core.economy.ShopInventoryState> =
            listOf(
                com.ktome.core.economy.ShopInventoryState(
                    shopId = "greenwood_supply_post",
                    purchasedOfferIds = setOf("offer.healing_potion", "offer.iron_shield"),
                ),
            )
        assertTrue(session.perform(PlayerCommand.SaveGame))
        val baseline = requireNotNull(saveManager.load())

        saveManager.save(
            baseline.copy(
                worldProgress = expectedWorldProgress,
                shardBalance = 123,
                shopStates = expectedShopStates,
                headlessTurnEquivalent = 34,
            ),
        )

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))

        assertEquals(expectedWorldProgress, loaded.worldProgress())
        assertEquals(expectedShopStates, loaded.shopStates())
        assertEquals(123, loaded.currentShardBalance())
        assertEquals(34, loaded.currentHeadlessTurnEquivalent())
        assertEquals(listOf("shattered_outpost", "greenwood_fringe"), loaded.config.zoneRoute)
        assertEquals(1, loaded.config.routeIndex)
    }
}
