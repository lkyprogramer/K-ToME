package com.ktome.client.ui.combat

import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActionHintModelBuilderTest {
    @Test
    fun `known targeted action exposes typed facts but keeps legal target count unknown`() {
        val snapshot = snapshot(targetablePositions = listOf(GridPointSnapshot(2, 1)))

        val hint = ActionHintModelBuilder.build(snapshot, "talent:1")

        assertEquals(ActionAvailability.AVAILABLE, hint.availability)
        assertEquals(1, hint.resourceCosts.size)
        assertEquals(0, hint.cooldownTurns)
        assertNull(hint.legalTargetSummary.count)
        assertEquals("ui.combat.fact.missing", hint.legalTargetSummary.missingReason?.key)
        assertEquals("ui.combat.range.line", hint.rangeSummary?.key)
    }

    @Test
    fun `targeted action does not infer zero legal targets from the global target list`() {
        val snapshot = snapshot(targetablePositions = emptyList())

        val hint = ActionHintModelBuilder.build(snapshot, "talent:1")

        assertEquals(ActionAvailability.AVAILABLE, hint.availability)
        assertNull(hint.legalTargetSummary.count)
        assertEquals("ui.combat.fact.missing", hint.legalTargetSummary.missingReason?.key)
        assertNull(hint.disabledReason)
    }

    @Test
    fun `missing action returns missing fact reason without reading hidden rule state`() {
        val hint = ActionHintModelBuilder.build(snapshot(), "talent:missing")

        assertEquals(ActionAvailability.UNKNOWN, hint.availability)
        assertEquals("ui.combat.fact.missing", hint.missingFactReason?.key)
        assertEquals("ui.combat.fact.missing", hint.legalTargetSummary.missingReason?.key)
    }

    @Test
    fun `real zero cost does not become missing fact`() {
        val snapshot = snapshot(resourceCost = 0, targetablePositions = listOf(GridPointSnapshot(2, 1)))

        val hint = ActionHintModelBuilder.build(snapshot, "talent:1")

        assertTrue(hint.resourceCosts.isNotEmpty())
        assertNull(hint.missingFactReason)
    }

    @Test
    fun `inscription cost and range stay missing instead of rendering as zero none`() {
        val snapshot =
            snapshot(
                talents = emptyList(),
                inscriptions =
                    listOf(
                        InscriptionSlotSnapshot(
                            hotkey = 5,
                            inscriptionId = "phase_door",
                            nameKey = "inscription.phase_door.name",
                            descKey = "inscription.phase_door.desc",
                            iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
                            categoryId = "MOVEMENT",
                            cooldownRemaining = 0,
                            maxCooldown = 10,
                            requiresTarget = true,
                        ),
                    ),
            )

        val hint = ActionHintModelBuilder.build(snapshot, "inscription:5")

        assertTrue(hint.resourceCosts.isEmpty())
        assertNull(hint.rangeSummary)
        assertEquals("ui.combat.fact.missing", hint.missingFactReason?.key)
        assertEquals("ui.combat.fact.missing", hint.legalTargetSummary.missingReason?.key)
    }

    @Test
    fun `boss telegraph overlay does not inject missing fact into unrelated player action`() {
        val snapshot =
            snapshot(
                overlays =
                    listOf(
                        OverlayRenderSnapshot(
                            id = "telegraph:boss-cleave",
                            visualKey = "telegraph.boss.cleave",
                            previewTurns = 1,
                            dangerLevel = 3,
                            shape = OverlayShapeSnapshot.SINGLE_TILE,
                            sourceAbilityId = "boss.cleave",
                            cells = listOf(GridPointSnapshot(2, 1)),
                        ),
                    ),
            )

        val hint = ActionHintModelBuilder.build(snapshot, "talent:1")

        assertNull(hint.telegraphLinkage)
        assertNull(hint.missingFactReason)
    }

    private fun snapshot(
        resourceCost: Int = 3,
        targetablePositions: List<GridPointSnapshot> = listOf(GridPointSnapshot(2, 1)),
        overlays: List<OverlayRenderSnapshot> = emptyList(),
        talents: List<TalentSlotSnapshot> =
            listOf(
                TalentSlotSnapshot(
                    slot = 1,
                    talentId = "power_strike",
                    nameKey = "talent.vanguard.power_strike.name",
                    level = 1,
                    maxLevel = 5,
                    resourceCost = resourceCost,
                    resourceLabelKey = "ui.hud.stamina.short",
                    range = 3,
                    minRange = 1,
                    currentCooldown = 0,
                    maxCooldown = 2,
                    requiresTarget = true,
                ),
            ),
        inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
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
            overlays = overlays,
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 10,
                            maxHp = 10,
                            currentResource = 5,
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
                    talents = talents,
                    inscriptions = inscriptions,
                    inventory = emptyList(),
                    targetablePositions = targetablePositions,
                ),
        )
}
