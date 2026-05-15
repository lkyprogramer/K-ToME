package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.render.DemoNavRailButtonLayout
import com.ktome.client.render.layout.DemoShellLayoutRequest
import com.ktome.client.render.layout.DemoShellLayoutSolver
import com.ktome.client.replay.ReplayInputSource
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.ModalStack
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.validation.ValidationScenarioPresentationCatalog
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.InscriptionReplacementCategoryChangeSnapshot
import com.ktome.core.snapshot.InscriptionReplacementEntrySnapshot
import com.ktome.core.snapshot.InscriptionReplacementPromptSnapshot
import com.ktome.core.snapshot.InscriptionSlotSnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.RouteOptionSnapshot
import com.ktome.core.snapshot.RouteSelectionSnapshot
import com.ktome.core.snapshot.ShopOfferSnapshot
import com.ktome.core.snapshot.ShopPanelSnapshot
import com.ktome.core.snapshot.ShopSellEntrySnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.GameModule
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import com.ktome.game.PlayerCommand
import com.ktome.game.validation.ValidationOverlaySection
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationScenarioId
import com.ktome.game.validation.ValidationScenarioRegistry
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.validationSessionOptionsForPreset
import java.nio.file.Path
import kotlin.math.roundToInt
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
    fun `scenario validation overlay starts at scenario initial section`() {
        val scenario = ValidationScenarioRegistry.require(ValidationScenarioId("phase4-v4-pr00-selftest"))
        val input = ReplayInputSource()
        val handler =
            InputHandler(
                input = input,
                validationOverlayAvailability = ValidationOverlayAvailability.ENABLED,
                validationPreset = scenario.runtime.preset,
                validationScenarioId = scenario.id,
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-scenario-save")),
                    options = validationSessionOptionsForPreset(scenario.runtime.preset),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))

        assertEquals(
            ValidationScenarioPresentationCatalog.require(scenario.id).initialOverlaySection,
            handler.overlayState().validationCursor?.selectedSection,
        )
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
        assertEquals(ValidationOverlaySection.PR05_COMBAT, handler.overlayState().validationCursor?.selectedSection)
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
        assertEquals(ValidationOverlaySection.PR05_COMBAT, handler.overlayState().validationCursor?.selectedSection)
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
    fun `demo nav rail clicks open inventory context focus talent and validation panels`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 1,
                talentTrees =
                    listOf(
                        talentTree(
                            treeId = "vanguard_arms",
                            nodes = listOf(talentTreeNode("charge", "vanguard_arms", "talent.vanguard.charge.name")),
                        ),
                    ),
            )

        input.clickNavRailButton(index = 1)
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        assertEquals(ModalFrameKind.INVENTORY, handler.overlayState().activeModalKind)
        input.clear()

        input.clickNavRailButton(index = 2)
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertEquals(PaneFocusAnchor.CONTEXT, handler.overlayState().paneFocusAnchor)
        input.clear()

        input.clickNavRailButton(index = 3)
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
        assertEquals(ModalFrameKind.TALENT_ASSIGN, handler.overlayState().activeModalKind)
        input.clear()

        input.clickNavRailButton(index = 4)
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        input.clear()

        input.clickNavRailButton(index = 0)
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertEquals(PaneFocusAnchor.WORLD, handler.overlayState().paneFocusAnchor)
    }

    @Test
    fun `validation overlay noops inspect vim keys without moving the inspect cursor`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-overlay-vim-noop-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        val cursor = handler.overlayState().inspectCursor
        input.clear()

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(cursor, handler.overlayState().inspectCursor)
        assertEquals("DEBUG validation.inspect-key.noop", handler.overlayState().debugMessageKey)
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
            val scope =
                ValidationOverlayDescriptorScope(
                    preset = preset,
                    restartMode =
                        if (restartNextSeedEnabled) {
                            ValidationOverlayRestartMode.NEXT_SEED_ENABLED
                        } else {
                            ValidationOverlayRestartMode.SAME_PRESET_ONLY
                        },
                )
            val plan = ValidationOverlayDescriptorPlanCache.plan(scope)
            availableValidationOverlaySections(scope).forEach { section ->
                val descriptors = plan.descriptors(section)
                assertTrue(descriptors.isNotEmpty(), "Expected actions for $preset / $section")
                descriptors.forEachIndexed { index, descriptor ->
                    if (descriptor.buildAction != null) {
                        assertEquals(
                            descriptor.requireGameAction(inspectCursor),
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
                    } else {
                        assertEquals(
                            descriptor,
                            validationOverlayActionDescriptor(
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
    }

    @Test
    fun `pr05 validation combat decision fixtures open client only surfaces`() {
        val input = ReplayInputSource()
        val handler =
            InputHandler(
                input = input,
                validationOverlayAvailability = ValidationOverlayAvailability.ENABLED,
                validationPreset = ValidationPreset.BOSS_VARIANT,
            )
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("pr05-combat-fixture-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.BOSS_VARIANT),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        input.clear()

        repeat(ValidationOverlaySection.entries.indexOf(ValidationOverlaySection.PR05_COMBAT)) {
            input.frame(justPressed = setOf(Keys.DOWN))
            assertNull(handler.pollCommand(session.renderSnapshot()))
            input.clear()
        }
        assertEquals(ValidationOverlaySection.PR05_COMBAT, handler.overlayState().validationCursor?.selectedSection)

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        assertEquals(
            com.ktome.client.ui.combat.CombatDecisionValidationSurface.METHOD,
            handler.overlayState().validationCombatDecisionSurface,
        )
        assertEquals(
            com.ktome.client.ui.combat.CombatDecisionPhase.METHOD,
            handler.overlayState().modalFrames.last().localState.combatDecisionState?.phase,
        )
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(
            com.ktome.client.ui.combat.CombatDecisionPhase.TARGET,
            handler.overlayState().modalFrames.last().localState.combatDecisionState?.phase,
        )
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals("ui.message.save.blocked-in-combat-decision", handler.overlayState().uiMessageKey)
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
    fun `inspect question mark opens explain pane and backspace closes subview first`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.SLASH), pressed = setOf(Keys.SHIFT_LEFT, Keys.SLASH))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        assertTrue(handler.overlayState().explainPaneOpen)
        assertEquals(ModalFrameKind.INSPECT, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        assertFalse(handler.overlayState().explainPaneOpen)
        assertEquals(ModalFrameKind.INSPECT, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.SLASH), pressed = setOf(Keys.SHIFT_LEFT, Keys.SLASH))
        assertNull(handler.pollCommand(snapshot))
        assertTrue(handler.overlayState().explainPaneOpen)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertFalse(handler.overlayState().explainPaneOpen)
    }

    @Test
    fun `reopening inspect clears stale explain pane after validation toggle`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("inspect-explain-validation-toggle-save")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.SLASH), pressed = setOf(Keys.SHIFT_LEFT, Keys.SLASH))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertTrue(handler.overlayState().explainPaneOpen)
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        assertFalse(handler.overlayState().explainPaneOpen)
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertFalse(handler.overlayState().explainPaneOpen)
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.INSPECT, handler.overlayState().mode)
        assertFalse(handler.overlayState().explainPaneOpen)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `pending stat allocation passively takes over and blocks inspect stack`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val session = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("pending-stat-save")))
        requireNotNull(runtimeWorld(session).get<Experience>(session.playerId)).unspentStatPoints = 1

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals(UiMode.STAT_ASSIGN, handler.overlayState().mode)
        assertEquals("ui.message.force-switch.stat-assign", handler.overlayState().uiMessageKey)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
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
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.SYM, Keys.S))
        assertEquals(PlayerCommand.SaveGame, handler.pollCommand(session.renderSnapshot()))
    }

    @Test
    fun `map root escape backspace and f are noops while tab cycles pane focus`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout()

        listOf(Keys.ESCAPE, Keys.BACKSPACE, Keys.F).forEach { key ->
            input.frame(justPressed = setOf(key))
            assertNull(handler.pollCommand(snapshot))
            assertEquals(UiMode.MAP, handler.overlayState().mode)
            input.clear()
        }

        input.frame(justPressed = setOf(Keys.TAB))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(PaneFocusAnchor.CONTEXT, handler.overlayState().paneFocusAnchor)
        input.clear()

        input.frame(justPressed = setOf(Keys.TAB), pressed = setOf(Keys.SHIFT_LEFT, Keys.TAB))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(PaneFocusAnchor.WORLD, handler.overlayState().paneFocusAnchor)
    }

    @Test
    fun `modal save preserves active inventory stack while targeting and validation block saves`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val snapshot =
            snapshotWithLoadout(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = ItemRenderSnapshot(baseItemId = "long_sword", nameKey = "item.long_sword.name", typeId = "WEAPON"),
                        ),
                    ),
                reserveTalents = listOf(reserveTalent("charge", "talent.vanguard.charge.name")),
                inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(3, 3)),
            )

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertEquals(PlayerCommand.SaveGame, handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(snapshot))
        assertEquals("ui.message.save.blocked-in-combat-decision", handler.overlayState().uiMessageKey)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.VALIDATION, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(snapshot))
        assertEquals("ui.message.save.blocked-in-validation", handler.overlayState().uiMessageKey)
    }

    @Test
    fun `validation blocked save feedback persists across render frames`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val session =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-save-toast-persistence")),
                    options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF),
                ),
            )

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals("ui.message.save.blocked-in-validation", handler.overlayState().uiMessageKey)
        input.clear()

        repeat(3) {
            input.frame()
            assertNull(handler.pollCommand(session.renderSnapshot()))
            assertEquals("ui.message.save.blocked-in-validation", handler.overlayState().uiMessageKey)
        }
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.SYM, Keys.S))
        assertNull(handler.pollCommand(session.renderSnapshot()))
        assertEquals("ui.message.save.blocked-in-validation", handler.overlayState().uiMessageKey)
    }

    @Test
    fun `passive world map and shop takeover clear active modal stack with feedback`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val baseSnapshot =
            snapshotWithLoadout(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = ItemRenderSnapshot(baseItemId = "long_sword", nameKey = "item.long_sword.name", typeId = "WEAPON"),
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(baseSnapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        assertNull(handler.pollCommand(baseSnapshot.copy(uiState = baseSnapshot.uiState.copy(activeRouteSelection = sampleRouteSelection()))))
        assertEquals(UiMode.WORLD_MAP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        assertEquals(PaneFocusAnchor.WORLD, handler.overlayState().paneFocusAnchor)
        assertEquals("ui.message.force-switch.world-map", handler.overlayState().uiMessageKey)

        assertNull(handler.pollCommand(baseSnapshot))
        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(baseSnapshot))
        input.clear()

        assertNull(handler.pollCommand(baseSnapshot.copy(uiState = baseSnapshot.uiState.copy(activeShop = sampleShop()))))
        assertEquals(UiMode.SHOP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        assertEquals("ui.message.force-switch.shop", handler.overlayState().uiMessageKey)
    }

    @Test
    fun `passive owners reconcile before validation toggle`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input, ValidationOverlayAvailability.ENABLED)
        val snapshot = snapshotWithLoadout(activeShop = sampleShop())

        input.frame(justPressed = setOf(Keys.F9))
        assertNull(handler.pollCommand(snapshot))

        assertEquals(UiMode.SHOP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        assertEquals("ui.message.force-switch.shop", handler.overlayState().uiMessageKey)
    }

    @Test
    fun `shop inscription replacement accepts number selection enter confirm and escape cancel`() {
        val prompt = sampleInscriptionReplacementPrompt()
        val snapshot = snapshotWithLoadout(activeShop = sampleShop(inscriptionReplacementPrompt = prompt))
        val input = ReplayInputSource()
        val handler = InputHandler(input)

        input.frame(justPressed = setOf(Keys.NUM_6))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.SHOP, handler.overlayState().mode)
        assertEquals(6, handler.overlayState().inscriptionReplacementHotkeySelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.BuyShopOffer(index = 2, offerFingerprint = "prompt-fp", replacementHotkey = 6), handler.pollCommand(snapshot))
        input.clear()

        val cancelInput = ReplayInputSource()
        val cancelHandler = InputHandler(cancelInput)
        cancelInput.frame(justPressed = setOf(Keys.ESCAPE))
        assertEquals(PlayerCommand.CancelInscriptionReplacementPurchase, cancelHandler.pollCommand(snapshot))

        val noHotkeyPrompt = prompt.copy(currentSlots = prompt.currentSlots.map { slot -> slot.copy(hotkey = null) })
        val noHotkeySnapshot = snapshotWithLoadout(activeShop = sampleShop(inscriptionReplacementPrompt = noHotkeyPrompt))
        val noHotkeyCancelInput = ReplayInputSource()
        val noHotkeyCancelHandler = InputHandler(noHotkeyCancelInput)
        noHotkeyCancelInput.frame(justPressed = setOf(Keys.ESCAPE))
        assertEquals(PlayerCommand.CancelInscriptionReplacementPurchase, noHotkeyCancelHandler.pollCommand(noHotkeySnapshot))
    }

    @Test
    fun `combat decision frame blocks ctrl s without saving`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(3, 3)),
            )

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(snapshot))
        assertEquals("ui.message.save.blocked-in-combat-decision", handler.overlayState().uiMessageKey)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `combat decision frame walks action target and save semantics`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talents =
                    listOf(
                        activeTalent(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(4, 3)),
            )

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.S), pressed = setOf(Keys.CONTROL_LEFT, Keys.S))
        assertNull(handler.pollCommand(snapshot))
        assertEquals("ui.message.save.blocked-in-combat-decision", handler.overlayState().uiMessageKey)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(Point(4, 3), handler.overlayState().targetingCursor)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.UseTalent(slot = 1, target = Point(4, 3)), handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `combat decision reports no legal target only after target phase has no targets`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talents =
                    listOf(
                        activeTalent(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = emptyList(),
            )

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(
            com.ktome.client.ui.combat.CombatDecisionPhase.TARGET,
            handler.overlayState().modalFrames.last().localState.combatDecisionState?.phase,
        )
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals("ui.message.combat.no-legal-target", handler.overlayState().uiMessageKey)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
    }

    @Test
    fun `combat decision target phase ignores out of range numeric shortcuts`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talents =
                    listOf(
                        activeTalent(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(4, 3)),
            )

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(Point(4, 3), handler.overlayState().targetingCursor)
        input.clear()

        input.frame(justPressed = setOf(Keys.NUM_9))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        assertEquals(Point(4, 3), handler.overlayState().targetingCursor)
        assertNull(handler.overlayState().uiMessageKey)
    }

    @Test
    fun `modal depth preflight keeps current frame and emits overflow feedback`() {
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
                    ),
            )
        val stackField = InputHandler::class.java.getDeclaredField("modalStack")
        stackField.isAccessible = true
        val stack = stackField.get(handler) as ModalStack
        stack.push(ModalFrame(ModalFrameKind.INVENTORY))
        stack.push(ModalFrame(ModalFrameKind.INSPECT))
        stack.push(ModalFrame(ModalFrameKind.ITEM_DETAIL))
        val modeField = InputHandler::class.java.getDeclaredField("mode")
        modeField.isAccessible = true
        modeField.set(handler, UiMode.INVENTORY)

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_DETAIL, handler.overlayState().activeModalKind)
        assertEquals("ui.message.modal.stack-overflow", handler.overlayState().uiMessageKey)
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
    fun `controlled inscription hotkey enters combat decision target phase and confirms targeted use`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(3, 3), com.ktome.core.snapshot.GridPointSnapshot(4, 3)),
            )

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
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
    fun `controlled inscription combat decision accepts arbitrary cursor target outside hostile list`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)),
                targetablePositions = emptyList(),
            )

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TARGETING, handler.overlayState().mode)
        assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.RIGHT))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(Point(4, 3), handler.overlayState().targetingCursor)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.UseInscription(hotkey = 5, target = Point(4, 3)), handler.pollCommand(snapshot))
    }

    @Test
    fun `rejected targeted talent command reuses current targeting frame`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talents =
                    listOf(
                        activeTalent(
                            slot = 1,
                            talentId = "power_strike",
                            nameKey = "talent.vanguard.power_strike.name",
                            requiresTarget = true,
                        ),
                    ),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(3, 3)),
            )

        input.frame(justPressed = setOf(Keys.NUM_1))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(1, handler.overlayState().modalFrames.size)
        input.clear()

        repeat(2) {
            input.frame(justPressed = setOf(Keys.ENTER))
            val command = requireNotNull(handler.pollCommand(snapshot))
            assertEquals(PlayerCommand.UseTalent(slot = 1, target = Point(3, 3)), command)
            handler.onCommandResult(snapshot, command, consumed = false)
            assertEquals(UiMode.TARGETING, handler.overlayState().mode)
            assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
            assertEquals(1, handler.overlayState().modalFrames.size)
            assertEquals(1, handler.overlayState().targetingSlot)
            assertEquals(Point(3, 3), handler.overlayState().targetingCursor)
            input.clear()
        }
    }

    @Test
    fun `rejected targeted inscription command reuses current targeting frame`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inscriptions = listOf(inscriptionSlot(hotkey = 5, requiresTarget = true)),
                targetablePositions = listOf(com.ktome.core.snapshot.GridPointSnapshot(3, 3)),
            )

        input.frame(justPressed = setOf(Keys.NUM_5))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(1, handler.overlayState().modalFrames.size)
        input.clear()

        repeat(2) {
            input.frame(justPressed = setOf(Keys.ENTER))
            val command = requireNotNull(handler.pollCommand(snapshot))
            assertEquals(PlayerCommand.UseInscription(hotkey = 5, target = Point(3, 3)), command)
            handler.onCommandResult(snapshot, command, consumed = false)
            assertEquals(UiMode.TARGETING, handler.overlayState().mode)
            assertEquals(ModalFrameKind.COMBAT_DECISION, handler.overlayState().activeModalKind)
            assertEquals(1, handler.overlayState().modalFrames.size)
            assertEquals(5, handler.overlayState().targetingInscriptionHotkey)
            assertEquals(Point(3, 3), handler.overlayState().targetingCursor)
            input.clear()
        }
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
    fun `pending stat allocation clears active loadout stack`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithLoadout(statPoints = 1, reserveTalents = listOf(reserveTalent("charge", "talent.vanguard.charge.name")))

        input.frame(justPressed = setOf(Keys.L))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.STAT_ASSIGN, handler.overlayState().mode)
        assertEquals("ui.message.force-switch.stat-assign", handler.overlayState().uiMessageKey)
    }

    @Test
    fun `talent assign mode maps confirm rollback and respec commands`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 2,
                talents =
                    listOf(
                        activeTalent(slot = 1, talentId = "power_strike", nameKey = "talent.vanguard.power_strike.name", hasPendingAllocation = true),
                        activeTalent(slot = 2, talentId = "shield_bash", nameKey = "talent.vanguard.shield_bash.name"),
                        activeTalent(slot = 3, talentId = "guard_stance", nameKey = "talent.vanguard.guard_stance.name"),
                    ),
            )

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
    fun `inventory mode opens item detail and uses escape as full close`() {
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

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(0, handler.overlayState().inventorySelection)
        assertEquals(ModalFrameKind.INVENTORY, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.SPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_DETAIL, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.INVENTORY, handler.overlayState().activeModalKind)
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        input.clear()

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.D))
        assertEquals(PlayerCommand.DropInventoryItem(0), handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_DETAIL, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_COMPARE, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.E))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_COMPARE, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.ITEM_DETAIL, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        input.clear()

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.F))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
    }

    @Test
    fun `inventory escape and backspace close root frame while x is not list navigation`() {
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

        input.frame(justPressed = setOf(Keys.X))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(0, handler.overlayState().inventorySelection)
        assertEquals(ModalFrameKind.INVENTORY, handler.overlayState().activeModalKind)
        input.clear()

        input.frame(justPressed = setOf(Keys.BACKSPACE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
        input.clear()

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        assertTrue(handler.overlayState().modalFrames.isEmpty())
    }

    @Test
    fun `inventory page up and page down jump to page first slots while map mode keeps diagonal movement`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                inventory =
                    List(17) { index ->
                        InventoryEntrySnapshot(
                            index = index,
                            item =
                                ItemRenderSnapshot(
                                    baseItemId = "test_item_$index",
                                    nameKey = "item.healing_potion.name",
                                    typeId = "CONSUMABLE",
                                ),
                        )
                    },
            )

        input.frame(justPressed = setOf(Keys.I))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.INVENTORY, handler.overlayState().mode)
        assertEquals(0, handler.overlayState().inventorySelection)
        input.clear()

        repeat(3) {
            input.frame(justPressed = setOf(Keys.DOWN))
            assertNull(handler.pollCommand(snapshot))
            input.clear()
        }
        assertEquals(3, handler.overlayState().inventorySelection)

        input.frame(justPressed = setOf(Keys.PAGE_DOWN))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(8, handler.overlayState().inventorySelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.PAGE_DOWN))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(16, handler.overlayState().inventorySelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.PAGE_UP))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(8, handler.overlayState().inventorySelection)
        input.clear()

        input.frame(justPressed = setOf(Keys.ESCAPE))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.MAP, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.PAGE_DOWN))
        assertEquals(PlayerCommand.Move(Point(1, 1)), handler.pollCommand(snapshot))
    }

    @Test
    fun `talent assign respec follows focused tree owner`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 2,
                talentTrees =
                    listOf(
                        talentTree(
                            treeId = "shalore_moon",
                            ownerType = TalentTreeOwnerType.RACE,
                            treeOwnerId = "shalore",
                            nodes =
                                listOf(
                                    talentTreeNode(
                                        talentId = "moon_blessing",
                                        treeId = "shalore_moon",
                                        nameKey = "talent.shalore.moon_blessing.name",
                                        ownerType = TalentTreeOwnerType.RACE,
                                        treeOwnerId = "shalore",
                                    ),
                                ),
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
    fun `talent assign mode can invest selected tree talent directly`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 2,
                talentTrees =
                    listOf(
                        talentTree(
                            treeId = "vanguard_arms",
                            nodes =
                                listOf(
                                    talentTreeNode("charge", "vanguard_arms", "talent.vanguard.charge.name"),
                                    talentTreeNode("sweeping_strike", "vanguard_arms", "talent.vanguard.sweeping_strike.name"),
                                ),
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(UiMode.TALENT_ASSIGN, handler.overlayState().mode)
        input.clear()

        input.frame(justPressed = setOf(Keys.DOWN))
        assertNull(handler.pollCommand(snapshot))
        assertEquals(1, handler.overlayState().talentTreeSelection)
        assertEquals(TalentAssignFocus.TREE, handler.overlayState().talentAssignFocus)
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.AssignTalent("sweeping_strike"), handler.pollCommand(snapshot))
    }

    @Test
    fun `pending active or sustained tree talent opens active slot choice when loadout is full`() {
        listOf(TalentCategory.ACTIVE, TalentCategory.SUSTAINED).forEach { category ->
            val input = ReplayInputSource()
            val handler = InputHandler(input)
            val snapshot = snapshotWithPendingTreeTalent(category)

            input.frame(justPressed = setOf(Keys.T))
            assertNull(handler.pollCommand(snapshot))
            input.clear()

            input.frame(justPressed = setOf(Keys.ENTER))
            assertNull(handler.pollCommand(snapshot))
            assertEquals(ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE, handler.overlayState().activeModalKind)
        }
    }

    @Test
    fun `pending passive tree talent confirms without active slot choice when loadout is full`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot = snapshotWithPendingTreeTalent(TalentCategory.PASSIVE)

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        input.clear()

        input.frame(justPressed = setOf(Keys.ENTER))
        assertEquals(PlayerCommand.ConfirmTalentDraft, handler.pollCommand(snapshot))
        assertEquals(ModalFrameKind.TALENT_ASSIGN, handler.overlayState().activeModalKind)
    }

    @Test
    fun `talent assign p toggles tree preview without creating a command`() {
        val input = ReplayInputSource()
        val handler = InputHandler(input)
        val snapshot =
            snapshotWithLoadout(
                talentPoints = 1,
                talentTrees =
                    listOf(
                        talentTree(
                            treeId = "vanguard_arms",
                            nodes =
                                listOf(
                                    talentTreeNode("charge", "vanguard_arms", "talent.vanguard.charge.name"),
                                ),
                        ),
                    ),
            )

        input.frame(justPressed = setOf(Keys.T))
        assertNull(handler.pollCommand(snapshot))
        assertTrue(handler.overlayState().talentTreePreviewExpanded)
        input.clear()

        input.frame(justPressed = setOf(Keys.P))
        assertNull(handler.pollCommand(snapshot))
        assertFalse(handler.overlayState().talentTreePreviewExpanded)
        assertFalse(handler.overlayState().modalFrames.last().localState.talentTreePreviewExpanded)
        input.clear()

        input.frame(justPressed = setOf(Keys.P))
        assertNull(handler.pollCommand(snapshot))
        assertTrue(handler.overlayState().talentTreePreviewExpanded)
        assertTrue(handler.overlayState().modalFrames.last().localState.talentTreePreviewExpanded)
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

    private fun ReplayInputSource.clickNavRailButton(index: Int) {
        val layout =
            DemoShellLayoutSolver.resolve(
                DemoShellLayoutRequest(
                    viewportWidth = 1280,
                    viewportHeight = 800,
                    cellSize = 32,
                ),
            )
        val bounds = DemoNavRailButtonLayout.resolve(layout.navRail, itemCount = 5)[index]
        click(
            x = (bounds.x + bounds.width / 2f).roundToInt(),
            y = (bounds.y + bounds.height / 2f).roundToInt(),
        )
    }

    private fun snapshotWithLoadout(
        statPoints: Int = 0,
        talentPoints: Int = 0,
        reserveTalents: List<TalentReserveSnapshot> = emptyList(),
        inscriptions: List<InscriptionSlotSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
        activeShop: ShopPanelSnapshot? = null,
        activeRouteSelection: RouteSelectionSnapshot? = null,
        targetablePositions: List<com.ktome.core.snapshot.GridPointSnapshot> = emptyList(),
        talentTrees: List<TalentTreeSnapshot> = emptyList(),
        talents: List<TalentSlotSnapshot> =
            listOf(
                activeTalent(slot = 1, talentId = "power_strike", nameKey = "talent.vanguard.power_strike.name"),
                activeTalent(slot = 2, talentId = "shield_bash", nameKey = "talent.vanguard.shield_bash.name"),
                activeTalent(slot = 3, talentId = "guard_stance", nameKey = "talent.vanguard.guard_stance.name"),
                activeTalent(slot = 4, talentId = "war_cry", nameKey = "talent.vanguard.war_cry.name"),
            ),
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
                    talents = talents,
                    reserveTalents = reserveTalents,
                    talentTrees = talentTrees,
                    inscriptions = inscriptions,
                    inventory = inventory,
                    activeShop = activeShop,
                    activeRouteSelection = activeRouteSelection,
                    targetablePositions = targetablePositions,
                ),
        )

    private fun talentTree(
        treeId: String,
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        nodes: List<TalentTreeNodeSnapshot>,
    ): TalentTreeSnapshot =
        TalentTreeSnapshot(
            treeId = treeId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = "talent_tree.$treeId.name",
            descKey = "talent_tree.$treeId.desc",
            nodes = nodes,
        )

    private fun talentTreeNode(
        talentId: String,
        treeId: String,
        nameKey: String,
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        state: TalentNodeStateSnapshot = TalentNodeStateSnapshot.LEARNABLE,
        category: TalentCategory = TalentCategory.ACTIVE,
        rank: Int = if (state == TalentNodeStateSnapshot.LEARNED_ACTIVE || state == TalentNodeStateSnapshot.LEARNED_RESERVE) 1 else 0,
        committedRank: Int = rank,
        hasPendingAllocation: Boolean = false,
    ): TalentTreeNodeSnapshot =
        TalentTreeNodeSnapshot(
            talentId = talentId,
            treeId = treeId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = nameKey,
            descKey = nameKey.replace(".name", ".desc"),
            category = category,
            state = state,
            rank = rank,
            committedRank = committedRank,
            maxRank = 5,
            unlockLevel = 1,
            resourceCost = 8,
            resourceLabelKey = "ui.hud.stamina.short",
            range = 3,
            minRange = 1,
            currentCooldown = 0,
            maxCooldown = 3,
            requiresTarget = true,
            hasPendingAllocation = hasPendingAllocation,
        )

    private fun snapshotWithPendingTreeTalent(category: TalentCategory): RenderSnapshot =
        snapshotWithLoadout(
            talents =
                (1..PLAYER_ACTIVE_TALENT_SLOT_COUNT).map { slot ->
                    activeTalent(
                        slot = slot,
                        talentId = "active_$slot",
                        nameKey = "talent.vanguard.power_strike.name",
                    )
                },
            talentTrees =
                listOf(
                    talentTree(
                        treeId = "vanguard_arms",
                        nodes =
                            listOf(
                                talentTreeNode(
                                    talentId = "charge",
                                    treeId = "vanguard_arms",
                                    nameKey = "talent.vanguard.charge.name",
                                    state = TalentNodeStateSnapshot.LEARNED_RESERVE,
                                    category = category,
                                    rank = 1,
                                    committedRank = 0,
                                    hasPendingAllocation = true,
                                ),
                            ),
                    ),
                ),
        )

    private fun activeTalent(
        slot: Int,
        talentId: String,
        nameKey: String,
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        hasPendingAllocation: Boolean = false,
        requiresTarget: Boolean = false,
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
            requiresTarget = requiresTarget,
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

    private fun sampleShop(
        inscriptionReplacementPrompt: InscriptionReplacementPromptSnapshot? = null,
    ): ShopPanelSnapshot =
        ShopPanelSnapshot(
            shopId = "test_shop",
            shopNameKey = "ui.sidebar.shop",
            offers =
                listOf(
                    ShopOfferSnapshot(
                        index = 0,
                        labelKey = "item.long_sword.name",
                        price = 10,
                        offerFingerprint = "offer-0",
                    ),
                ),
            sellEntries = listOf(ShopSellEntrySnapshot(inventoryIndex = 0, price = 4)),
            inscriptionReplacementPrompt = inscriptionReplacementPrompt,
        )

    private fun sampleInscriptionReplacementPrompt(): InscriptionReplacementPromptSnapshot =
        InscriptionReplacementPromptSnapshot(
            offerIndex = 2,
            offerFingerprint = "prompt-fp",
            candidate =
                InscriptionReplacementEntrySnapshot(
                    inscriptionId = "controlled_phase",
                    nameKey = "inscription.controlled_phase.name",
                    descKey = "inscription.controlled_phase.desc",
                    iconKey = "icon.skill.arcanist.blink",
                    categoryId = "MOVEMENT",
                    categoryLabelKey = "ui.inscription.category.MOVEMENT",
                    effectTagLabelKeys = listOf("ui.inscription.effect_tag.mobility", "ui.inscription.effect_tag.control"),
                    maxCooldown = 15,
                    upgradeFromInscriptionId = "phase_door",
                ),
            currentSlots =
                listOf(
                    InscriptionReplacementEntrySnapshot(
                        hotkey = 5,
                        inscriptionId = "healing_light",
                        nameKey = "inscription.healing_light.name",
                        descKey = "inscription.healing_light.desc",
                        iconKey = "icon.inscription.heal",
                        categoryId = "HEALING",
                        categoryLabelKey = "ui.inscription.category.HEALING",
                        effectTagLabelKeys = listOf("ui.inscription.effect_tag.heal"),
                        maxCooldown = 8,
                    ),
                    InscriptionReplacementEntrySnapshot(
                        hotkey = 6,
                        inscriptionId = "phase_door",
                        nameKey = "inscription.phase_door.name",
                        descKey = "inscription.phase_door.desc",
                        iconKey = "icon.skill.arcanist.blink",
                        categoryId = "MOVEMENT",
                        categoryLabelKey = "ui.inscription.category.MOVEMENT",
                        effectTagLabelKeys = listOf("ui.inscription.effect_tag.mobility"),
                        maxCooldown = 12,
                    ),
                ),
            categoryChanges =
                listOf(
                    InscriptionReplacementCategoryChangeSnapshot(
                        targetHotkey = 6,
                        categoryId = "MOVEMENT",
                        categoryLabelKey = "ui.inscription.category.MOVEMENT",
                        beforeCount = 1,
                        afterCount = 1,
                        limit = 2,
                    ),
                ),
            price = 45,
        )

    private fun sampleRouteSelection(): RouteSelectionSnapshot =
        RouteSelectionSnapshot(
            currentZoneNameKey = "zone.shattered_outpost.name",
            options =
                listOf(
                    RouteOptionSnapshot(
                        index = 0,
                        routeId = "route:next",
                        destinationZoneId = "greenwood_fringe",
                        destinationZoneNameKey = "zone.greenwood_fringe.name",
                        recommendedLevelMin = 1,
                        recommendedLevelMax = 3,
                        shardReward = 1,
                    ),
                ),
        )
}
