package com.ktome.game

import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.get
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.resource.ResourceType
import com.ktome.core.save.SaveManager
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.RenderSnapshotHasher
import com.ktome.core.stats.StatsCalculator
import com.ktome.game.data.DataLoader
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

    private val schemaCatalog = DataLoader().loadSchemaCatalog()

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
        assertEquals("zone.shattered_outpost.name", snapshot.metadata.zoneNameKey)
        assertEquals("audio.zone.shattered_outpost", snapshot.metadata.zoneAudioProfile)
        assertEquals("ambient.shattered_outpost", snapshot.metadata.ambientProfile)
        assertEquals("tileset.ruins", snapshot.metadata.tilesetKey)
        assertEquals("ui.hud.mana.short", snapshot.uiState.playerStatus.resourceLabelKey)
        assertEquals("MANA", snapshot.uiState.playerStatus.resourceTypeId)
        assertEquals(100, snapshot.uiState.playerStatus.currentResource)
        assertEquals(152, snapshot.uiState.playerStatus.maxResource)
        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "supply_crate" })
        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "alarm_bonfire" })
        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.objective.activate" })
        assertTrue(snapshot.actors.all { actor -> actor.nameKey.isNotBlank() })
        assertTrue(snapshot.logEvents.all { event -> event.message.key.isNotBlank() })
    }

    @Test
    fun `arcanist mana cap follows runtime wil after stat recalculation`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("arcanist-runtime-resource")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("arcanist")
        val stats = requireNotNull(world.get<Stats>(playerId))

        stats.wil += 2
        StatsCalculator.recalculateAndStore(world, playerId)

        val pools = PlayerResourcePools.sync(world, playerId, profession)
        val mana = requireNotNull(pools.pool(ResourceType.MANA))

        assertEquals(164, mana.max)
        assertEquals(112, mana.current)
        assertEquals(164, session.renderSnapshot().uiState.playerStatus.maxResource)
    }

    @Test
    fun `rogue energy resource restores on hit`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "rogue"),
                saveManager = SaveManager(tempDir.resolve("rogue-energy")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("rogue")
        val pools = PlayerResourcePools.sync(world, playerId, profession)
        val energy = requireNotNull(pools.pool(ResourceType.ENERGY))

        energy.spend(20)
        PlayerResourcePools.onSuccessfulHit(world, playerId, profession)

        assertEquals(88, energy.current)
        assertEquals(100, session.renderSnapshot().uiState.playerStatus.maxResource)
        assertEquals(88, session.renderSnapshot().uiState.playerStatus.currentResource)
    }

    @Test
    fun `templar positive energy reacts to combat gain and out of combat decay`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("templar-resource")),
            )
        val world = session.automationWorld()
        val playerId = world.entitiesWith(PlayerControlled::class).single()
        val profession = profession("templar")
        val pools = PlayerResourcePools.sync(world, playerId, profession)
        val positive = requireNotNull(pools.pool(ResourceType.POSITIVE_ENERGY))

        val status = session.renderSnapshot().uiState.playerStatus

        assertEquals("POSITIVE_ENERGY", status.resourceTypeId)
        assertEquals("ui.hud.positive_energy.short", status.resourceLabelKey)
        assertEquals(0, status.currentResource)
        assertEquals(100, status.maxResource)

        PlayerResourcePools.onDamageTaken(world, playerId, profession, damage = 20)
        PlayerResourcePools.onSuccessfulHit(world, playerId, profession)
        PlayerResourcePools.onTurnStart(world, playerId, profession, inCombat = false)
        session.automationMovePlayerTo(session.playerPosition())

        assertEquals(1, positive.current)
        assertEquals(1, session.renderSnapshot().uiState.playerStatus.currentResource)
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
        assertEquals("log.warning.boss_presence", overlay.warningMessage?.key)
        assertEquals(1, overlay.previewTurns)
        assertEquals(3, overlay.dangerLevel)
        assertEquals(OverlayShapeSnapshot.SINGLE_TILE, overlay.shape)
        assertTrue(overlay.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })
        assertEquals("war_cry", initialTelegraph.sourceAbilityId)
        assertEquals("log.warning.telegraph", initialTelegraph.warningMessage?.key)
        assertEquals(OverlayShapeSnapshot.RING, initialTelegraph.shape)
        assertEquals(1, initialTelegraph.previewTurns)
        assertTrue(initialTelegraph.cells.any { cell -> cell.x == bossPoint.x && cell.y == bossPoint.y })
        val warningKeys = initialSnapshot.logEvents.takeLast(2).map { event -> event.message.key }
        assertEquals(listOf("log.warning.boss_presence", "log.warning.telegraph"), warningKeys)

        assertTrue(session.perform(PlayerCommand.Wait))

        val followUpSnapshot = session.renderSnapshot()
        val followUpTelegraph = requireNotNull(followUpSnapshot.overlays.singleOrNull { candidate -> candidate.id == "telegraph:${bossId.value}:power_strike" })
        assertEquals("power_strike", followUpTelegraph.sourceAbilityId)
        assertEquals(OverlayShapeSnapshot.SINGLE_TILE, followUpTelegraph.shape)
        assertEquals(1, followUpTelegraph.previewTurns)
        assertTrue(followUpTelegraph.cells.isNotEmpty())
    }

    @Test
    fun `descending into shattered outpost final floor exposes breach props and objective advance`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("objective-props")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))

        session.automationMovePlayerTo(stairsDown)
        assertTrue(session.perform(PlayerCommand.Descend))

        val snapshot = session.renderSnapshot()

        assertTrue(snapshot.props.any { prop -> prop.propTypeId == "armory_gate" })
        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.objective.advance" })
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

    private fun profession(id: String) =
        requireNotNull(schemaCatalog.professions.firstOrNull { profession -> profession.id == id }) {
            "Unknown profession '$id'."
        }
}
