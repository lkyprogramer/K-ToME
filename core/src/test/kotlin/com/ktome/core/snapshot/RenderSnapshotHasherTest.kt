package com.ktome.core.snapshot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RenderSnapshotHasherTest {
    @Test
    fun `same snapshot yields stable sha256`() {
        val snapshot = sampleSnapshot(revision = 3L)

        val first = RenderSnapshotHasher.sha256(snapshot)
        val second = RenderSnapshotHasher.sha256(snapshot)

        assertEquals(first, second)
    }

    @Test
    fun `different revisions change canonical hash`() {
        val before = sampleSnapshot(revision = 3L)
        val after = sampleSnapshot(revision = 4L)

        assertNotEquals(RenderSnapshotHasher.sha256(before), RenderSnapshotHasher.sha256(after))
    }

    private fun sampleSnapshot(revision: Long): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = revision,
                    zoneId = "shattered_outpost",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = 2,
                    height = 2,
                    playerX = 0,
                    playerY = 0,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.foundation.ascii",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                listOf(
                    MapCellSnapshot(
                        x = 0,
                        y = 0,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.foundation.ascii.floor",
                    ),
                    MapCellSnapshot(
                        x = 1,
                        y = 0,
                        visibility = CellVisibilitySnapshot.EXPLORED,
                        terrainTypeId = "wall",
                        terrainVisualKey = "tileset.foundation.ascii.wall",
                        stairDirectionId = "DOWN",
                    ),
                ),
            props =
                listOf(
                    PropRenderSnapshot(
                        id = "stair:down:9",
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
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 20,
                            maxHp = 20,
                            currentResource = 10,
                            maxResource = 10,
                            resourceLabelKey = "ui.hud.stamina.short",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 10,
                            statPoints = 0,
                            talentPoints = 1,
                            attack = 5,
                            defense = 3,
                            accuracy = 6,
                            evasion = 4,
                            speed = 100,
                        ),
                    equipment =
                        listOf(
                            EquipmentSlotSnapshot(
                                slotId = "WEAPON",
                                item =
                                    ItemRenderSnapshot(
                                        baseItemId = "short_sword",
                                        nameKey = "item.short_sword.name",
                                        typeId = "WEAPON",
                                        slotId = "WEAPON",
                                        stats = ItemStatModifierSnapshot(attack = 3),
                                    ),
                            ),
                        ),
                    talents = emptyList(),
                    inventory = emptyList(),
                    targetablePositions = listOf(GridPointSnapshot(1, 0)),
                ),
            logEvents = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.enter_dungeon"))),
        )
}
