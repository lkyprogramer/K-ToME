package com.ktome.client.audio

import com.ktome.client.assets.AudioManifestResourceLoader
import com.ktome.client.assets.AudioManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.game.PlayerCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioRouterTest {
    @Test
    fun `menu interactions map to the expected ui cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)

        router.onMenuInteraction(selectionChanged = true)
        router.onMenuInteraction(accepted = true)
        router.onMenuInteraction(rejected = true)

        assertEquals(
            listOf("audio.ui.hover", "audio.ui.confirm", "audio.ui.cancel"),
            sink.events,
        )
    }

    @Test
    fun `menu and gameplay transitions switch the background track once per exact cue`() {
        val sink = RecordingBackgroundAudioSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), backgroundSink = sink)
        val snapshot = sampleSnapshot()

        router.onMenuShown()
        router.onMenuShown()
        router.onSnapshotUpdated(null, snapshot)
        router.onSnapshotUpdated(snapshot, snapshot)

        assertEquals(listOf("audio.music.menu", "audio.zone.shattered_outpost"), sink.transitions)
    }

    @Test
    fun `overlay transitions and inventory movement emit navigation cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)

        router.onOverlayStateChanged(
            previous = OverlayState(mode = UiMode.MAP),
            current = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0),
        )
        router.onOverlayStateChanged(
            previous = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 0),
            current = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 1),
        )
        router.onOverlayStateChanged(
            previous = OverlayState(mode = UiMode.INVENTORY, inventorySelection = 1),
            current = OverlayState(mode = UiMode.MAP),
        )

        assertEquals(
            listOf("audio.ui.confirm", "audio.ui.hover", "audio.ui.cancel"),
            sink.events,
        )
    }

    @Test
    fun `command feedback distinguishes movement from bump attacks and prefers talent audio`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val movedSnapshot =
            sampleSnapshot().copy(
                metadata =
                    sampleSnapshot().metadata.copy(
                        playerX = 1,
                        playerY = 0,
                    ),
            )
        val stationarySnapshot = sampleSnapshot()

        router.onCommandResolved(sampleSnapshot(), movedSnapshot, PlayerCommand.Move(com.ktome.core.map.Point(1, 0)), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Move(com.ktome.core.map.Point(1, 0)), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.UseTalent(slot = 1), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.UseTalent(slot = 3), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Interact, consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Descend, consumed = false)

        assertEquals(
            listOf(
                "audio.footstep.default",
                "audio.melee.light",
                "audio.talent.power_strike",
                "audio.spell.basic",
                "audio.interactable.open",
                "audio.ui.cancel",
            ),
            sink.events,
        )
    }

    @Test
    fun `new boss warning overlay emits its audio cue once`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                overlays =
                    listOf(
                        com.ktome.core.snapshot.OverlayRenderSnapshot(
                            id = "boss-warning:7",
                            visualKey = "vfx.boss.warning.sigil_01",
                            audioProfile = "audio.boss.warning",
                            previewTurns = 1,
                            dangerLevel = 3,
                            shape = com.ktome.core.snapshot.OverlayShapeSnapshot.SINGLE_TILE,
                            sourceAbilityId = "dungeon_lord_encounter",
                            cells = emptyList(),
                        ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)
        router.onSnapshotUpdated(current, current)

        assertEquals(listOf("audio.boss.warning"), sink.events)
    }

    private fun sampleSnapshot(): RenderSnapshot =
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
            mapCells = emptyList(),
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
                            nextLevelRequirement = 10,
                            statPoints = 0,
                            talentPoints = 0,
                            attack = 7,
                            defense = 5,
                            accuracy = 6,
                            evasion = 4,
                            speed = 100,
                        ),
                    equipment = emptyList(),
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                iconKey = "icon.skill.vanguard.power_strike",
                                audioProfile = "audio.talent.power_strike",
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 8,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 1,
                                minRange = 0,
                                currentCooldown = 0,
                                maxCooldown = 0,
                                requiresTarget = false,
                            ),
                            TalentSlotSnapshot(
                                slot = 3,
                                talentId = "mystery_spell",
                                nameKey = "talent.unknown.name",
                                iconKey = null,
                                audioProfile = null,
                                level = 1,
                                maxLevel = 1,
                                resourceCost = 4,
                                resourceLabelKey = "ui.hud.mana.short",
                                resourceTypeId = "MANA",
                                range = 4,
                                minRange = 0,
                                currentCooldown = 0,
                                maxCooldown = 0,
                                requiresTarget = true,
                            ),
                        ),
                    inventory = emptyList(),
                    targetablePositions = emptyList(),
                ),
        )
}

private class RecordingAudioCueSink : AudioCueSink {
    val events = mutableListOf<String>()

    override fun emit(cue: com.ktome.client.assets.ResolvedAudioCue) {
        events += cue.resolvedKey
    }
}

private class RecordingBackgroundAudioSink : BackgroundAudioSink {
    val transitions = mutableListOf<String>()
    private var currentKey: String? = null

    override fun transitionTo(cue: com.ktome.client.assets.ResolvedAudioCue?) {
        val nextKey = cue?.resolvedKey
        if (nextKey == currentKey) {
            return
        }
        currentKey = nextKey
        nextKey?.let(transitions::add)
    }
}
