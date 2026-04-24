package com.ktome.client.ui.combat

import com.ktome.core.map.Point
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatDecisionPanelTest {
    @Test
    fun `action panel consumes formal combat affordance keys`() {
        val model =
            CombatDecisionPanel.build(
                CombatDecisionPanelRequest(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    snapshot = snapshot(),
                    state = CombatDecisionFrame.initialState,
                    focusIndex = 0,
                    renderText = { token -> token.key },
                ),
            )

        assertEquals(CombatAffordanceResourceKeys.ACTION_ICON, model.phaseIconKey)
        assertEquals(CombatAffordanceResourceKeys.ACTION_CONFIRM_AUDIO, model.confirmAudioCueKey)
        assertTrue(model.rows.any { row -> row.iconKey == CombatAffordanceResourceKeys.ACTION_ICON })
    }

    @Test
    fun `action panel renders missing fact instead of global legal target count`() {
        val model =
            CombatDecisionPanel.build(
                CombatDecisionPanelRequest(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    snapshot = snapshot(targetablePositions = emptyList()),
                    state = CombatDecisionFrame.initialState,
                    focusIndex = 0,
                    renderText = { token -> token.key },
                ),
            )

        assertTrue(model.rows.first().text.contains("ui.combat.fact.missing"))
        assertTrue(model.rows.first().enabled)
    }

    @Test
    fun `action panel keeps inscription cost and range as missing facts`() {
        val model =
            CombatDecisionPanel.build(
                CombatDecisionPanelRequest(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    snapshot =
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
                        ),
                    state = CombatDecisionFrame.initialState,
                    focusIndex = 0,
                    renderText = { token -> token.key },
                ),
            )

        assertTrue(model.rows.first().text.contains("ui.combat.fact.missing"))
        assertFalse(model.rows.first().text.contains("0"))
        assertFalse(model.rows.first().text.contains("none", ignoreCase = true))
    }

    @Test
    fun `target panel keeps free cursor inscriptions out of no legal target state`() {
        val model =
            CombatDecisionPanel.build(
                CombatDecisionPanelRequest(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    snapshot =
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
                        ),
                    state =
                        CombatDecisionFrameState(
                            phase = CombatDecisionPhase.TARGET,
                            selectedActionId = "inscription:5",
                            selectedMethodId = "default",
                            skippedMethod = true,
                        ),
                    focusIndex = 0,
                    targetCursor = Point(3, 1),
                    renderText = { token -> token.key },
                ),
            )

        assertEquals(1, model.rows.size)
        assertEquals(CombatAffordanceResourceKeys.LOCK_ICON, model.rows.single().iconKey)
        assertTrue(model.rows.single().selected)
        assertTrue(model.rows.single().enabled)
        assertFalse(model.rows.single().text.contains(CombatDecisionFeedbackKeys.NO_LEGAL_TARGET))
    }

    private fun snapshot(
        targetablePositions: List<GridPointSnapshot> = listOf(GridPointSnapshot(2, 1)),
        talents: List<TalentSlotSnapshot> =
            listOf(
                TalentSlotSnapshot(
                    slot = 1,
                    talentId = "power_strike",
                    nameKey = "talent.vanguard.power_strike.name",
                    iconKey = CombatAffordanceResourceKeys.ACTION_ICON,
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
