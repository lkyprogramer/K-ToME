package com.ktome.game

import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.get
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.RenderSnapshotHasher
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RenderSnapshotContractTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `same seed and locale produce stable initial render snapshot hash`() {
        val left =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("left")),
            )
        val right =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("right")),
            )

        assertEquals(
            RenderSnapshotHasher.sha256(left.renderSnapshot()),
            RenderSnapshotHasher.sha256(right.renderSnapshot()),
        )
    }

    @Test
    fun `initial snapshot exposes phase2 render contract fields`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("single")),
            )

        val snapshot = session.renderSnapshot()

        assertFalse(snapshot.mapCells.isEmpty())
        assertFalse(snapshot.actors.isEmpty())
        assertTrue(snapshot.uiState.equipment.size >= 2)
        assertTrue(snapshot.logEvents.isNotEmpty())
        assertEquals("shattered_outpost", snapshot.metadata.zoneId)
        assertEquals("audio.zone.shattered_outpost", snapshot.metadata.zoneAudioProfile)
        assertEquals("ambient.shattered_outpost", snapshot.metadata.ambientProfile)
        assertEquals("ui.hud.stamina.short", snapshot.uiState.playerStatus.resourceLabelKey)
        assertTrue(snapshot.actors.all { actor -> actor.nameKey.isNotBlank() })
        assertTrue(snapshot.logEvents.all { event -> event.message.key.isNotBlank() })
    }

    @Test
    fun `explored cells do not expose hidden actor ids`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("fog-contract")),
            )
        val world = session.automationWorld()
        val origin = session.playerPosition()
        val hiddenMonsterId =
            world.entitiesWith(Position::class, MonsterTemplateId::class)
                .first { entityId ->
                    requireNotNull(world.get<Position>(entityId)).toPoint() !in session.visibleTiles()
                }
        val monsterPoint = requireNotNull(world.get<Position>(hiddenMonsterId)).toPoint()

        session.automationMovePlayerTo(monsterPoint)
        assertTrue(monsterPoint in session.visibleTiles())

        session.automationMovePlayerTo(origin)
        val snapshot = session.renderSnapshot()
        val cell = snapshot.mapCells.single { mapCell -> mapCell.x == monsterPoint.x && mapCell.y == monsterPoint.y }

        assertEquals(CellVisibilitySnapshot.EXPLORED, cell.visibility)
        assertNull(cell.actorEntityId)
        assertTrue(snapshot.actors.none { actor -> actor.entityId == hiddenMonsterId.value })
    }

    @Test
    fun `boss floor telegraph follows the next usable boss talent intent`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("boss-warning")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))

        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(session.automationEntityByTemplateId(FOUNDATION_BOSS_TEMPLATE_ID))
        val bossPoint = requireNotNull(session.automationWorld().get<Position>(bossId)).toPoint()
        session.automationMovePlayerTo(bossPoint)

        val initialSnapshot = session.renderSnapshot()
        val overlay = requireNotNull(initialSnapshot.overlays.singleOrNull { candidate -> candidate.id == "boss-warning:${bossId.value}" })
        val initialTelegraph = requireNotNull(initialSnapshot.overlays.singleOrNull { candidate -> candidate.id == "telegraph:${bossId.value}:war_cry" })
        assertEquals("vfx.boss.warning.sigil_01", overlay.visualKey)
        assertEquals(1, overlay.previewTurns)
        assertEquals(3, overlay.dangerLevel)
        assertEquals(OverlayShapeSnapshot.SINGLE_TILE, overlay.shape)
        assertTrue(overlay.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })
        assertEquals("war_cry", initialTelegraph.sourceAbilityId)
        assertEquals(OverlayShapeSnapshot.RING, initialTelegraph.shape)
        assertEquals(1, initialTelegraph.previewTurns)
        assertTrue(initialTelegraph.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })

        assertTrue(session.perform(PlayerCommand.Wait))

        val followUpSnapshot = session.renderSnapshot()
        val followUpTelegraph = requireNotNull(followUpSnapshot.overlays.singleOrNull { candidate -> candidate.id == "telegraph:${bossId.value}:power_strike" })
        assertEquals("power_strike", followUpTelegraph.sourceAbilityId)
        assertEquals(OverlayShapeSnapshot.SINGLE_TILE, followUpTelegraph.shape)
        assertEquals(1, followUpTelegraph.previewTurns)
        assertTrue(followUpTelegraph.cells.isNotEmpty())
    }

    @Test
    fun `player action advances render snapshot revision and hash`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("action-revision")),
            )

        val initial = session.renderSnapshot()
        assertTrue(session.perform(PlayerCommand.Wait))
        val updated = session.renderSnapshot()

        assertTrue(updated.metadata.revision > initial.metadata.revision)
        assertNotEquals(RenderSnapshotHasher.sha256(initial), RenderSnapshotHasher.sha256(updated))
    }

    @Test
    fun `loaded session save load round trip preserves render snapshot hash`() {
        val saveManager = SaveManager(tempDir.resolve("snapshot-roundtrip"))
        val original =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = saveManager,
            )
        repeat(2) {
            assertTrue(original.perform(PlayerCommand.Wait))
        }
        assertTrue(original.saveOnExit())

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val loadedSnapshot = loaded.renderSnapshot()
        assertTrue(loaded.saveOnExit())

        val reloaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val reloadedSnapshot = reloaded.renderSnapshot()

        assertEquals(RenderSnapshotHasher.sha256(loadedSnapshot), RenderSnapshotHasher.sha256(reloadedSnapshot))
    }

    @Test
    fun `same floor transition produces stable render snapshot hash`() {
        val left =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("transition-left")),
            )
        val right =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("transition-right")),
            )

        left.automationMovePlayerTo(requireNotNull(left.automationStairPoint(StairDirection.DOWN)))
        right.automationMovePlayerTo(requireNotNull(right.automationStairPoint(StairDirection.DOWN)))
        assertTrue(left.perform(PlayerCommand.Descend))
        assertTrue(right.perform(PlayerCommand.Descend))

        val leftSnapshot = left.renderSnapshot()
        val rightSnapshot = right.renderSnapshot()

        assertEquals(2, leftSnapshot.metadata.currentFloor)
        assertEquals(leftSnapshot.metadata.currentFloor, rightSnapshot.metadata.currentFloor)
        assertEquals(RenderSnapshotHasher.sha256(leftSnapshot), RenderSnapshotHasher.sha256(rightSnapshot))
    }
}
