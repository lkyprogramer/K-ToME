package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.economy.ShardEconomy
import com.ktome.core.economy.ShopServiceType
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.Inventory
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.save.SaveManager
import com.ktome.core.talent.EffectTracker
import com.ktome.core.world.ObjectiveState
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.ItemFactory
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunWorldStructureSessionTest {
    @TempDir
    lateinit var tempDir: Path

    private val itemBasesById = DataLoader().loadItemBundle().baseItems.associateBy { item -> item.id }

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
        requireNotNull(
            firstClearSession.milestoneRewardSummaries().firstOrNull { reward ->
                reward.rewardSource == MilestoneRewardSource.ROUTE &&
                    reward.sourceId == "route.greenwood_fringe.deep_iron_pit" &&
                    reward.qualityTier.ordinal >= ItemQuality.MAGIC.ordinal &&
                    reward.affixIds.isNotEmpty()
            },
        )

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
        requireNotNull(
            unlockSession.milestoneRewardSummaries().firstOrNull { reward ->
                reward.rewardSource == MilestoneRewardSource.ROUTE &&
                    reward.sourceId == "route.greenwood_fringe.bandit_camp" &&
                    reward.affixIds.isNotEmpty()
            },
        )
    }

    @Test
    fun `route reward is not claimed when guaranteed reward cannot fit inventory`() {
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
                saveManager = SaveManager(tempDir.resolve("route-reward-inventory-capacity-save")),
            )

        fillInventoryToCapacity(session)
        assertEquals(0, session.currentShardBalance())
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertTrue(session.perform(PlayerCommand.SelectRoute(1)))

        assertEquals("deep_iron_pit", session.config.zoneId)
        assertEquals(0, session.currentShardBalance())
        assertTrue("route.greenwood_fringe.deep_iron_pit" !in session.worldProgress().claimedRouteRewards)
        assertTrue(session.milestoneRewardSummaries().isEmpty())
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
    fun `refresh stock service replaces unpurchased stock and keeps rescue offers affordable`() {
        val saveManager = SaveManager(tempDir.resolve("refresh-stock-save"))
        val baselineSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = saveManager,
            )
        assertTrue(baselineSession.perform(PlayerCommand.SaveGame))
        val baseline = requireNotNull(saveManager.load())
        saveManager.save(baseline.copy(shardBalance = 200))
        val session = requireNotNull(GameModule.loadFoundationSession(saveManager))

        session.automationMovePlayerTo(interactablePoint(session, "merchant_stall"))
        assertTrue(session.perform(PlayerCommand.Interact))

        val offensiveOffer = requireNotNull(session.automationVisibleShopOffers().firstOrNull { offer -> "OFFENSE" in offer.tags })
        val initialServiceOffer =
            requireNotNull(
                session.automationVisibleShopOffers().firstOrNull { offer ->
                    offer.serviceType == ShopServiceType.REFRESH_STOCK
                },
            )
        assertTrue(session.perform(PlayerCommand.BuyShopOffer(indexOfOffer(session, offensiveOffer.id))))
        val refreshIndex = indexOfOffer(session, initialServiceOffer.id)
        assertTrue(session.perform(PlayerCommand.BuyShopOffer(refreshIndex)))

        val refreshedOffers = session.automationVisibleShopOffers()
        assertTrue(refreshedOffers.none { offer -> offer.id == offensiveOffer.id })
        assertTrue(refreshedOffers.any { offer -> offer.id.startsWith("offer.refresh.") })
        assertEquals(1, session.currentShopRefreshPurchaseCount())

        val shopState = requireNotNull(session.shopStates().firstOrNull { it.shopId == "greenwood_supply_post" })
        assertTrue(initialServiceOffer.id in shopState.purchasedOfferIds)
        assertTrue(shopState.activeRefreshableOffers.isNotEmpty())

        val shopSchema = requireNotNull(DataLoader().loadSchemaCatalog().shopNodes.firstOrNull { shop -> shop.id == "greenwood_supply_post" })
        val affordableRescue =
            ShardEconomy.mandatoryAffordableOffers(
                offers = refreshedOffers,
                balance = shopSchema.rescuePolicy.affordability.expectedShardBudgetByCheckpoint,
                requiredTags = shopSchema.rescuePolicy.affordability.requiredAffordableTags,
            )
        assertTrue(affordableRescue.size >= shopSchema.rescuePolicy.affordability.mandatoryAffordableItemCount)
        val affordableTags = affordableRescue.flatMapTo(linkedSetOf()) { offer -> offer.tags }
        assertTrue("RECOVERY" in affordableTags)
        assertTrue("PROTECTION" in affordableTags)

        assertFalse(session.perform(PlayerCommand.BuyShopOffer(refreshIndex)))
    }

    @Test
    fun `smoke bot can spend extra shards on refresh stock after rescue coverage is satisfied`() {
        val saveManager = SaveManager(tempDir.resolve("refresh-stock-bot-save"))
        val baselineSession =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = saveManager,
            )
        assertTrue(baselineSession.perform(PlayerCommand.SaveGame))
        val baseline = requireNotNull(saveManager.load())
        saveManager.save(baseline.copy(shardBalance = 200))
        val session = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val bot = SmokeBot()

        session.automationMovePlayerTo(interactablePoint(session, "merchant_stall"))
        assertTrue(session.perform(PlayerCommand.Interact))

        repeat(8) { turnIndex ->
            val activeShop = session.renderSnapshot().uiState.activeShop ?: return@repeat
            val command = bot.decide(RunObservationCapture.capture(session, turnIndex))
            assertTrue(
                command is PlayerCommand.BuyShopOffer || command == PlayerCommand.CloseShop,
                "Expected a shop command while active shop '${activeShop.shopId}' is open, but got $command.",
            )
            assertTrue(session.perform(command))
        }

        assertEquals(1, session.currentShopRefreshPurchaseCount())
        val shopState = requireNotNull(session.shopStates().firstOrNull { it.shopId == "greenwood_supply_post" })
        assertTrue("offer.refresh_stock" in shopState.purchasedOfferIds)
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
            lockedSession.worldProgress().questStates[AbyssalRuntimeKeys.Temple.QUEST_ID]?.objectiveStates?.get(AbyssalRuntimeKeys.Temple.OBJECTIVE_ID),
        )
        assertTrue("${AbyssalRuntimeKeys.Temple.QUEST_ID}.cleared" !in lockedSession.worldProgress().worldFlags)

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

        unlockedSession.automationMovePlayerTo(interactablePoint(unlockedSession, AbyssalRuntimeKeys.Temple.INTERACTABLE_ID))
        assertTrue(unlockedSession.perform(PlayerCommand.Interact))
        assertEquals(
            ObjectiveState.IN_PROGRESS,
            unlockedSession.worldProgress().questStates[AbyssalRuntimeKeys.Temple.QUEST_ID]?.objectiveStates?.get(AbyssalRuntimeKeys.Temple.OBJECTIVE_ID),
        )
        assertEquals(AbyssalRuntimeKeys.Temple.SHOP_NODE_ID, requireNotNull(unlockedSession.renderSnapshot().uiState.activeShop).shopId)
        assertTrue(unlockedSession.perform(PlayerCommand.CloseShop))
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
            unlockedSession.worldProgress().questStates[AbyssalRuntimeKeys.Temple.QUEST_ID]?.objectiveStates?.get(AbyssalRuntimeKeys.Temple.OBJECTIVE_ID),
        )
        assertTrue("${AbyssalRuntimeKeys.Temple.QUEST_ID}.cleared" in unlockedSession.worldProgress().worldFlags)
    }

    @Test
    fun `abyssal reliquary post spends shards without breaking abyssal heart access`() {
        val session =
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
                saveManager = SaveManager(tempDir.resolve("abyssal-reliquary-post-save")),
            )

        setShardBalance(session, 220)
        session.automationMovePlayerTo(interactablePoint(session, AbyssalRuntimeKeys.Temple.INTERACTABLE_ID))
        assertTrue(session.perform(PlayerCommand.Interact))
        assertEquals(AbyssalRuntimeKeys.Temple.SHOP_NODE_ID, requireNotNull(session.renderSnapshot().uiState.activeShop).shopId)
        assertTrue(session.perform(PlayerCommand.BuyShopOffer(indexOfOffer(session, "offer.reliquary.sanctified_seal"))))
        assertEquals(1, session.currentLateRunReliquaryPurchaseCount())
        assertEquals(1, session.currentLateRunReliquaryVisitCount())
        assertEquals(1, session.currentLateRunReliquaryItemPurchaseCount())
        assertEquals(0, session.currentLateRunReliquaryRefreshCount())
        assertEquals(0, session.currentLateRunReliquaryNonMandatoryPurchaseCount())
        assertEquals(78, session.currentLateRunReliquaryShardSpent())
        assertEquals(mapOf("PROTECTION" to 1), session.currentLateRunReliquaryPurchaseTagDistribution())
        assertTrue(session.perform(PlayerCommand.CloseShop))

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val routeSelection = session.renderSnapshot().uiState.activeRouteSelection
        assertTrue(
            session.config.zoneId == "abyssal_heart" ||
                routeSelection?.options?.any { option -> option.destinationZoneId == "abyssal_heart" } == true,
        )
    }

    @Test
    fun `heart ward focus marks finale objective in progress before abyssal guardian defeat`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260331L,
                        zoneId = "abyssal_heart",
                        playerProfessionId = "templar",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple", "abyssal_heart"),
                        routeIndex = 6,
                    ),
                saveManager = SaveManager(tempDir.resolve("abyssal-heart-focus-objective-save")),
            )

        session.automationMovePlayerTo(interactablePoint(session, AbyssalRuntimeKeys.Finale.INTERACTABLE_ID))
        assertTrue(session.perform(PlayerCommand.Interact))
        assertEquals(
            ObjectiveState.IN_PROGRESS,
            session.worldProgress().questStates[AbyssalRuntimeKeys.Finale.QUEST_ID]?.objectiveStates?.get(AbyssalRuntimeKeys.Finale.OBJECTIVE_ID),
        )
        assertTrue(
            requireNotNull(session.automationWorld().get<EffectTracker>(session.playerId))
                .activeEffects()
                .any { effect -> effect.schemaId in AbyssalRuntimeKeys.WARD_STATUS_IDS },
        )
        val eruptionEntity = session.automationWorld().entitiesWith(VoidEruptionRuntimeState::class).single()
        assertTrue(requireNotNull(session.automationWorld().get<VoidEruptionRuntimeState>(eruptionEntity)).stabilizedTurnsRemaining > 0)

        val bossId = requireNotNull(session.automationEntityByTemplateId("abyssal.guardian"))
        invokeHandleDeath(session, EntityId(bossId.value), session.playerId)

        assertTrue(session.isVictory())
        assertEquals(
            ObjectiveState.COMPLETED,
            session.worldProgress().questStates[AbyssalRuntimeKeys.Finale.QUEST_ID]?.objectiveStates?.get(AbyssalRuntimeKeys.Finale.OBJECTIVE_ID),
        )
        assertTrue("${AbyssalRuntimeKeys.Finale.QUEST_ID}.cleared" in session.worldProgress().worldFlags)
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
    fun `shattered outpost route milestone prefers open off hand when utility reward uses no slot`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260325L,
                        zoneId = "shattered_outpost",
                        playerProfessionId = "arcanist",
                        zoneRoute = listOf("shattered_outpost"),
                        routeIndex = 0,
                    ),
                saveManager = SaveManager(tempDir.resolve("shattered-outpost-route-slot-save")),
            )

        movePlayerTo(session, stairPoint(session, StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(session.automationEntityByTemplateId("bandit.captain"))
        invokeHandleDeath(session, EntityId(bossId.value), session.playerId)

        val rewardSummary =
            requireNotNull(
                session.milestoneRewardSummaries().firstOrNull { reward ->
                    reward.rewardSource == MilestoneRewardSource.ROUTE &&
                        reward.sourceId == "route.shattered_outpost.greenwood_fringe" &&
                        reward.qualityTier.ordinal >= ItemQuality.MAGIC.ordinal &&
                        reward.affixIds.isNotEmpty()
                },
            )
        assertEquals(EquipSlot.OFF_HAND, requireNotNull(itemBasesById[rewardSummary.baseItemId]).slot)
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

    private fun indexOfOffer(
        session: FoundationGameSession,
        offerId: String,
    ): Int =
        requireNotNull(
            session.automationVisibleShopOffers().indexOfFirst { offer -> offer.id == offerId }.takeIf { index -> index >= 0 },
        ) {
            "Expected visible offer '$offerId'."
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

    private fun fillInventoryToCapacity(session: FoundationGameSession) {
        val world = session.automationWorld()
        val inventory = requireNotNull(world.get<Inventory>(session.playerId))
        val seedItemId = requireNotNull(inventory.itemIds.firstOrNull()) { "Expected at least one starter inventory item." }
        val seedItem = requireNotNull(world.get<ItemInstance>(seedItemId)) { "Missing starter inventory item instance." }
        val itemFactory = ItemFactory()
        while (inventory.itemIds.size < inventory.capacity) {
            inventory.itemIds += itemFactory.createCarriedItem(world, seedItem.copy())
        }
    }
}
