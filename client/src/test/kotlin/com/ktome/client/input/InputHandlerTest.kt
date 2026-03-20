package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.replay.ReplayInputSource
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class InputHandlerTest {
    @TempDir
    lateinit var tempDir: Path

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
}
