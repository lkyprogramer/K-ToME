package com.ktome.core.snapshot

import com.ktome.core.talent.TalentCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
                            items =
                                listOf(
                                    ItemRenderSnapshot(
                                        baseItemId = "thornpath_crook",
                                        specialTemplateId = "unique.thornpath_crook",
                                        specialTierId = "UNIQUE",
                                        nameKey = "item.unique.thornpath_crook.name",
                                        typeId = "WEAPON",
                                        iconKey = "item.unique.thornpath_crook.icon",
                                    ),
                                ),
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
                        talentTrees =
                            listOf(
                                TalentTreeSnapshot(
                                    treeId = "vanguard_warcry",
                                    treeOwnerId = "vanguard",
                                    nameKey = "talent_tree.vanguard_warcry.name",
                                    descKey = "talent_tree.vanguard_warcry.desc",
                                    iconKey = "icon.tree.vanguard_warcry",
                                    nodes =
                                        listOf(
                                            TalentTreeNodeSnapshot(
                                                talentId = "war_cry",
                                                treeId = "vanguard_warcry",
                                                treeOwnerId = "vanguard",
                                                nameKey = "talent.vanguard.war_cry.name",
                                                descKey = "talent.vanguard.war_cry.desc",
                                                iconKey = "icon.skill.vanguard.war_cry",
                                                category = TalentCategory.SUSTAINED,
                                                state = TalentNodeStateSnapshot.LOCKED,
                                                rank = 0,
                                                maxRank = 5,
                                                unlockLevel = 2,
                                                resourceCost = 12,
                                                resourceLabelKey = "ui.hud.stamina.short",
                                                range = 0,
                                                minRange = 0,
                                                currentCooldown = 0,
                                                maxCooldown = 6,
                                                requiresTarget = false,
                                                descriptionModel =
                                                    DescriptionModelSnapshot(
                                                        templateKey = "talent.vanguard.war_cry.desc",
                                                        placeholders = mapOf("rank" to DescriptionValueSnapshot.IntValue(1)),
                                                        keywords = listOf("buff"),
                                                    ),
                                                nextBreakpointPreview =
                                                    TalentBreakpointPreviewSnapshot(
                                                        atRank = 3,
                                                        descriptionAddendumKey = "talent.vanguard.war_cry.breakpoint",
                                                        model = DescriptionModelSnapshot("talent.vanguard.war_cry.breakpoint"),
                                                    ),
                                                lockReasons =
                                                    listOf(
                                                        TalentNodeLockReasonSnapshot(
                                                            type = TalentNodeLockReasonTypeSnapshot.TREE_INVESTMENT,
                                                            messageKey = "ui.talent.tree.lock.tree_investment",
                                                            treeId = "vanguard_warcry",
                                                            treeNameKey = "talent_tree.vanguard_warcry.name",
                                                            requiredPoints = 2,
                                                            currentPoints = 0,
                                                        ),
                                                    ),
                                            ),
                                        ),
                                ),
                            ),
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
        assertTrue(encoded.contains("\"specialTierId\": \"UNIQUE\""))
        assertTrue(encoded.contains("\"talentTrees\""))
        assertTrue(encoded.contains("\"category\": \"SUSTAINED\""))
        assertTrue(encoded.contains("\"TREE_INVESTMENT\""))
        assertTrue(encoded.contains("\"talent.vanguard.war_cry.breakpoint\""))
        assertEquals(snapshot, decoded)
    }

    @Test
    fun `special item snapshot requires special template and tier together`() {
        assertThrows(IllegalArgumentException::class.java) {
            ItemRenderSnapshot(
                baseItemId = "thornpath_crook",
                specialTemplateId = "unique.thornpath_crook",
                nameKey = "item.unique.thornpath_crook.name",
                typeId = "WEAPON",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItemRenderSnapshot(
                baseItemId = "thornpath_crook",
                specialTierId = "UNIQUE",
                nameKey = "item.unique.thornpath_crook.name",
                typeId = "WEAPON",
            )
        }
    }
}
