package com.ktome.client

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.AssetVersionContract
import com.ktome.core.save.AssetVersionGate
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StairSnapshot
import com.ktome.client.assets.AudioManifest
import com.ktome.client.assets.AudioManifestEntry
import com.ktome.client.assets.AudioManifestResourceLoader
import com.ktome.client.assets.ClientFontCatalog
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestResourceLoader
import com.ktome.core.profile.AdvancedClassUnlockRule
import com.ktome.core.profile.ProfileData
import com.ktome.core.profile.ProfileManager
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.FoundationGameConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GameAppLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `refresh availability reflects loadable save`() {
        val saveManager = SaveManager(tempDir.resolve("lifecycle-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)

        assertTrue(coordinator.refreshContinueAvailability())
        assertTrue(coordinator.cachedContinueAvailability())
    }

    @Test
    fun `start new session only deletes save after creation`() {
        val saveManager = SaveManager(tempDir.resolve("new-game-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)

        assertThrows(IllegalStateException::class.java) {
            coordinator.startNewSession<Any> { throw IllegalStateException("Initialization failed") }
        }
        assertTrue(saveManager.savePath().exists())

        coordinator.startNewSession { Any() }
        assertFalse(saveManager.savePath().exists())
    }

    @Test
    fun `continue session clears availability when loader fails`() {
        val saveManager = SaveManager(tempDir.resolve("continue-save"))
        saveManager.save(sampleSnapshot())
        val coordinator = LifecycleCoordinator(saveManager)
        coordinator.refreshContinueAvailability()
        assertTrue(coordinator.cachedContinueAvailability())

        val session = coordinator.continueSession<Any> { null }
        assertNull(session)
        assertFalse(coordinator.cachedContinueAvailability())
    }

    @Test
    fun `legacy save surfaces an explicit notice`() {
        val saveManager = SaveManager(tempDir.resolve("legacy-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText("""{"version":2,"timestampEpochMillis":123}""")
        val coordinator = LifecycleCoordinator(saveManager)

        assertFalse(coordinator.refreshContinueAvailability())
        assertTrue(coordinator.consumeNotice()?.contains("Legacy saves") == true)
    }

    @Test
    fun `asset contract coordinator surfaces mismatch notice`() {
        val coordinator =
            AssetContractCoordinator(
                assetVersionProvider = {
                    AssetVersionContract.CURRENT.copy(visualManifestVersion = AssetVersionContract.CURRENT.visualManifestVersion + 1)
                },
                visualManifestProvider = { sampleVisualManifest() },
                audioManifestProvider = { sampleAudioManifest() },
                assetVersionGate = AssetVersionGate(),
            )

        val notice = coordinator.noticeOrNull()

        assertTrue(notice?.contains("Asset contract mismatch") == true)
    }

    @Test
    fun `asset contract coordinator surfaces load failure notice`() {
        val coordinator =
            AssetContractCoordinator(
                assetVersionProvider = {
                    throw AssetVersionLoadException("manifest missing")
                },
                visualManifestProvider = { sampleVisualManifest() },
                audioManifestProvider = { sampleAudioManifest() },
            )

        assertEquals("manifest missing", coordinator.noticeOrNull())
    }

    @Test
    fun `asset contract coordinator surfaces invalid manifest notice`() {
        val coordinator =
            AssetContractCoordinator(
                assetVersionProvider = { AssetVersionContract.CURRENT },
                visualManifestProvider = {
                    sampleVisualManifest().copy(
                        entries =
                            listOf(
                                sampleVisualManifest().entries.first(),
                                sampleVisualManifest().entries.first(),
                            ),
                    )
                },
                audioManifestProvider = { sampleAudioManifest() },
            )

        assertEquals("Client asset bundle is invalid.", coordinator.noticeOrNull())
    }

    @Test
    fun `asset contract coordinator tracks bootstrap session and warm cache phases`() {
        val coordinator =
            AssetContractCoordinator(
                assetVersionProvider = { AssetVersionContract.CURRENT },
                visualManifestProvider = { phase2VisualManifest() },
                audioManifestProvider = { phase2AudioManifest() },
            )

        assertNull(coordinator.noticeOrNull())
        val bootstrapState = requireNotNull(coordinator.loadStateOrNull())
        assertTrue(bootstrapState.bootstrapLoaded)
        assertTrue(ClientFontCatalog.UI_FONT_RESOURCE_ID in requireNotNull(bootstrapState.bootstrapDescriptor).fontResources)
        assertTrue("audio.music.menu" in requireNotNull(bootstrapState.bootstrapDescriptor).menuAudioKeys)
        assertTrue("audio.ui.hover" in requireNotNull(bootstrapState.bootstrapDescriptor).menuAudioKeys)

        coordinator.prepareSession(sampleRenderSnapshot())
        val sessionState = requireNotNull(coordinator.loadStateOrNull())
        assertEquals("shattered_outpost", sessionState.sessionZoneId)
        assertEquals("tileset.ruins", requireNotNull(sessionState.sessionDescriptor).tilesetKey)
        assertTrue("ambient.shattered_outpost" in requireNotNull(sessionState.sessionDescriptor).ambientAudioKeys)
        assertTrue("audio.interactable.open" in requireNotNull(sessionState.sessionDescriptor).interactionAudioKeys)
        assertTrue("tileset.ruins.ground_01" in requireNotNull(sessionState.sessionDescriptor).terrainVisualKeys)
        assertTrue("tileset.ruins.ground_01" in sessionState.sessionVisualKeys)
        assertTrue("ambient.shattered_outpost" in sessionState.sessionAudioKeys)
        assertTrue("audio.spell.basic" in sessionState.sessionAudioKeys)
        assertTrue(sessionState.warmVisualKeys.isEmpty())

        coordinator.warmCache(sampleRenderSnapshot())
        val warmState = requireNotNull(coordinator.loadStateOrNull())
        assertTrue("vfx.boss.warning.sigil_01" in requireNotNull(warmState.warmCacheDescriptor).highValueVfxKeys)
        assertTrue("vfx.boss.warning.sigil_01" in warmState.warmVisualKeys)
        assertTrue("audio.boss.warning" in warmState.warmAudioKeys)
    }

    @Test
    fun `malformed profile disables persistence and surfaces explicit notice`() {
        val profileDir = tempDir.resolve("corrupt-profile")
        Files.createDirectories(profileDir)
        profileDir.resolve(ProfileManager.DEFAULT_FILE_NAME).writeText("""{"profileVersion":"oops"}""")
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

        val result = loadProfilePersistenceState(ProfileManager(profileDir), localizer)

        assertEquals(ProfileData(), result.profileData)
        assertFalse(result.persistenceEnabled)
        assertEquals(localizer.text("ui.menu.profile_load_failed"), result.notice)
    }

    @Test
    fun `failed profile save keeps previous in memory progression`() {
        val profileDir = tempDir.resolve("blocked-profile")
        Files.createDirectories(profileDir)
        Files.createDirectories(profileDir.resolve(ProfileManager.DEFAULT_FILE_NAME))
        val profileManager = ProfileManager(profileDir)
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val originalProfile = ProfileData(releaseUnlockedClasses = setOf("berserker"))

        val result =
            appendAndPersistProfileRun(
                profileManager = profileManager,
                profile = originalProfile,
                persistenceEnabled = true,
                summary =
                    com.ktome.core.profile.RunSummary(
                        seed = 1L,
                        finishedAtEpochMillis = 2L,
                        classId = "arcanist",
                        raceId = "human",
                        finalZoneId = "abyssal_temple",
                        turnCount = 100,
                        headlessTurnEquivalent = 100,
                        zoneRouteHash = "route",
                        buildHash = "build",
                        rulesetVersion = "phase3",
                        victory = true,
                    ),
                unlockRules = listOf(AdvancedClassUnlockRule(classId = "spellblade", requiredProfessionId = "arcanist")),
                localizer = localizer,
            )

        assertEquals(originalProfile, result.profileData)
        assertFalse(result.persisted)
        assertEquals(localizer.text("ui.menu.profile_save_failed"), result.notice)
    }

    @Test
    fun `new game config copies selected profession and race`() {
        val config =
            newGameConfig(
                defaultConfig = FoundationGameConfig(playerProfessionId = "vanguard", playerRaceId = "human"),
                professionId = "spellblade",
                raceId = "elf",
            )

        assertEquals("spellblade", config.playerProfessionId)
        assertEquals("elf", config.playerRaceId)
    }
}

private fun sampleSnapshot(): SaveSnapshot =
    SaveSnapshot(
        timestampEpochMillis = 123L,
        worldSeed = 20260312L,
        currentZoneId = "shattered_outpost",
        floorIndex = 2,
        mapWidth = 60,
        mapHeight = 40,
        fovRadius = 8,
        messageLogSize = 8,
        playerProfessionId = "vanguard",
        playerRaceId = "human",
        maxFloor = 2,
        turnCount = 18,
        player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(4, 5),
                        blocksMovement = true,
                        faction = "PLAYER",
                        isPlayerControlled = true,
                    ),
            ),
        floors =
            listOf(
                FloorSnapshot(
                    floorIndex = 1,
                    map = sizedMap(width = 60, height = 40, playerStart = PointSnapshot(0, 0)),
                    stairsDown = PointSnapshot(4, 0),
                ),
                FloorSnapshot(
                    floorIndex = 2,
                    map = sizedMap(width = 60, height = 40, playerStart = PointSnapshot(1, 0)),
                    stairsUp = PointSnapshot(0, 0),
                    stairsDown = PointSnapshot(4, 0),
                    exploredTiles = listOf(PointSnapshot(0, 0), PointSnapshot(1, 0)),
                    entities =
                        listOf(
                            EntitySnapshot(
                                id = 9,
                                position = PointSnapshot(4, 0),
                                stair = StairSnapshot(StairDirection.DOWN.name),
                            ),
                        ),
                ),
            ),
    )

private fun sizedMap(
    width: Int,
    height: Int,
    playerStart: PointSnapshot,
): MapSnapshot =
    MapSnapshot(
        rows = List(height) { ".".repeat(width) },
        playerStart = playerStart,
    )

private fun sampleVisualManifest(): VisualManifest =
    VisualManifest(
        manifestVersion = 1,
        styleTag = "ktome-middle-fantasy-painterly-tile-v1",
        fallbackKey = "missing_visual",
        entries =
            listOf(
                VisualManifestEntry(
                    key = "missing_visual",
                    category = "debug",
                    rawOutputPath = "debug/missing_visual.png",
                    footprint = "ui",
                ),
            ),
        prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
    )

private fun sampleAudioManifest(): AudioManifest =
    AudioManifest(
        manifestVersion = 1,
        fallbackKey = "audio.fallback.silence",
        entries =
            listOf(
                AudioManifestEntry(
                    key = "audio.fallback.silence",
                    cueFamily = "silence",
                    eventId = "silence.default",
                    sourcePath = "audio/fallback/silence.ogg",
                ),
            ),
    )

private fun phase2VisualManifest(): VisualManifest = VisualManifestResourceLoader.load()

private fun phase2AudioManifest(): AudioManifest = AudioManifestResourceLoader.load()

private fun sampleRenderSnapshot(): RenderSnapshot =
    RenderSnapshot(
        metadata =
            RenderMetadataSnapshot(
                revision = 1,
                zoneId = "shattered_outpost",
                zoneNameKey = "zone.shattered_outpost.name",
                currentFloor = 1,
                maxFloor = 2,
                width = 2,
                height = 2,
                playerX = 0,
                playerY = 0,
                zoneVisualKey = "zone.shattered_outpost.visual",
                zoneAudioProfile = "audio.zone.shattered_outpost",
                tilesetKey = "tileset.ruins",
                ambientProfile = "ambient.shattered_outpost",
            ),
        mapCells =
            listOf(
                MapCellSnapshot(0, 0, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "floor", terrainVisualKey = "tileset.ruins.ground_01"),
                MapCellSnapshot(1, 0, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "floor", terrainVisualKey = "tileset.ruins.ground_01", stairDirectionId = "DOWN"),
            ),
        props =
            listOf(
                PropRenderSnapshot(
                    id = "stairs:down:9",
                    x = 1,
                    y = 0,
                    propTypeId = "stairs",
                    stairDirectionId = "DOWN",
                    visualKey = "prop.stairs.down",
                    audioProfile = "audio.interactable.stairs",
                ),
            ),
        actors =
            listOf(
                ActorRenderSnapshot(
                    entityId = 1,
                    x = 0,
                    y = 0,
                    visualKey = "actor.vanguard",
                    audioProfile = "audio.profession.vanguard",
                    nameKey = "actor.player.name",
                    isPlayer = true,
                    roleKind = ActorRoleKindSnapshot.PLAYER,
                ),
            ),
        overlays =
            listOf(
                OverlayRenderSnapshot(
                    id = "boss-warning:7",
                    visualKey = "vfx.boss.warning.sigil_01",
                    audioProfile = "audio.boss.warning",
                    previewTurns = 1,
                    dangerLevel = 3,
                    shape = OverlayShapeSnapshot.SINGLE_TILE,
                    sourceAbilityId = "dungeon_lord_encounter",
                    cells = listOf(GridPointSnapshot(1, 0)),
                ),
            ),
        uiState =
            RenderUiStateSnapshot(
                playerStatus =
                    PlayerStatusSnapshot(
                        currentHp = 24,
                        maxHp = 24,
                        currentResource = 12,
                        maxResource = 12,
                        resourceLabelKey = "ui.hud.stamina.short",
                        resourceTypeId = "STAMINA",
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 12,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 7,
                        defense = 5,
                        accuracy = 6,
                        evasion = 4,
                        speed = 100,
                    ),
                equipment = emptyList(),
                talents = emptyList(),
                inventory = emptyList(),
                targetablePositions = emptyList(),
            ),
    )
