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

    @Test
    fun `different zone description and reserve talent fields change canonical hash`() {
        val before = sampleSnapshot(revision = 3L)
        val after =
            before.copy(
                metadata = before.metadata.copy(zoneDescKey = "zone.grey_gate_depths.desc"),
                uiState =
                    before.uiState.copy(
                        reserveTalents =
                            before.uiState.reserveTalents.map { reserve ->
                                reserve.copy(descKey = "talent.vanguard.charge.desc.alt")
                            },
                    ),
            )

        assertNotEquals(RenderSnapshotHasher.sha256(before), RenderSnapshotHasher.sha256(after))
    }

    @Test
    fun `frontstage action cues participate in canonical hash`() {
        val before = sampleSnapshot(revision = 3L)
        val after =
            before.copy(
                uiState =
                    before.uiState.copy(
                        frontstageReadability =
                            FrontstageReadabilitySnapshot(
                                recentActionCues =
                                    listOf(
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SEARCH,
                                            priority = FrontstageActionPrioritySnapshot.HIGH,
                                            stableKey = "search:deep_iron:failed_check",
                                            message = RenderTextTokenSnapshot("log.search.failed_check"),
                                        ),
                                    ),
                            ),
                    ),
            )

        assertNotEquals(RenderSnapshotHasher.sha256(before), RenderSnapshotHasher.sha256(after))
    }

    @Test
    fun `item special tier participates in canonical hash`() {
        val before = sampleSnapshot(revision = 3L)
        val after =
            before.copy(
                uiState =
                    before.uiState.copy(
                        equipment =
                            listOf(
                                EquipmentSlotSnapshot(
                                    slotId = "WEAPON",
                                    item =
                                        ItemRenderSnapshot(
                                            baseItemId = "thornpath_crook",
                                            specialTemplateId = "unique.thornpath_crook",
                                            specialTierId = "UNIQUE",
                                            nameKey = "item.unique.thornpath_crook.name",
                                            typeId = "WEAPON",
                                            slotId = "WEAPON",
                                            iconKey = "item.unique.thornpath_crook.icon",
                                        ),
                                ),
                            ),
                    ),
            )

        assertNotEquals(RenderSnapshotHasher.sha256(before), RenderSnapshotHasher.sha256(after))
    }

    private fun sampleSnapshot(revision: Long): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = revision,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    zoneDescKey = "zone.shattered_outpost.desc",
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
                                descKey = "item.short_sword.desc",
                            ),
                            ),
                        ),
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                iconKey = "icon.skill.vanguard.power_strike",
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 5,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 1,
                                minRange = 1,
                                currentCooldown = 0,
                                maxCooldown = 3,
                                requiresTarget = true,
                                descKey = "talent.vanguard.power_strike.desc",
                            ),
                        ),
                    reserveTalents =
                        listOf(
                            TalentReserveSnapshot(
                                talentId = "charge",
                                nameKey = "talent.vanguard.charge.name",
                                iconKey = "icon.skill.vanguard.charge",
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 8,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 4,
                                minRange = 2,
                                currentCooldown = 0,
                                maxCooldown = 6,
                                requiresTarget = true,
                                descKey = "talent.vanguard.charge.desc",
                            ),
                        ),
                    inventory = emptyList(),
                    targetablePositions = listOf(GridPointSnapshot(1, 0)),
                ),
            logEvents = listOf(RenderLogEventSnapshot(RenderTextTokenSnapshot("log.session.enter_dungeon"))),
        )
}
