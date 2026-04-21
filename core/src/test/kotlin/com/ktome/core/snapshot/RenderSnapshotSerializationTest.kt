package com.ktome.core.snapshot

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RenderSnapshotSerializationTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `combat feedback events round trip through render snapshot serialization`() {
        val snapshot =
            RenderSnapshot(
                metadata =
                    RenderMetadataSnapshot(
                        revision = 7L,
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
                        frontstageReadability =
                            FrontstageReadabilitySnapshot(
                                recentActionCues =
                                    listOf(
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SEARCH,
                                            priority = FrontstageActionPrioritySnapshot.CRITICAL,
                                            stableKey = "search:deep_iron:revealed",
                                            message = RenderTextTokenSnapshot("log.search.revealed"),
                                        ),
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.SECRET,
                                            priority = FrontstageActionPrioritySnapshot.HIGH,
                                            stableKey = "secret:primer:deep_iron",
                                            message = RenderTextTokenSnapshot("log.hidden.primer.acquired"),
                                        ),
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.PASSIVE,
                                            priority = FrontstageActionPrioritySnapshot.MEDIUM,
                                            stableKey = "passive:on_hit_status:item",
                                            message = RenderTextTokenSnapshot("log.passive.on_hit_status"),
                                        ),
                                        FrontstageActionCueSnapshot(
                                            category = FrontstageActionCategorySnapshot.PASSIVE,
                                            priority = FrontstageActionPrioritySnapshot.LOW,
                                            stableKey = "passive:hp_regen:item",
                                            message = RenderTextTokenSnapshot("log.passive.hp_regen"),
                                        ),
                                    ),
                            ),
                        targetablePositions = emptyList(),
                    ),
                combatFeedbackEvents =
                    listOf(
                        CombatFeedbackSnapshot(
                            targetEntityId = 11,
                            sourceEntityId = 1,
                            x = 4,
                            y = 5,
                            type = CombatFeedbackTypeSnapshot.DAMAGE,
                            amount = 18,
                            damageTypeId = "FIRE",
                            critical = true,
                        ),
                        CombatFeedbackSnapshot(
                            targetEntityId = 11,
                            x = 4,
                            y = 5,
                            type = CombatFeedbackTypeSnapshot.STATUS_REMOVED,
                            statusNameKey = "status.stealth",
                        ),
                    ),
            )

        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<RenderSnapshot>(encoded)

        assertTrue(encoded.contains("\"combatFeedbackEvents\""))
        assertTrue(encoded.contains("\"critical\": true"))
        assertTrue(encoded.contains("\"recentActionCues\""))
        assertTrue(encoded.contains("\"stableKey\": \"search:deep_iron:revealed\""))
        assertTrue(encoded.contains("\"priority\": \"CRITICAL\""))
        assertEquals(snapshot, decoded)
    }
}
