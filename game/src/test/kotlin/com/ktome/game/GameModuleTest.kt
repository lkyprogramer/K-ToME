package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.MapSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.io.path.writeText

class GameModuleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `items spawn on tiles not occupied by monsters`() {
        val session = GameModule.newFoundationSession(FoundationGameConfig())
        val world = extractWorld(session)

        val itemPositions =
            world.entitiesWith(Position::class, ItemInstance::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()
        val monsterPositions =
            world.entitiesWith(Position::class, MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        assertTrue(itemPositions.intersect(monsterPositions).isEmpty())
    }

    @Test
    fun `new foundation session fails fast when zone or profession id is outside schema v2`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "invalid_zone"),
                SaveManager(tempDir.resolve("invalid-zone-save")),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameModule.newFoundationSession(
                FoundationGameConfig(playerProfessionId = "invalid_profession"),
                SaveManager(tempDir.resolve("invalid-profession-save")),
            )
        }
    }

    @Test
    fun `load foundation session rejects saves with unknown formal zone or profession ids`() {
        val saveManager = SaveManager(tempDir.resolve("invalid-loaded-save"))
        saveManager.save(
            SaveSnapshot(
                timestampEpochMillis = 1L,
                worldSeed = 20260318L,
                currentZoneId = "invalid_zone",
                floorIndex = 1,
                mapWidth = 5,
                mapHeight = 5,
                fovRadius = 8,
                messageLogSize = 8,
                playerProfessionId = "invalid_profession",
                maxFloor = 1,
                turnCount = 0,
                player =
                    PlayerSnapshot(
                        entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true),
                    ),
                floors =
                    listOf(
                        FloorSnapshot(
                            floorIndex = 1,
                            map = MapSnapshot(rows = List(5) { "....." }, playerStart = PointSnapshot(1, 1)),
                        ),
                    ),
            ),
        )

        assertThrows(InvalidSaveException::class.java) {
            GameModule.loadFoundationSession(saveManager)
        }
    }

    @Test
    fun `new foundation session derives starter talents stats and kit from profession schema`() {
        val vanguardSession =
            GameModule.newFoundationSession(
                FoundationGameConfig(playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("vanguard-save")),
            )
        val arcanistSession =
            GameModule.newFoundationSession(
                FoundationGameConfig(playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("arcanist-save")),
            )

        assertEquals(listOf("Power Strike", "Shield Bash", "War Cry"), vanguardSession.talentSlots().map { slot -> slot.name })
        assertTrue(arcanistSession.talentSlots().isEmpty())
        assertEquals(listOf("Short Sword", "Leather Armor", "Healing Potion"), vanguardSession.inventoryItems().map { item -> item.name })
        assertEquals(listOf("Healing Potion"), arcanistSession.inventoryItems().map { item -> item.name })
        assertEquals("Short Sword", vanguardSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.WEAPON }.itemName)
        assertEquals("Leather Armor", vanguardSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.ARMOR }.itemName)
        assertTrue(vanguardSession.playerStatus().maxHp > arcanistSession.playerStatus().maxHp)
    }

    @Test
    fun `new foundation session uses selected zone schema for map size and encounter pool`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "greenwood_fringe"),
                SaveManager(tempDir.resolve("greenwood-save")),
            )
        val world = extractWorld(session)
        val monsterIds =
            world.entitiesWith(MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value }
                .toSet()

        assertEquals(70, session.config.width)
        assertEquals(45, session.config.height)
        assertEquals(70, session.map.width)
        assertEquals(45, session.map.height)
        assertTrue(monsterIds.isNotEmpty())
        assertTrue(monsterIds.all { monsterId -> monsterId in setOf("beast.rat", "undead.bone_archer", "bandit.sentry") })
        assertTrue("undead.bone_archer" in monsterIds)
    }

    @Test
    fun `zone schema owns run length and non boss final floor exits the run`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "greenwood_fringe"),
                SaveManager(tempDir.resolve("greenwood-exit-save")),
            )

        assertEquals(2, session.maxFloor())
        session.automationMovePlayerTo(requireNotNull(session.automationStairPoint(StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertEquals(2, session.currentFloor())
        assertEquals(2, session.maxFloor())

        val exit = session.automationStairPoint(StairDirection.DOWN)
        assertNotNull(exit)
        session.automationMovePlayerTo(requireNotNull(exit))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertTrue(session.isVictory())
    }

    @Test
    fun `zone boss encounter id drives final floor boss routing`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "grey_gate_depths"),
                SaveManager(tempDir.resolve("grey-gate-save")),
            )

        session.automationMovePlayerTo(requireNotNull(session.automationStairPoint(StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = extractWorld(session)
        val monsterIds =
            world.entitiesWith(MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value }
                .toSet()

        assertEquals(2, session.maxFloor())
        assertTrue(FOUNDATION_BOSS_TEMPLATE_ID in monsterIds)
        assertNotNull(session.automationEntityByTemplateId(FOUNDATION_BOSS_TEMPLATE_ID))
        assertNull(session.automationStairPoint(StairDirection.DOWN))
    }

    @Test
    fun `load foundation session rejects saves whose run shape disagrees with zone schema`() {
        val saveManager = SaveManager(tempDir.resolve("stale-run-shape-save"))
        saveManager.save(
            SaveSnapshot(
                timestampEpochMillis = 1L,
                worldSeed = 20260318L,
                currentZoneId = "greenwood_fringe",
                floorIndex = 1,
                mapWidth = 70,
                mapHeight = 45,
                fovRadius = 8,
                messageLogSize = 8,
                playerProfessionId = "vanguard",
                maxFloor = 5,
                turnCount = 0,
                player =
                    PlayerSnapshot(
                        entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true),
                    ),
                floors =
                    listOf(
                        FloorSnapshot(
                            floorIndex = 1,
                            map = zoneSizedMap(width = 70, height = 45, playerStart = PointSnapshot(1, 1)),
                        ),
                    ),
            ),
        )

        assertThrows(InvalidSaveException::class.java) {
            GameModule.loadFoundationSession(saveManager)
        }
    }

    @Test
    fun `load foundation session rejects saves whose cached floor payload size drifts from top level contract`() {
        val saveManager = SaveManager(tempDir.resolve("floor-payload-drift-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText(
            """
            {
              "schemaVersion": 1,
              "saveContractVersion": { "major": 2, "minor": 0 },
              "buildMetadata": "phase2-dev",
              "timestampEpochMillis": 1,
              "worldSeed": 20260318,
              "currentZoneId": "greenwood_fringe",
              "floorIndex": 1,
              "mapWidth": 70,
              "mapHeight": 45,
              "fovRadius": 8,
              "messageLogSize": 8,
              "playerProfessionId": "vanguard",
              "maxFloor": 2,
              "turnCount": 0,
              "player": {
                "entity": {
                  "id": 1,
                  "position": { "x": 1, "y": 1 },
                  "isPlayerControlled": true
                },
                "carriedEntities": []
              },
              "floors": [
                {
                  "floorIndex": 1,
                  "map": {
                    "rows": [".....", ".....", ".....", ".....", "....."],
                    "playerStart": { "x": 1, "y": 1 }
                  },
                  "exploredTiles": [],
                  "entities": []
                }
              ],
              "pendingActionIds": []
            }
            """.trimIndent(),
        )

        assertThrows(InvalidSaveException::class.java) {
            GameModule.loadFoundationSession(saveManager)
        }
    }

    private fun extractWorld(session: FoundationGameSession): World {
        val field = FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }

    private fun zoneSizedMap(
        width: Int,
        height: Int,
        playerStart: PointSnapshot,
    ): MapSnapshot =
        MapSnapshot(
            rows = List(height) { ".".repeat(width) },
            playerStart = playerStart,
        )
}
