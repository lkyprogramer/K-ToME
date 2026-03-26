package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.save.SaveManager
import com.ktome.core.world.ObjectiveState
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunWorldStructureSessionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `new shattered outpost run does not start with cleared quest flags`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "shattered_outpost",
                        playerProfessionId = "vanguard",
                    ),
                saveManager = SaveManager(tempDir.resolve("fresh-shattered-outpost-save")),
            )

        val breachState = session.worldProgress().questStates["quest.shattered_outpost"]?.objectiveStates?.get("breach")
        assertEquals(ObjectiveState.AVAILABLE, breachState)
        assertTrue("quest.shattered_outpost.cleared" !in session.worldProgress().worldFlags)
    }

    @Test
    fun `greenwood exit opens dynamic route selection and selected branch becomes current zone`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("dynamic-route-save")),
            )

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val routeSelection = requireNotNull(session.renderSnapshot().uiState.activeRouteSelection)
        assertEquals(
            setOf("shattered_outpost", "bandit_camp", "deep_iron_pit", "elven_ruins"),
            routeSelection.options.map { option -> option.destinationZoneId }.toSet(),
        )

        assertTrue(session.perform(PlayerCommand.SelectRoute(0)))
        assertEquals("bandit_camp", session.config.zoneId)
        assertEquals(listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"), session.config.zoneRoute)
        assertTrue("route.greenwood_fringe.bandit_camp" in session.worldProgress().unlockedRoutes)
    }

    @Test
    fun `mandatory routes preserve bidirectional return options`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("mandatory-return-path-save")),
            )

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val routeSelection = requireNotNull(session.renderSnapshot().uiState.activeRouteSelection)
        val returnOption = requireNotNull(routeSelection.options.firstOrNull { option -> option.destinationZoneId == "shattered_outpost" })
        assertTrue(returnOption.isReturnPath)
        assertTrue(session.perform(PlayerCommand.SelectRoute(returnOption.index)))
        assertEquals("shattered_outpost", session.config.zoneId)
        assertEquals(listOf("shattered_outpost", "greenwood_fringe", "shattered_outpost"), session.config.zoneRoute)
    }

    @Test
    fun `route reward claim policies distinguish first clear from route unlock`() {
        val firstClearSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("first-clear-reward-save")),
            )

        assertEquals(0, firstClearSession.currentShardBalance())
        movePlayerTo(firstClearSession, stairPoint(firstClearSession, StairDirection.DOWN))
        assertTrue(firstClearSession.perform(PlayerCommand.Descend))
        movePlayerTo(firstClearSession, stairPoint(firstClearSession, StairDirection.DOWN))
        assertTrue(firstClearSession.perform(PlayerCommand.Descend))
        assertTrue(firstClearSession.perform(PlayerCommand.SelectRoute(1)))
        assertEquals(60, firstClearSession.currentShardBalance())
        assertTrue("route.greenwood_fringe.deep_iron_pit" in firstClearSession.worldProgress().claimedRouteRewards)

        val unlockSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("unlock-reward-save")),
            )

        assertEquals(0, unlockSession.currentShardBalance())
        movePlayerTo(unlockSession, stairPoint(unlockSession, StairDirection.DOWN))
        assertTrue(unlockSession.perform(PlayerCommand.Descend))
        movePlayerTo(unlockSession, stairPoint(unlockSession, StairDirection.DOWN))
        assertTrue(unlockSession.perform(PlayerCommand.Descend))
        assertTrue(unlockSession.perform(PlayerCommand.SelectRoute(0)))
        assertEquals(20, unlockSession.currentShardBalance())
        assertTrue("route.greenwood_fringe.bandit_camp" in unlockSession.worldProgress().claimedRouteRewards)
    }

    @Test
    fun `bandit camp objective only completes after cache interaction`() {
        val lockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("bandit-camp-locked-save")),
            )

        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        assertEquals("greenwood_fringe", lockedSession.config.zoneId)
        assertEquals(
            ObjectiveState.AVAILABLE,
            lockedSession.worldProgress().questStates["quest.bandit_camp"]?.objectiveStates?.get("cache_raid"),
        )
        assertTrue("quest.bandit_camp.cleared" !in lockedSession.worldProgress().worldFlags)

        val unlockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "bandit_camp",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "bandit_camp"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("bandit-camp-unlocked-save")),
            )

        unlockedSession.automationMovePlayerTo(interactablePoint(unlockedSession, "bandit_cache"))
        assertTrue(unlockedSession.perform(PlayerCommand.Interact))
        assertEquals(
            ObjectiveState.IN_PROGRESS,
            unlockedSession.worldProgress().questStates["quest.bandit_camp"]?.objectiveStates?.get("cache_raid"),
        )

        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        assertEquals("greenwood_fringe", unlockedSession.config.zoneId)
        assertEquals(
            ObjectiveState.COMPLETED,
            unlockedSession.worldProgress().questStates["quest.bandit_camp"]?.objectiveStates?.get("cache_raid"),
        )
        assertTrue("quest.bandit_camp.cleared" in unlockedSession.worldProgress().worldFlags)
    }

    @Test
    fun `shop interaction supports buy and sell loop`() {
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
                saveManager = SaveManager(tempDir.resolve("shop-loop-save")),
            )

        assertEquals("greenwood_fringe", session.config.zoneId)

        session.automationMovePlayerTo(interactablePoint(session, "merchant_stall"))
        assertTrue(session.perform(PlayerCommand.Interact))
        assertNotNull(session.renderSnapshot().uiState.activeShop)

        val sellEntry = requireNotNull(session.renderSnapshot().uiState.activeShop).sellEntries.first()
        val beforeSell = session.currentShardBalance()
        assertTrue(session.perform(PlayerCommand.SellInventoryItem(sellEntry.inventoryIndex)))
        assertTrue(session.currentShardBalance() > beforeSell)

        if (session.currentShardBalance() < 18) {
            val nextSell = requireNotNull(session.renderSnapshot().uiState.activeShop).sellEntries.first()
            assertTrue(session.perform(PlayerCommand.SellInventoryItem(nextSell.inventoryIndex)))
        }

        val beforeBuy = session.currentShardBalance()
        assertTrue(session.perform(PlayerCommand.BuyShopOffer(0)))
        assertTrue(session.currentShardBalance() < beforeBuy)
        assertTrue("offer.healing_potion" in requireNotNull(session.shopStates().firstOrNull { it.shopId == "greenwood_supply_post" }).purchasedOfferIds)

        assertTrue(session.perform(PlayerCommand.CloseShop))
        assertTrue(session.renderSnapshot().uiState.activeShop == null)
    }

    @Test
    fun `save load preserves unlocked routes shard balance and headless turn equivalent`() {
        val saveManager = SaveManager(tempDir.resolve("route-roundtrip-save"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = saveManager,
            )

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertTrue(session.perform(PlayerCommand.SelectRoute(0)))
        assertTrue(session.perform(PlayerCommand.Wait))
        val expectedHeadless = session.currentHeadlessTurnEquivalent()
        val expectedShards = session.currentShardBalance()

        assertTrue(session.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager, GameLocale.EN_US))

        assertEquals(session.config.zoneId, loaded.config.zoneId)
        assertEquals(session.config.zoneRoute, loaded.config.zoneRoute)
        assertEquals(session.worldProgress().unlockedRoutes, loaded.worldProgress().unlockedRoutes)
        assertEquals(expectedShards, loaded.currentShardBalance())
        assertEquals(expectedHeadless, loaded.currentHeadlessTurnEquivalent())
    }

    @Test
    fun `underground river requires ferry anchor progress before abyssal temple route opens`() {
        val lockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "underground_river",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river"),
                        routeIndex = 4,
                    ),
                saveManager = SaveManager(tempDir.resolve("underground-river-locked-save")),
            )

        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        assertEquals("crystal_cavern", lockedSession.config.zoneId)
        assertTrue(lockedSession.renderSnapshot().uiState.activeRouteSelection == null)
        assertEquals(
            ObjectiveState.AVAILABLE,
            lockedSession.worldProgress().questStates["quest.underground_river"]?.objectiveStates?.get("crossing"),
        )
        assertTrue("quest.underground_river.cleared" !in lockedSession.worldProgress().worldFlags)

        val unlockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "underground_river",
                        playerProfessionId = "rogue",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river"),
                        routeIndex = 4,
                    ),
                saveManager = SaveManager(tempDir.resolve("underground-river-unlocked-save")),
            )

        unlockedSession.automationMovePlayerTo(interactablePoint(unlockedSession, "river_ferry_anchor"))
        assertTrue(unlockedSession.perform(PlayerCommand.Interact))
        assertEquals(
            ObjectiveState.IN_PROGRESS,
            unlockedSession.worldProgress().questStates["quest.underground_river"]?.objectiveStates?.get("crossing"),
        )
        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        val unlockedSelection = requireNotNull(unlockedSession.renderSnapshot().uiState.activeRouteSelection)
        assertTrue(unlockedSelection.options.any { option -> option.destinationZoneId == "abyssal_temple" })
        assertEquals(
            ObjectiveState.COMPLETED,
            unlockedSession.worldProgress().questStates["quest.underground_river"]?.objectiveStates?.get("crossing"),
        )
        assertTrue("quest.underground_river.cleared" in unlockedSession.worldProgress().worldFlags)
    }

    @Test
    fun `abyssal temple requires ward reliquary progress before abyssal heart route opens`() {
        val lockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "abyssal_temple",
                        playerProfessionId = "templar",
                        zoneRoute =
                            listOf(
                                "shattered_outpost",
                                "greenwood_fringe",
                                "deep_iron_pit",
                                "grey_gate_depths",
                                "underground_river",
                                "abyssal_temple",
                            ),
                        routeIndex = 5,
                    ),
                saveManager = SaveManager(tempDir.resolve("abyssal-temple-locked-save")),
            )

        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(lockedSession, stairPoint(lockedSession, StairDirection.DOWN))
        assertTrue(lockedSession.perform(PlayerCommand.Descend))
        assertTrue(lockedSession.isVictory())
        assertTrue(lockedSession.renderSnapshot().uiState.activeRouteSelection == null)
        assertEquals(
            ObjectiveState.AVAILABLE,
            lockedSession.worldProgress().questStates["quest.abyssal_temple"]?.objectiveStates?.get("sanctum"),
        )
        assertTrue("quest.abyssal_temple.cleared" !in lockedSession.worldProgress().worldFlags)

        val unlockedSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "abyssal_temple",
                        playerProfessionId = "templar",
                        zoneRoute =
                            listOf(
                                "shattered_outpost",
                                "greenwood_fringe",
                                "deep_iron_pit",
                                "grey_gate_depths",
                                "underground_river",
                                "abyssal_temple",
                            ),
                        routeIndex = 5,
                    ),
                saveManager = SaveManager(tempDir.resolve("abyssal-temple-unlocked-save")),
            )

        unlockedSession.automationMovePlayerTo(interactablePoint(unlockedSession, "temple_ward_reliquary"))
        assertTrue(unlockedSession.perform(PlayerCommand.Interact))
        assertEquals(
            ObjectiveState.IN_PROGRESS,
            unlockedSession.worldProgress().questStates["quest.abyssal_temple"]?.objectiveStates?.get("sanctum"),
        )
        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        movePlayerTo(unlockedSession, stairPoint(unlockedSession, StairDirection.DOWN))
        assertTrue(unlockedSession.perform(PlayerCommand.Descend))
        val unlockedSelection = unlockedSession.renderSnapshot().uiState.activeRouteSelection
        assertTrue(
            unlockedSession.config.zoneId == "abyssal_heart" ||
                unlockedSelection?.options?.any { option -> option.destinationZoneId == "abyssal_heart" } == true,
        )
        assertEquals(
            ObjectiveState.COMPLETED,
            unlockedSession.worldProgress().questStates["quest.abyssal_temple"]?.objectiveStates?.get("sanctum"),
        )
        assertTrue("quest.abyssal_temple.cleared" in unlockedSession.worldProgress().worldFlags)
    }

    @Test
    fun `defeating molten giant records boss kill and unlocks grey gate route`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "deep_iron_pit",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit"),
                        routeIndex = 2,
                    ),
                saveManager = SaveManager(tempDir.resolve("deep-iron-boss-route-save")),
            )

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(session.automationEntityByTemplateId("orc.molten_giant"))
        invokeHandleDeath(session, EntityId(bossId.value), session.playerId)

        assertTrue("orc.molten_giant" in session.worldProgress().defeatedBossIds)
        val routeSelection = requireNotNull(session.renderSnapshot().uiState.activeRouteSelection)
        assertTrue(routeSelection.options.any { option -> option.destinationZoneId == "grey_gate_depths" })
    }

    @Test
    fun `finale zone resolves to victory instead of reopening return routes`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "abyssal_heart",
                        playerProfessionId = "templar",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple", "abyssal_heart"),
                        routeIndex = 6,
                    ),
                saveManager = SaveManager(tempDir.resolve("abyssal-heart-finale-save")),
            )

        val bossId = requireNotNull(session.automationEntityByTemplateId("abyssal.guardian"))
        invokeHandleDeath(session, EntityId(bossId.value), session.playerId)

        assertTrue(session.isVictory())
        assertTrue(session.renderSnapshot().uiState.activeRouteSelection == null)
    }

    private fun movePlayerTo(
        session: FoundationGameSession,
        point: com.ktome.core.map.Point,
    ) {
        session.automationMovePlayerTo(point)
    }

    private fun stairPoint(
        session: FoundationGameSession,
        direction: StairDirection,
    ): com.ktome.core.map.Point =
        requireNotNull(session.automationStairPoint(direction))

    private fun interactablePoint(
        session: FoundationGameSession,
        interactableId: String,
    ): com.ktome.core.map.Point {
        val world = session.automationWorld()
        val entityId =
            requireNotNull(
                world.entitiesWith(Position::class, Interactable::class)
                    .firstOrNull { candidate -> world.get<Interactable>(candidate)?.id == interactableId },
            )
        return requireNotNull(world.get<Position>(entityId)).toPoint()
    }

    private fun invokeHandleDeath(
        session: FoundationGameSession,
        target: EntityId,
        killer: EntityId?,
    ) {
        val method =
            requireNotNull(
                FoundationGameSession::class.java.declaredMethods.firstOrNull { candidate ->
                    candidate.name.startsWith("handleDeath") && candidate.parameterCount == 2
                },
            ) {
                "Expected a private handleDeath overload on FoundationGameSession."
            }
        method.isAccessible = true
        method.invoke(session, target.value, killer)
    }
}
