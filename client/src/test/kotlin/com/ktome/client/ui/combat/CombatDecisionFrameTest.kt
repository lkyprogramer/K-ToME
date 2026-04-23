package com.ktome.client.ui.combat

import com.ktome.core.map.Point
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.game.PlayerCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatDecisionFrameTest {
    @Test
    fun `available actions expose existing snapshot facts without hidden rule lookup`() {
        val snapshot = snapshot(resource = 5, targetablePositions = listOf(GridPointSnapshot(3, 1)))

        val action = CombatDecisionFrame.availableActions(snapshot).single()
        val targets = CombatDecisionFrame.legalTargets(snapshot, action)

        assertEquals("talent:1", action.id)
        assertEquals("power_strike", action.sourceAbilityId)
        assertEquals(3, action.resourceCost)
        assertEquals(1, targets.size)
        assertEquals(Point(3, 1), targets.single().point)
        assertFalse(CombatDecisionFrame.isActionDisabled(snapshot, action))
        assertEquals(PlayerCommand.UseTalent(slot = 1, target = Point(3, 1)), action.command(Point(3, 1)))
    }

    @Test
    fun `actions do not use global target list emptiness as action disabled reason`() {
        val snapshot = snapshot(resource = 5, targetablePositions = emptyList())
        val action = CombatDecisionFrame.availableActions(snapshot).single()

        assertFalse(CombatDecisionFrame.isActionDisabled(snapshot, action))
        assertTrue(CombatDecisionFrame.legalTargets(snapshot, action).isEmpty())
        assertEquals(null, CombatDecisionFrame.disabledReasonKey(snapshot, action))
    }

    @Test
    fun `resource shortage is represented as a disabled reason from ui state only`() {
        val snapshot = snapshot(resource = 1, targetablePositions = listOf(GridPointSnapshot(3, 1)))
        val action = CombatDecisionFrame.availableActions(snapshot).single()

        assertTrue(CombatDecisionFrame.isActionDisabled(snapshot, action))
        assertEquals("ui.combat.disabled.resource", CombatDecisionFrame.disabledReasonKey(snapshot, action))
    }

    private fun snapshot(
        resource: Int,
        targetablePositions: List<GridPointSnapshot>,
    ): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "test",
                    zoneNameKey = "zone.shattered_outpost.name",
                    currentFloor = 1,
                    maxFloor = 1,
                    width = 4,
                    height = 4,
                    playerX = 1,
                    playerY = 1,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.test",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                listOf(
                    MapCellSnapshot(
                        x = 1,
                        y = 1,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.test.ground_01",
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 10,
                            maxHp = 10,
                            currentResource = resource,
                            maxResource = 5,
                            resourceLabelKey = "ui.hud.stamina.short",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 10,
                            statPoints = 0,
                            talentPoints = 0,
                            attack = 1,
                            defense = 1,
                            accuracy = 1,
                            evasion = 1,
                            speed = 100,
                        ),
                    equipment = emptyList(),
                    talents =
                        listOf(
                            TalentSlotSnapshot(
                                slot = 1,
                                talentId = "power_strike",
                                nameKey = "talent.vanguard.power_strike.name",
                                level = 1,
                                maxLevel = 5,
                                resourceCost = 3,
                                resourceLabelKey = "ui.hud.stamina.short",
                                range = 3,
                                minRange = 1,
                                currentCooldown = 0,
                                maxCooldown = 2,
                                requiresTarget = true,
                            ),
                        ),
                    inventory = emptyList(),
                    targetablePositions = targetablePositions,
                ),
        )
}
