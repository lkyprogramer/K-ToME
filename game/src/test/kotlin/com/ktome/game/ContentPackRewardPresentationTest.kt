package com.ktome.game

import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.contentpack.ContentPackFixtureCatalog
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.i18n.GameLocale
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ContentPackRewardPresentationTest {
    private data class SamplePackRewardOutcome(
        val seed: Long,
        val inventoryEntry: InventoryEntrySnapshot,
        val specialTemplateId: String,
    )

    private val samplePackHarnessSpec by lazy(LazyThreadSafetyMode.NONE) {
        ContentPackFixtureCatalog.harnessSpec(ContentPackFixtureCatalog.samplePackId)
    }
    private val sampleSecretBindingId = SearchBindingId("search.underground_river.crystal_rift")

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `sample pack reward uses pack-local presentation in live runtime and survives reload`() {
        val saveManager = SaveManager(tempDir.resolve("sample-pack-reward-save"))
        val session = newSamplePackSession(seed = firstSamplePackRewardOutcome().seed, saveManager = saveManager)

        val claimedEntry = claimSamplePackReward(session)
        val inventoryView = requireNotNull(session.inventoryItems().firstOrNull { item -> item.specialTemplateId != null })

        assertEquals("sample_flooded_relics.zone.flooded_reliquary.name", session.renderSnapshot().metadata.zoneNameKey)
        assertEquals(inventoryView.specialTemplateId, expectedTemplateId(claimedEntry))
        assertEquals(expectedNameKey(inventoryView.specialTemplateId), claimedEntry.item.nameKey)
        assertEquals(expectedVisualKey(inventoryView.specialTemplateId), claimedEntry.item.visualKey)
        assertEquals(expectedAudioProfile(inventoryView.specialTemplateId), claimedEntry.item.audioProfile)
        assertTrue(inventoryView.name.contains(expectedDisplayName(inventoryView.specialTemplateId)))
        assertNotNull(session.renderSnapshot().uiState.recentRewards.lastOrNull())

        assertTrue(session.perform(PlayerCommand.SaveGame))
        val loaded =
            requireNotNull(
                GameModule.loadFoundationSession(
                    saveManager = saveManager,
                    locale = GameLocale.EN_US,
                    contentPackSelection = samplePackSelection(),
                ),
            )
        val loadedInventoryView = requireNotNull(loaded.inventoryItems().firstOrNull { item -> item.specialTemplateId != null })
        val loadedEntry = claimedInventoryEntry(loaded)

        assertEquals(inventoryView.specialTemplateId, loadedInventoryView.specialTemplateId)
        assertEquals(inventoryView.name, loadedInventoryView.name)
        assertEquals(claimedEntry.item.nameKey, loadedEntry.item.nameKey)
        assertEquals(claimedEntry.item.visualKey, loadedEntry.item.visualKey)
        assertEquals(claimedEntry.item.audioProfile, loadedEntry.item.audioProfile)
    }

    @Test
    fun `sample pack fixed seeds cover both special templates through live secret-route execution`() {
        val generatedTemplateIds =
            samplePackRewardOutcomes(seedList = samplePackHarnessSpec.generatedTemplateSeeds)
                .map(SamplePackRewardOutcome::specialTemplateId)
                .toSet()

        assertEquals(
            setOf(
                "sample.flooded_relics.unique.floodtide_lantern",
                "sample.flooded_relics.artifact.tideglass_echo",
            ),
            generatedTemplateIds,
        )
    }

    @Test
    fun `sample pack inventory logs keep pack-local name keys after reward is claimed`() {
        val session = newSamplePackSession(seed = firstSamplePackRewardOutcome().seed, saveManager = SaveManager(tempDir.resolve("sample-pack-drop-log")))

        claimSamplePackReward(session)
        val rewardItem = requireNotNull(session.inventoryItems().firstOrNull { item -> item.specialTemplateId != null })

        assertTrue(session.perform(PlayerCommand.DropInventoryItem(rewardItem.index)))
        val dropLog = requireNotNull(logEventByKey(session, "log.inventory.drop"))
        val itemArgument = dropLog.message.arguments.single { argument -> argument.name == "item" }
        val displayToken = requireNotNull(itemArgument.valueToken)

        assertEquals("item.display.composed", displayToken.key)
        assertEquals(
            expectedNameKey(rewardItem.specialTemplateId),
            displayToken.arguments.first { argument -> argument.name == "base" }.valueKey,
        )
    }

    private fun newSamplePackSession(
        seed: Long,
        saveManager: SaveManager,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = seed, zoneId = "underground_river", playerProfessionId = "arcanist"),
            saveManager = saveManager,
            locale = GameLocale.EN_US,
            contentPackSelection = samplePackSelection(),
        )

    private fun claimSamplePackReward(session: FoundationGameSession): InventoryEntrySnapshot {
        clearMonsters(session)
        assertTrue(session.perform(PlayerCommand.DropInventoryItem(session.inventoryItems().first().index)))
        session.automationMovePlayerTo(requireNotNull(session.automationInteractablePoint("crystal_cache_chest")))
        assertTrue(session.perform(PlayerCommand.Interact))
        session.automationMovePlayerTo(requireNotNull(session.automationSearchPointForBinding(sampleSecretBindingId)))
        assertTrue(session.perform(PlayerCommand.Search))
        assertTrue(session.automationSearchState().single().result == com.ktome.core.world.solvability.SearchActionResult.REVEALED)

        session.automationMovePlayerTo(requireNotNull(session.automationHiddenEntrancePointForBinding(sampleSecretBindingId)))
        assertTrue(session.perform(PlayerCommand.Interact))
        assertTrue(session.automationVisitedSecretZoneIds().any { secretZone -> secretZone.id == "underground_river_crystal_rift" })
        val secretZoneId = requireNotNull(session.automationVisitedSecretZoneIds().firstOrNull())
        val content = sessionContent(session)
        val secretZone = requireNotNull(content.secretZone(secretZoneId))
        assertEquals(listOf("hidden.event.underground_river.crystal_rift.reward"), secretZone.guaranteedContent.map { contentRef -> contentRef.id })
        val hiddenEvent = requireNotNull(content.hiddenEventRegistry.resolve("hidden.event.underground_river.crystal_rift.reward"))
        assertNotNull(hiddenEvent)
        val rewardProfileId =
            hiddenEvent.rewards
                .mapNotNull { reward -> reward.payload as? com.ktome.game.hidden.HiddenEventRewardPayload.LootProfile }
                .first()
                .lootProfileRef
                .id
        assertEquals("sample.flooded_relics.loot.flooded_reliquary.secret", rewardProfileId)

        session.automationMovePlayerTo(requireNotNull(session.automationSecretRewardPointForBinding(sampleSecretBindingId)))
        check(session.perform(PlayerCommand.Interact)) {
            "Failed to claim sample-pack reward. zone=${session.renderSnapshot().metadata.zoneNameKey} consumed=${session.automationConsumedHiddenEventIds()} props=${session.renderSnapshot().props.map { prop -> prop.propTypeId to Point(prop.x, prop.y) }}"
        }
        assertEquals("sample_flooded_relics.zone.flooded_reliquary.name", session.renderSnapshot().metadata.zoneNameKey)
        return claimedInventoryEntry(session)
    }

    private fun claimedInventoryEntry(session: FoundationGameSession): InventoryEntrySnapshot =
        requireNotNull(
            session.renderSnapshot().uiState.inventory.firstOrNull { entry ->
                entry.item.nameKey.startsWith("sample_flooded_relics.item.")
            },
        ) {
            "Expected sample-pack inventory entry. keys=${session.renderSnapshot().uiState.inventory.map { entry -> entry.item.nameKey }} templates=${session.inventoryItems().map { item -> item.specialTemplateId }}"
        }

    private fun firstSamplePackRewardOutcome(): SamplePackRewardOutcome {
        val failures = mutableListOf<String>()
        samplePackHarnessSpec.generatedTemplateSeeds.forEach { seed ->
            runCatching { samplePackRewardOutcome(seed) }
                .onSuccess { outcome -> return outcome }
                .onFailure { throwable ->
                    failures += "$seed:${throwable.message ?: throwable::class.simpleName.orEmpty()}"
                }
        }
        error("Expected at least one deterministic generated-template seed to produce a pack-local secret reward. failures=$failures")
    }

    private fun samplePackRewardOutcomes(seedList: Iterable<Long>): List<SamplePackRewardOutcome> {
        val failures = mutableListOf<String>()
        val outcomes =
            buildList {
                seedList.forEach { seed ->
                    runCatching { samplePackRewardOutcome(seed) }
                        .onSuccess(::add)
                        .onFailure { throwable ->
                            failures += "$seed:${throwable.message ?: throwable::class.simpleName.orEmpty()}"
                        }
                }
            }
        assertTrue(
            failures.isEmpty(),
            "Sample-pack generated-template seeds must stay reproducible. successes=${outcomes.map { outcome -> "${outcome.seed}:${outcome.specialTemplateId}" }} failures=$failures",
        )
        return outcomes
    }

    private fun samplePackRewardOutcome(seed: Long): SamplePackRewardOutcome {
        val session = newSamplePackSession(seed = seed, saveManager = SaveManager(tempDir.resolve("sample-pack-seed-$seed")))
        val inventoryEntry = claimSamplePackReward(session)
        val specialTemplateId =
            requireNotNull(requireNotNull(session.inventoryItems().firstOrNull { item -> item.specialTemplateId != null }).specialTemplateId) {
                "Missing generated special template for sample-pack seed $seed."
            }
        return SamplePackRewardOutcome(
            seed = seed,
            inventoryEntry = inventoryEntry,
            specialTemplateId = specialTemplateId,
        )
    }

    private fun clearMonsters(session: FoundationGameSession) {
        val world = session.automationWorld()
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    private fun logEventByKey(
        session: FoundationGameSession,
        key: String,
    ) = session.renderSnapshot().logEvents.firstOrNull { event -> event.message.key == key }

    private fun sessionContent(session: FoundationGameSession): GameContent =
        session.javaClass.getDeclaredField("content").let { field ->
            field.isAccessible = true
            field.get(session) as GameContent
        }

    private fun samplePackSelection(): ContentPackSelection =
        ContentPackFixtureCatalog.selection(activePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()))

    private fun expectedTemplateId(entry: InventoryEntrySnapshot): String =
        when (entry.item.nameKey) {
            "sample_flooded_relics.item.floodtide_lantern.name" -> "sample.flooded_relics.unique.floodtide_lantern"
            "sample_flooded_relics.item.tideglass_echo.name" -> "sample.flooded_relics.artifact.tideglass_echo"
            else -> error("Unexpected sample-pack item '${entry.item.nameKey}'.")
        }

    private fun expectedNameKey(templateId: String?): String =
        when (templateId) {
            "sample.flooded_relics.unique.floodtide_lantern" -> "sample_flooded_relics.item.floodtide_lantern.name"
            "sample.flooded_relics.artifact.tideglass_echo" -> "sample_flooded_relics.item.tideglass_echo.name"
            else -> error("Unexpected sample-pack template '$templateId'.")
        }

    private fun expectedVisualKey(templateId: String?): String =
        when (templateId) {
            "sample.flooded_relics.unique.floodtide_lantern" -> "sample_flooded_relics.item.floodtide_lantern.visual"
            "sample.flooded_relics.artifact.tideglass_echo" -> "sample_flooded_relics.item.tideglass_echo.visual"
            else -> error("Unexpected sample-pack template '$templateId'.")
        }

    private fun expectedAudioProfile(templateId: String?): String =
        when (templateId) {
            "sample.flooded_relics.unique.floodtide_lantern" -> "sample_flooded_relics.audio.item.floodtide_lantern"
            "sample.flooded_relics.artifact.tideglass_echo" -> "sample_flooded_relics.audio.item.tideglass_echo"
            else -> error("Unexpected sample-pack template '$templateId'.")
        }

    private fun expectedDisplayName(templateId: String?): String =
        when (templateId) {
            "sample.flooded_relics.unique.floodtide_lantern" -> "Floodtide Lantern"
            "sample.flooded_relics.artifact.tideglass_echo" -> "Tideglass Echo"
            else -> error("Unexpected sample-pack template '$templateId'.")
        }
}
