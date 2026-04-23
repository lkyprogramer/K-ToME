package com.ktome.client.audio

import com.ktome.client.assets.AudioManifestResourceLoader
import com.ktome.client.assets.AudioManifestResolver
import com.ktome.core.snapshot.ActorMutationRenderSnapshot
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.BossVariantRenderSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
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
        val routeSnapshot =
            snapshot.copy(
                metadata =
                    snapshot.metadata.copy(
                        zoneId = "greenwood_fringe",
                        zoneNameKey = "zone.greenwood_fringe.name",
                        zoneVisualKey = "zone.greenwood_fringe.visual",
                        zoneAudioProfile = "audio.zone.greenwood_fringe",
                        ambientProfile = "ambient.greenwood_fringe",
                    ),
            )

        router.onMenuShown()
        router.onMenuShown()
        router.onSnapshotUpdated(null, snapshot)
        router.onSnapshotUpdated(snapshot, routeSnapshot)
        router.onSnapshotUpdated(routeSnapshot, routeSnapshot)

        assertEquals(listOf("audio.music.menu", "audio.zone.shattered_outpost", "audio.zone.greenwood_fringe"), sink.transitions)
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
            listOf("audio.ui.card_open", "audio.ui.hover", "audio.ui.cancel"),
            sink.events,
        )
    }

    @Test
    fun `runtime error presentation emits dedicated critical error cue`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)

        router.onCriticalError()

        assertEquals(listOf("audio.ui.critical_error"), sink.events)
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
        val powerStrikeSnapshot =
            stationarySnapshot.copy(
                uiState =
                    stationarySnapshot.uiState.copy(
                        playerStatus = stationarySnapshot.uiState.playerStatus.copy(currentResource = 4),
                    ),
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.damage")),
                    ),
            )

        router.onCommandResolved(sampleSnapshot(), movedSnapshot, PlayerCommand.Move(com.ktome.core.map.Point(1, 0)), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Move(com.ktome.core.map.Point(1, 0)), consumed = true)
        router.onCommandResolved(sampleSnapshot(), powerStrikeSnapshot, PlayerCommand.UseTalent(slot = 1), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.UseTalent(slot = 3), consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Interact, consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Descend, consumed = true)
        router.onCommandResolved(sampleSnapshot(), stationarySnapshot, PlayerCommand.Descend, consumed = false)

        assertEquals(
            listOf(
                "audio.footstep.default",
                "audio.melee.light",
                "audio.talent.power_strike",
                "audio.resource.stamina.spend",
                "audio.damage.physical_hit",
                "audio.spell.basic",
                "audio.interactable.open",
                "audio.interactable.stairs",
                "audio.ui.cancel",
            ),
            sink.events,
        )
    }

    @Test
    fun `movement prefers terrain cue when the player steps onto formal terrain audio`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                metadata =
                    previous.metadata.copy(
                        playerX = 1,
                        playerY = 0,
                    ),
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 1,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "water",
                            terrainVisualKey = "vfx.terrain.interaction.water",
                            terrainAudioProfile = "audio.terrain.water",
                        ),
                    ),
            )

        router.onCommandResolved(previous, current, PlayerCommand.Move(com.ktome.core.map.Point(1, 0)), consumed = true)

        assertEquals(listOf("audio.terrain.water"), sink.events)
    }

    @Test
    fun `talent miss keeps cast audio but suppresses hit cue`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val missedStrikeSnapshot =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        playerStatus = previous.uiState.playerStatus.copy(currentResource = 4),
                    ),
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.miss")),
                    ),
            )

        router.onCommandResolved(previous, missedStrikeSnapshot, PlayerCommand.UseTalent(slot = 1), consumed = true)

        assertEquals(
            listOf("audio.talent.power_strike", "audio.resource.stamina.spend"),
            sink.events,
        )
    }

    @Test
    fun `talent miss suppresses hit cue even if damage logs are also present`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val ambiguousSnapshot =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        playerStatus = previous.uiState.playerStatus.copy(currentResource = 4),
                    ),
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.miss")),
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.damage")),
                    ),
            )

        router.onCommandResolved(previous, ambiguousSnapshot, PlayerCommand.UseTalent(slot = 1), consumed = true)

        assertEquals(
            listOf("audio.talent.power_strike", "audio.resource.stamina.spend"),
            sink.events,
        )
    }

    @Test
    fun `search command reuses interactable cues without introducing a dedicated audio key`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val snapshot = sampleSnapshot()

        router.onCommandResolved(snapshot, snapshot, PlayerCommand.Search, consumed = true)
        router.onCommandResolved(snapshot, snapshot, PlayerCommand.Search, consumed = false)

        assertEquals(listOf("audio.interactable.open", "audio.ui.cancel"), sink.events)
    }

    @Test
    fun `snapshot log and resource changes emit progression and restore cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        playerStatus =
                            previous.uiState.playerStatus.copy(
                                currentResource = 15,
                                maxResource = 18,
                                level = 2,
                            ),
                    ),
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.level_up")),
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.unlock")),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(
            listOf("audio.ui.level_up", "audio.ui.talent_unlock", "audio.resource.stamina.restore"),
            sink.events,
        )
    }

    @Test
    fun `snapshot transitions emit mutation variant and terrain audio cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous =
            sampleSnapshot().copy(
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "water",
                            terrainVisualKey = "vfx.terrain.interaction.water",
                            terrainAudioProfile = "audio.terrain.water",
                        ),
                    ),
            )
        val current =
            previous.copy(
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "ice",
                            terrainVisualKey = "vfx.terrain.interaction.ice",
                            terrainAudioProfile = "audio.terrain.ice",
                        ),
                    ),
                actors =
                    listOf(
                        ActorRenderSnapshot(
                            entityId = 7,
                            x = 1,
                            y = 1,
                            visualKey = "actor.bandit.captain",
                            nameKey = "monster.bandit.captain.name",
                            isPlayer = false,
                            mutations =
                                listOf(
                                    ActorMutationRenderSnapshot(
                                        mutationId = "elite.stonehide",
                                        nameKey = "mutation.stonehide.name",
                                        iconKey = "icon.mutation.stonehide",
                                        audioProfile = "audio.mutation.stonehide",
                                        kindId = "STAT_PACKAGE",
                                        tierId = "MINOR",
                                    ),
                                ),
                            bossVariant =
                                BossVariantRenderSnapshot(
                                    variantId = "boss.variant.grey_crown",
                                    nameKey = "boss.variant.grey_crown.name",
                                    visualTintKey = "vfx.boss.variant.grey_crown",
                                    audioProfile = "audio.boss.variant.grey_crown",
                                ),
                        ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(
            listOf("audio.mutation.stonehide", "audio.boss.variant.grey_crown", "audio.terrain.ice"),
            sink.events,
        )
    }

    @Test
    fun `revealing static terrain does not emit a terrain cue`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous =
            sampleSnapshot().copy(
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.HIDDEN,
                            terrainTypeId = "hidden",
                            terrainVisualKey = "tile.hidden",
                        ),
                    ),
            )
        val current =
            previous.copy(
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 0,
                            y = 1,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "water",
                            terrainVisualKey = "vfx.terrain.interaction.water",
                            terrainAudioProfile = "audio.terrain.water",
                        ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(emptyList<String>(), sink.events)
    }

    @Test
    fun `objective route and victory logs emit dedicated route cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                logEvents =
                    listOf(
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.objective.progress")),
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.route.advance")),
                        RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.victory.escape")),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(
            listOf("audio.objective.progress", "audio.route.transition", "audio.route.complete"),
            sink.events,
        )
    }

    @Test
    fun `energy and positive energy route to dedicated resource cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val roguePrevious =
            sampleSnapshot().copy(
                uiState =
                    sampleSnapshot().uiState.copy(
                        playerStatus =
                            sampleSnapshot().uiState.playerStatus.copy(
                                currentResource = 30,
                                maxResource = 100,
                                resourceLabelKey = "ui.hud.energy.short",
                                resourceTypeId = "ENERGY",
                            ),
                        talents =
                            listOf(
                                TalentSlotSnapshot(
                                    slot = 7,
                                    talentId = "backstab",
                                    nameKey = "talent.rogue.backstab.name",
                                    iconKey = "icon.skill.rogue.backstab",
                                    damageTypeIconKey = "icon.damage_type.physical",
                                    audioProfile = "audio.talent.backstab",
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 12,
                                    resourceLabelKey = "ui.hud.energy.short",
                                    resourceTypeId = "ENERGY",
                                    range = 1,
                                    minRange = 0,
                                    currentCooldown = 0,
                                    maxCooldown = 0,
                                    requiresTarget = true,
                                ),
                            ),
                    ),
            )
        val rogueCurrent =
            roguePrevious.copy(
                uiState = roguePrevious.uiState.copy(playerStatus = roguePrevious.uiState.playerStatus.copy(currentResource = 18)),
                logEvents = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot(key = "log.talent.damage"))),
            )
        val templarPrevious =
            roguePrevious.copy(
                uiState =
                    roguePrevious.uiState.copy(
                        playerStatus =
                            roguePrevious.uiState.playerStatus.copy(
                                currentResource = 10,
                                resourceLabelKey = "ui.hud.positive_energy.short",
                                resourceTypeId = "POSITIVE_ENERGY",
                            ),
                        talents =
                            listOf(
                                TalentSlotSnapshot(
                                    slot = 8,
                                    talentId = "holy_strike",
                                    nameKey = "talent.templar.holy_strike.name",
                                    iconKey = "icon.skill.templar.holy_strike",
                                    damageTypeIconKey = "icon.damage_type.holy",
                                    audioProfile = "audio.talent.holy_strike",
                                    level = 1,
                                    maxLevel = 5,
                                    resourceCost = 10,
                                    resourceLabelKey = "ui.hud.positive_energy.short",
                                    resourceTypeId = "POSITIVE_ENERGY",
                                    range = 1,
                                    minRange = 0,
                                    currentCooldown = 0,
                                    maxCooldown = 0,
                                    requiresTarget = true,
                                ),
                            ),
                    ),
            )
        val templarCurrent =
            templarPrevious.copy(
                uiState = templarPrevious.uiState.copy(playerStatus = templarPrevious.uiState.playerStatus.copy(currentResource = 22)),
            )

        router.onCommandResolved(roguePrevious, rogueCurrent, PlayerCommand.UseTalent(slot = 7), consumed = true)
        router.onSnapshotUpdated(templarPrevious, templarCurrent)

        assertEquals(
            listOf(
                "audio.talent.backstab",
                "audio.resource.energy.spend",
                "audio.damage.physical_hit",
                "audio.resource.positive_energy.restore",
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

    @Test
    fun `new inventory reward item emits its item audio cue once`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "long_sword",
                                            nameKey = "item.long_sword.name",
                                            typeId = "WEAPON",
                                            visualKey = "item.long_sword.visual",
                                            iconKey = "item.long_sword.icon",
                                            audioProfile = "audio.item.long_sword",
                                        ),
                                ),
                            ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)
        router.onSnapshotUpdated(current, current)

        assertEquals(listOf("audio.item.long_sword"), sink.events)
    }

    @Test
    fun `new high value inventory item emits pickup accent after item cue`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "heartroot_gambit",
                                            specialTemplateId = "artifact.heartroot_gambit",
                                            specialTierId = "ARTIFACT",
                                            nameKey = "item.artifact.heartroot_gambit.name",
                                            typeId = "ACCESSORY",
                                            visualKey = "item.artifact.heartroot_gambit.visual",
                                            iconKey = "item.artifact.heartroot_gambit.icon",
                                            audioProfile = "audio.item.artifact.heartroot_gambit",
                                        ),
                                ),
                            ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(listOf("audio.item.artifact.heartroot_gambit", "audio.item.pickup.artifact"), sink.events)
    }

    @Test
    fun `new unique inventory item emits unique pickup accent`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        inventory =
                            listOf(
                                InventoryEntrySnapshot(
                                    index = 0,
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "thornpath_crook",
                                            specialTemplateId = "unique.thornpath_crook",
                                            specialTierId = "UNIQUE",
                                            nameKey = "item.unique.thornpath_crook.name",
                                            typeId = "WEAPON",
                                            visualKey = "item.unique.thornpath_crook.visual",
                                            iconKey = "item.unique.thornpath_crook.icon",
                                            audioProfile = "audio.item.unique.thornpath_crook",
                                        ),
                                ),
                            ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)

        assertEquals(listOf("audio.item.unique.thornpath_crook", "audio.item.pickup.unique"), sink.events)
    }

    @Test
    fun `equipment activation uses changed or rejected item cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                uiState =
                    previous.uiState.copy(
                        equipment =
                            listOf(
                                EquipmentSlotSnapshot(
                                    slotId = "WEAPON",
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "hunter_bow",
                                            nameKey = "item.hunter_bow.name",
                                            typeId = "WEAPON",
                                            visualKey = "item.base.rogue.weapon.icon",
                                            iconKey = "item.base.rogue.weapon.icon",
                                            audioProfile = "audio.item.long_sword",
                                        ),
                                ),
                            ),
                    ),
            )

        router.onCommandResolved(previous, current, PlayerCommand.ActivateInventoryItem(index = 0), consumed = true)
        router.onCommandResolved(previous, previous, PlayerCommand.ActivateInventoryItem(index = 0), consumed = false)

        assertEquals(listOf("audio.item.equip.changed", "audio.item.equip.rejected"), sink.events)
    }

    @Test
    fun `shop purchase success and failure use dedicated cues`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val snapshot = sampleSnapshot()

        router.onCommandResolved(snapshot, snapshot, PlayerCommand.BuyShopOffer(index = 0), consumed = true)
        router.onCommandResolved(snapshot, snapshot, PlayerCommand.SellInventoryItem(index = 0), consumed = true)
        router.onCommandResolved(snapshot, snapshot, PlayerCommand.BuyShopOffer(index = 0), consumed = false)

        assertEquals(
            listOf("audio.shop.purchase_success", "audio.shop.purchase_success", "audio.shop.purchase_failed"),
            sink.events,
        )
    }

    @Test
    fun `new visible ground loot emits its item audio cue once`() {
        val sink = RecordingAudioCueSink()
        val router = AudioRouter(AudioManifestResolver(AudioManifestResourceLoader.load()), sink)
        val previous = sampleSnapshot()
        val current =
            previous.copy(
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 1,
                            y = 0,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tile.floor",
                            items =
                                listOf(
                                    ItemRenderSnapshot(
                                        baseItemId = "battle_axe",
                                        nameKey = "item.battle_axe.name",
                                        typeId = "WEAPON",
                                        visualKey = "item.battle_axe.visual",
                                        iconKey = "item.battle_axe.icon",
                                        audioProfile = "audio.item.battle_axe",
                                    ),
                                ),
                        ),
                    ),
            )

        router.onSnapshotUpdated(previous, current)
        router.onSnapshotUpdated(current, current)

        assertEquals(listOf("audio.item.battle_axe"), sink.events)
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
                                damageTypeIconKey = "icon.damage_type.physical",
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
            logEvents = emptyList(),
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
