package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.replay.ReplayInputSource
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.validationSessionOptionsForPreset
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class InputHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `validation overlay only opens for enabled input handlers`() {
        val validationSession =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-open-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )
        val standardSession = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("standard-overlay-open-save")))
        val disabledInput = ReplayInputSource()
        val disabledHandler = InputHandler(disabledInput)

        disabledInput.frame(justPressed = setOf(Keys.F9))
        assertNull(disabledHandler.pollCommand(standardSession.renderSnapshot()))
        assertEquals(UiMode.MAP, disabledHandler.overlayState().mode)
        disabledInput.clear()

        val enabledInput = ReplayInputSource()
        val enabledHandler = InputHandler(enabledInput, ValidationOverlayAvailability.ENABLED)
        enabledInput.frame(justPressed = setOf(Keys.F9))
        assertNull(enabledHandler.pollCommand(validationSession.renderSnapshot()))
        assertEquals(UiMode.VALIDATION, enabledHandler.overlayState().mode)
        assertEquals(ValidationOverlaySection.RESTART, enabledHandler.overlayState().validationCursor?.selectedSection)
    }

    @Test
    fun `validation overlay captures navigation keys and returns map movement after close`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-navigation-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        assertEquals(ValidationOverlaySection.TRAVEL, handler.overlayState().validationCursor?.selectedSection)
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        assertEquals(ValidationOverlaySection.RESTART, handler.overlayState().validationCursor?.selectedSection)
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.S))
        assertEquals(PlayerCommand.Move(Point(0, 1)), handler.pollCommand(session.renderSnapshot()))
    }

    @Test
    fun `validation overlay navigation emits typed validation commands`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-command-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(ValidationOverlaySection.TRAVEL, handler.overlayState().validationCursor?.selectedSection)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(
            PlayerCommand.Validation(com.ktome.game.validation.ValidationAction.TravelToStair(com.ktome.core.dungeon.StairDirection.DOWN)),
            handler.pollCommand(session.renderSnapshot()),
        )
        assertTrue(handler.overlayState().validationCursor != null)
        assertFalse(handler.isMapMode())
    }

    @Test
    fun `single seed validation overlays keep restart on same preset`() {
        val input = ReplayInputSource()
        val handler =
            InputHandler(
                input = input,
                validationOverlayAvailability = ValidationOverlayAvailability.ENABLED,
                validationPreset = ValidationPreset.HIDDEN_CONTENT,
                validationRestartNextSeedEnabled = false,
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-single-seed-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.HIDDEN_CONTENT),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.RIGHT))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(
            PlayerCommand.Validation(com.ktome.game.validation.ValidationAction.RestartSamePreset),
            handler.pollCommand(session.renderSnapshot()),
        )
    }

    @Test
    fun `validation overlay action descriptors stay aligned with dispatch`() {
        val inspectCursor = Point(4, 7)
        val cases =
            listOf(
                ValidationPreset.MAPGEN_DIFF to true,
                ValidationPreset.HIDDEN_CONTENT to false,
            )

        cases.forEach { (preset, restartNextSeedEnabled) ->
            ValidationOverlaySection.entries.forEach { section ->
                val descriptors =
                    validationOverlayActionDescriptors(
                        scope =
                            ValidationOverlayDescriptorScope(
                                preset = preset,
                                restartMode =
                                    if (restartNextSeedEnabled) {
                                        ValidationOverlayRestartMode.NEXT_SEED_ENABLED
                                    } else {
                                        ValidationOverlayRestartMode.SAME_PRESET_ONLY
                                    },
                            ),
                        section = section,
                    )
                assertTrue(descriptors.isNotEmpty(), "Expected actions for $preset / $section")
                descriptors.forEachIndexed { index, descriptor ->
                    assertEquals(
                        descriptor.buildAction(inspectCursor),
                        validationOverlayAction(
                            ValidationOverlaySelection(
                                preset = preset,
                                restartNextSeedEnabled = restartNextSeedEnabled,
                                section = section,
                                index = index,
                                inspectCursor = inspectCursor,
                            ),
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun `x enters inspect mode and cursor movement stays UI only`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("inspect-mode-save")))
        val playerStart = session.playerPosition()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        assertEquals(playerStart, handler.overlayState().inspectCursor)
        input.clear()

        val start = requireNotNull(handler.overlayState().inspectCursor)
        input.frame(justPressed = setOf(Keys.S))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(
            Point(start.x, (start.y + 1).coerceAtMost(session.map.height - 1)),
            handler.overlayState().inspectCursor,
        )
        assertEquals(playerStart, session.playerPosition())
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `inspect mode remains available while stat points are pending`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("pending-stat-save")))
        requireNotNull(runtimeWorld(session).get<Experience>(session.playerId)).unspentStatPoints = 1

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        input.clear()

        input.frame()
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)

        input.frame()
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.STAT_ASSIGN, handler.overlayState().mode)
    }

    @Test
    fun `map mode keeps south movement on s while control s remains save`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("movement-save")))

        input.frame(justPressed = setOf(Keys.S))
        assertEquals(PlayerCommand.Move(Point(0, 1)), handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertEquals(PlayerCommand.SaveGame, handler.pollCommand(session.renderSnapshot()))
    }

    @Test
    fun `r triggers search only in map mode`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout()

        input.frame(justPressed = setOf(Keys.R))
        assertEquals(PlayerCommand.Search, handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.R))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
    }

    @Test
    fun `controlled inscription hotkey enters targeting mode and confirms targeted use`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout(inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)))

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(5, handler.overlayState().targetingInscriptionHotkey)
        assertEquals(Point(3, 3), handler.overlayState().targetingCursor)
        input.clear()

        input.frame(justPressed = setOf(Keys.RIGHT))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(Point(4, 3), handler.overlayState().targetingCursor)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.UseInscription(hotkey = 5, target = Point(4, 3)), handler.pollCommand(snapshot))
    }

    @Test
    fun `holding a movement key repeats move after a short delay`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("held-movement-save")))
        val snapshot = session.renderSnapshot()

        input.frame(justPressed = setOf(Keys.RIGHT), pressed = setOf(Keys.RIGHT))
        assertEquals(PlayerCommand.Move(Point(1, 0)), handler.pollCommand(snapshot))

        repeat(11) {
            input.frame(pressed = setOf(Keys.RIGHT))
            assertNull(handler.pollCommand(snapshot))
        }

        input.frame(pressed = setOf(Keys.RIGHT))
        assertEquals(PlayerCommand.Move(Point(1, 0)), handler.pollCommand(snapshot))

        repeat(2) {
            input.frame(pressed = setOf(Keys.RIGHT))
            assertNull(handler.pollCommand(snapshot))
        }

        input.frame(pressed = setOf(Keys.RIGHT))
        assertEquals(PlayerCommand.Move(Point(1, 0)), handler.pollCommand(snapshot))

        input.clear()
        input.frame()
        assertNull(handler.pollCommand(snapshot))
    }

    @Test
    fun `l enters loadout edit and enter equips selected reserve talent`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                reserveTalents =
                    listOf(
                        reserveTalent("charge", "talent.vanguard.charge.name"),
                        reserveTalent("sweeping_strike", "talent.vanguard.sweeping_strike.name"),
                    ),
            )

        input.frame(justPressed = setOf(Keys.L))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.LOADOUT_EDIT, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.NUM_4))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(4, handler.overlayState().loadoutSlotSelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(1, handler.overlayState().loadoutReserveSelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.EquipTalentToSlot(slot = 4, talentId = "sweeping_strike"), handler.pollCommand(snapshot))
    }

    @Test
    fun `loadout edit remains available while stat points are pending`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout(statPoints = 1, reserveTalents = listOf(reserveTalent("charge", "talent.vanguard.charge.name")))

        input.frame(justPressed = setOf(Keys.L))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.LOADOUT_EDIT, handler.overlayState().mode)
        input.clear()

        input.frame()
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.LOADOUT_EDIT, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.F))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)

        input.frame()
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.STAT_ASSIGN, handler.overlayState().mode)
    }

    @Test
    fun `talent assign mode maps confirm rollback and respec commands`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout(talentPoints = 2, reserveTalents = listOf(reserveTalent("charge", "talent.vanguard.charge.name")))

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.ConfirmTalentDraft, handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertEquals(PlayerCommand.RollbackTalentDraft, handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.R))
        assertEquals(
            PlayerCommand.RespecTalentTree(
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = "vanguard",
            ),
            handler.pollCommand(snapshot),
        )
    }

    @Test
    fun `inventory mode maps drop command and closes with f instead of escape`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = ItemRenderSnapshot(baseItemId = "long_sword", nameKey = "item.long_sword.name", typeId = "WEAPON"),
                        ),
                        InventoryEntrySnapshot(
                            index = 1,
                            item = ItemRenderSnapshot(baseItemId = "healing_potion", nameKey = "item.healing_potion.name", typeId = "CONSUMABLE"),
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.D))
        assertEquals(PlayerCommand.DropInventoryItem(0), handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.F))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `talent assign respec follows focused reserve tree owner`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 2,
                reserveTalents =
                    listOf(
                        reserveTalent(
                            talentId = "moon_blessing",
                            nameKey = "talent.shalore.moon_blessing.name",
                            ownerType = TalentTreeOwnerType.RACE,
                            treeOwnerId = "shalore",
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.R))
        assertEquals(
            PlayerCommand.RespecTalentTree(
                ownerType = TalentTreeOwnerType.RACE,
                treeOwnerId = "shalore",
            ),
            handler.pollCommand(snapshot),
        )
    }

    @Test
    fun `talent assign mode can invest selected reserve talent directly`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 2,
                reserveTalents =
                    listOf(
                        reserveTalent("charge", "talent.vanguard.charge.name"),
                        reserveTalent("sweeping_strike", "talent.vanguard.sweeping_strike.name"),
                    ),
            )

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(1, handler.overlayState().loadoutReserveSelection)
        assertEquals(TalentAssignFocus.RESERVE, handler.overlayState().talentAssignFocus)
        input.clear()

        input.frame(justPressed = setOf(Keys.E))
        assertEquals(PlayerCommand.AssignTalent("sweeping_strike"), handler.pollCommand(snapshot))
    }

    @Test
    fun `pending reserve allocation keeps talent assign mode available`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                reserveTalents =
                    listOf(
                        reserveTalent(
                            talentId = "charge",
                            nameKey = "talent.vanguard.charge.name",
                            hasPendingAllocation = true,
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
        input.clear()

        input.frame()
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
    }

    @Test
    fun `enter on stairs triggers floor transition command`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            RenderSnapshot(
                metadata =
                    RenderMetadataSnapshot(
                        revision = 1,
                        zoneId = "shattered_outpost",
                        zoneNameKey = "zone.shattered_outpost.name",
                        currentFloor = 1,
                        maxFloor = 2,
                        width = 8,
                        height = 8,
                        playerX = 3,
                        playerY = 3,
                        zoneVisualKey = "zone.shattered_outpost.visual",
                        zoneAudioProfile = "audio.zone.shattered_outpost",
                        tilesetKey = "tileset.ruins",
                        ambientProfile = "ambient.shattered_outpost",
                    ),
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 3,
                            y = 3,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.ruins.ground_01",
                            stairDirectionId = "DOWN",
                        ),
                    ),
                props =
                    listOf(
                        PropRenderSnapshot(
                            id = "stair:down:1",
                            x = 3,
                            y = 3,
                            propTypeId = "stairs",
                            stairDirectionId = "DOWN",
                            visualKey = "prop.stairs.down",
                            audioProfile = "audio.interactable.stairs",
                        ),
                    ),
                uiState =
                    RenderUiStateSnapshot(
                        playerStatus =
                            PlayerStatusSnapshot(
                                currentHp = 12,
                                maxHp = 12,
                                currentResource = 8,
                                maxResource = 8,
                                resourceLabelKey = "ui.hud.stamina.short",
                                resourceTypeId = "STAMINA",
                                level = 1,
                                currentExperience = 0,
                                nextLevelRequirement = 12,
                                statPoints = 0,
                                talentPoints = 0,
                                attack = 4,
                                defense = 2,
                                accuracy = 3,
                                evasion = 2,
                                speed = 100,
                            ),
                        equipment = emptyList(),
                        talents = emptyList(),
                        inventory = emptyList(),
                        targetablePositions = emptyList(),
                    ),
            )

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.Descend, handler.pollCommand(snapshot))
        input.clear()
    }

    @Test
    fun `enter on interactable tile triggers interact command before stairs fallback`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            RenderSnapshot(
                metadata =
                    RenderMetadataSnapshot(
                        revision = 1,
                        zoneId = "shattered_outpost",
                        zoneNameKey = "zone.shattered_outpost.name",
                        currentFloor = 1,
                        maxFloor = 2,
                        width = 8,
                        height = 8,
                        playerX = 3,
                        playerY = 3,
                        zoneVisualKey = "zone.shattered_outpost.visual",
                        zoneAudioProfile = "audio.zone.shattered_outpost",
                        tilesetKey = "tileset.ruins",
                        ambientProfile = "ambient.shattered_outpost",
                    ),
                mapCells =
                    listOf(
                        MapCellSnapshot(
                            x = 3,
                            y = 3,
                            visibility = CellVisibilitySnapshot.VISIBLE,
                            terrainTypeId = "floor",
                            terrainVisualKey = "tileset.ruins.ground_01",
                            stairDirectionId = "DOWN",
                        ),
                    ),
                props =
                    listOf(
                        PropRenderSnapshot(
                            id = "interactable:supply_crate:1",
                            x = 3,
                            y = 3,
                            propTypeId = "supply_crate",
                            visualKey = "prop.supply_crate",
                            audioProfile = "audio.interactable.open",
                        ),
                    ),
                uiState =
                    RenderUiStateSnapshot(
                        playerStatus =
                            PlayerStatusSnapshot(
                                currentHp = 12,
                                maxHp = 12,
                                currentResource = 8,
                                maxResource = 8,
                                resourceLabelKey = "ui.hud.stamina.short",
                                resourceTypeId = "STAMINA",
                                level = 1,
                                currentExperience = 0,
                                nextLevelRequirement = 12,
                                statPoints = 0,
                                talentPoints = 0,
                                attack = 4,
                                defense = 2,
                                accuracy = 3,
                                evasion = 2,
                                speed = 100,
                            ),
                        equipment = emptyList(),
                        talents = emptyList(),
                        inventory = emptyList(),
                        targetablePositions = emptyList(),
                    ),
            )

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.Interact, handler.pollCommand(snapshot))
        input.clear()
    }

    private fun runtimeWorld(session: com.ktome.game.FoundationGameSession): World {
        val field = com.ktome.game.FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }

    private fun snapshotWithLoadout(
        statPoints: Int = 0,
        talentPoints: Int = 0,
        reserveTalents: List<TalentReserveSnapshot> = emptyList(),
        inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
    ): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = 8,
                    height = 8,
                    playerX = 3,
                    playerY = 3,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.ruins",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                listOf(
                    MapCellSnapshot(
                        x = 3,
                        y = 3,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.ruins.ground_01",
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 12,
                            maxHp = 12,
                            currentResource = 8,
                            maxResource = 8,
                            resourceLabelKey = "ui.hud.stamina.short",
                            resourceTypeId = "STAMINA",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 12,
                            statPoints = statPoints,
                            talentPoints = talentPoints,
                            attack = 4,
                            defense = 2,
                            accuracy = 3,
                            evasion = 2,
                            speed = 100,
                        ),
                    equipment = emptyList(),
                    talents =
                        listOf(
                            activeTalent(slot = 1, talentId = "power_strike", nameKey = "talent.vanguard.power_strike.name"),
                            activeTalent(slot = 2, talentId = "shield_bash", nameKey = "talent.vanguard.shield_bash.name"),
                            activeTalent(slot = 3, talentId = "guard_stance", nameKey = "talent.vanguard.guard_stance.name"),
                            activeTalent(slot = 4, talentId = "war_cry", nameKey = "talent.vanguard.war_cry.name"),
                        ),
                    reserveTalents = reserveTalents,
                    inscriptions = inscriptions,
                    inventory = inventory,
                    targetablePositions = emptyList(),
                ),
        )

    private fun activeTalent(
        slot: Int,
        talentId: String,
        nameKey: String,
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        hasPendingAllocation: Boolean = false,
    ): TalentSlotSnapshot =
        TalentSlotSnapshot(
            slot = slot,
            talentId = talentId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = nameKey,
            level = 1,
            maxLevel = 5,
            resourceCost = 8,
            resourceLabelKey = "ui.hud.stamina.short",
            resourceTypeId = "STAMINA",
            range = 1,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 3,
            requiresTarget = false,
            hasPendingAllocation = hasPendingAllocation,
        )

    private fun reserveTalent(
        talentId: String,
        nameKey: String,
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        hasPendingAllocation: Boolean = false,
    ): TalentReserveSnapshot =
        TalentReserveSnapshot(
            talentId = talentId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = nameKey,
            level = 1,
            maxLevel = 5,
            resourceCost = 8,
            resourceLabelKey = "ui.hud.stamina.short",
            resourceTypeId = "STAMINA",
            range = 3,
            minRange = 1,
            currentCooldown = 0,
            maxCooldown = 3,
            requiresTarget = true,
            descKey = nameKey.replace(".name", ".desc"),
            hasPendingAllocation = hasPendingAllocation,
        )

    private fun inscriptionSlot(
        hotkey: Int,
        requiresTarget: Boolean,
    ): InscriptionSlotSnapshot =
        InscriptionSlotSnapshot(
            hotkey = hotkey,
            inscriptionId = if (requiresTarget) "controlled_phase" else "phase_door",
            nameKey = "inscription.controlled_phase.name",
            descKey = "inscription.controlled_phase.desc",
            iconKey = "icon.skill.arcanist.blink",
            categoryId = "MOVEMENT",
            cooldownRemaining = 0,
            maxCooldown = 15,
            requiresTarget = requiresTarget,
        )
}
