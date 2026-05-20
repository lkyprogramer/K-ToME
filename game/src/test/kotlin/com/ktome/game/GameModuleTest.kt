package com.ktome.game

import com.ktome.core.dungeon.StairDirection
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.profile.AvailabilityContext
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.core.profile.ProfileData
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveContractVersion
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.UnsupportedSaveContractVersionException
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.resource.ResourcePoolSnapshot
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaLevelRange
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationSessionRequest
import com.ktome.game.validation.validationSessionOptionsForPreset
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.math.abs
import kotlin.io.path.writeText

class GameModuleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `recommended level for floor tracks zone floor progression conservatively`() {
        val zone = DataLoader().loadSchemaCatalog().zones.first { schema -> schema.id == "shattered_outpost" }

        assertEquals(2, recommendedLevelForZoneFloor(zone, 1))
        assertEquals(4, recommendedLevelForZoneFloor(zone, 2))
        assertEquals(3, recommendedLevelForFloor(SchemaLevelRange(min = 2, max = 8), floorIndex = 1, floorCount = 4))
        assertEquals(5, recommendedLevelForFloor(SchemaLevelRange(min = 2, max = 8), floorIndex = 2, floorCount = 4))
        assertEquals(7, recommendedLevelForFloor(SchemaLevelRange(min = 2, max = 8), floorIndex = 3, floorCount = 4))
        assertEquals(8, recommendedLevelForFloor(SchemaLevelRange(min = 2, max = 8), floorIndex = 4, floorCount = 4))
    }

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
    fun `foundation session factory reuses content without sharing mutable session state`() {
        val factory = GameModule.newFoundationSessionFactory()
        val first =
            factory.newSession(
                config = FoundationGameConfig(seed = 20260409010000L, zoneId = "greenwood_fringe", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("factory-first")),
            )
        val second =
            factory.newSession(
                config = FoundationGameConfig(seed = 20260409010001L, zoneId = "greenwood_fringe", playerProfessionId = "arcanist"),
                saveManager = SaveManager(tempDir.resolve("factory-second")),
            )
        val secondStart = second.playerPosition()

        assertEquals(first.automationContentIdentityHash(), second.automationContentIdentityHash())
        assertFalse(first.automationWorld() === second.automationWorld())

        first.automationMovePlayerTo(requireNotNull(first.automationStairPoint(StairDirection.DOWN)))
        assertTrue(first.perform(PlayerCommand.Wait))

        assertEquals(secondStart, second.playerPosition())
        assertEquals(0, second.currentTurnCount())
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
    fun `new foundation session enforces player creation availability for advanced professions`() {
        val blocked =
            assertThrows(IllegalArgumentException::class.java) {
                GameModule.newFoundationSession(
                    config = FoundationGameConfig(playerProfessionId = "berserker"),
                    saveManager = SaveManager(tempDir.resolve("blocked-advanced-profession")),
                )
            }

        assertTrue(blocked.message!!.contains("UNLOCKED_BUT_UNAVAILABLE"))

        val unlocked =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(playerProfessionId = "berserker"),
                saveManager = SaveManager(tempDir.resolve("released-advanced-profession")),
                profile = ProfileData(releaseUnlockedClasses = setOf("berserker")),
            )

        assertEquals("berserker", unlocked.config.playerProfessionId)

        val devLab =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(playerProfessionId = "spellblade"),
                saveManager = SaveManager(tempDir.resolve("dev-lab-advanced-profession")),
                availabilityContext = AvailabilityContext.DEV_LAB,
            )

        assertEquals("spellblade", devLab.config.playerProfessionId)
    }

    @Test
    fun `validation session uses white box availability for advanced professions without weakening locked race rules`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameModule.newFoundationSession(
                config = FoundationGameConfig(playerProfessionId = "spellblade"),
                saveManager = SaveManager(tempDir.resolve("standard-validation-profession-blocked")),
            )
        }

        val validation =
            GameModule.newValidationSession(
                ValidationSessionRequest(
                    saveManager = SaveManager(tempDir.resolve("validation-profession-allowed")),
                    options =
                        validationSessionOptionsForPreset(ValidationPreset.CUSTOM).copy(
                            foundationConfig = FoundationGameConfig(playerProfessionId = "spellblade"),
                        ),
                ),
            )

        assertEquals("spellblade", validation.config.playerProfessionId)

        val lockedRace =
            assertThrows(IllegalArgumentException::class.java) {
                GameModule.newValidationSession(
                    ValidationSessionRequest(
                        saveManager = SaveManager(tempDir.resolve("validation-locked-race")),
                        options =
                            validationSessionOptionsForPreset(ValidationPreset.CUSTOM).copy(
                                foundationConfig = FoundationGameConfig(playerRaceId = "orc"),
                            ),
                    ),
                )
            }

        assertTrue(lockedRace.message!!.contains("Race 'orc'"))
    }

    @Test
    fun `new foundation session rejects locked races even in white box contexts`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                GameModule.newFoundationSession(
                    config = FoundationGameConfig(playerRaceId = "orc"),
                    saveManager = SaveManager(tempDir.resolve("locked-race")),
                    availabilityContext = AvailabilityContext.WHITE_BOX,
                )
            }

        assertTrue(exception.message!!.contains("Race 'orc'"))
    }

    @Test
    fun `player creation state exposes profession and race options with playability`() {
        val state = GameModule.playerCreationState()

        assertEquals("vanguard", state.selection.professionId)
        assertEquals("human", state.selection.raceId)
        assertTrue(state.professionOptions.all { option -> option.resourceHintKey == "profession.${option.id}.resource_hint" })
        assertEquals(
            com.ktome.core.profile.ClassUnlockState.RELEASE_UNLOCKED,
            state.professionOptions.first { option -> option.id == "vanguard" }.unlockState,
        )
        assertEquals(
            com.ktome.core.profession.ProfessionTier.ADVANCED,
            state.professionOptions.first { option -> option.id == "spellblade" }.tier,
        )
        assertEquals(
            ClassPlayabilityState.UNLOCKED_BUT_UNAVAILABLE,
            state.professionOptions.first { option -> option.id == "spellblade" }.playabilityState,
        )
        assertEquals(
            ClassPlayabilityState.LOCKED,
            state.raceOptions.first { option -> option.id == "orc" }.playabilityState,
        )
        assertEquals(
            com.ktome.core.profile.ClassUnlockState.LOCKED,
            state.raceOptions.first { option -> option.id == "orc" }.unlockState,
        )
    }

    @Test
    fun `player creation state preserves playable selection and rewrites hidden defaults to playable options`() {
        val preserved =
            GameModule.playerCreationState(
                previousSelection = PlayerCreationSelection(professionId = "templar", raceId = "dwarf"),
            )

        assertEquals("templar", preserved.selection.professionId)
        assertEquals("dwarf", preserved.selection.raceId)

        val fallback =
            GameModule.playerCreationState(
                previousSelection = PlayerCreationSelection(professionId = "spellblade", raceId = "orc"),
            )

        assertEquals("vanguard", fallback.selection.professionId)
        assertEquals("human", fallback.selection.raceId)
        assertTrue(fallback.canStartNewGame())
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
                playerRaceId = "human",
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
    fun `load foundation session fails fast on pre interactable save contract version`() {
        val saveManager = SaveManager(tempDir.resolve("stale-contract-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText(
            """
            {
              "schemaVersion": 1,
              "saveContractVersion": { "major": 2, "minor": 0 },
              "buildMetadata": "phase2-dev",
              "timestampEpochMillis": 1,
              "worldSeed": 20260318,
              "currentZoneId": "shattered_outpost",
              "floorIndex": 1,
              "mapWidth": 60,
              "mapHeight": 40,
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
                    "rows": [".".repeat(60)],
                    "playerStart": { "x": 1, "y": 1 }
                  },
                  "exploredTiles": [],
                  "entities": []
                }
              ],
              "pendingActionIds": []
            }
            """.trimIndent().replace("\".\".repeat(60)", "\"${".".repeat(60)}\""),
        )

        assertThrows(UnsupportedSaveContractVersionException::class.java) {
            GameModule.loadFoundationSession(saveManager)
        }
    }

    @Test
    fun `load foundation session rejects pre phase3 save schema before applying defaults`() {
        val saveManager = SaveManager(tempDir.resolve("phase2-schema-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText(
            """
            {
              "schemaVersion": 2,
              "saveContractVersion": { "major": ${SaveContractVersion.CURRENT.major}, "minor": ${SaveContractVersion.CURRENT.minor} },
              "talentSchemaVersion": ${SaveSnapshot.CURRENT_TALENT_SCHEMA_VERSION},
              "buildMetadata": "phase2-dev",
              "timestampEpochMillis": 1,
              "worldSeed": 20260318,
              "currentZoneId": "shattered_outpost",
              "floorIndex": 1,
              "mapWidth": 60,
              "mapHeight": 40,
              "fovRadius": 8,
              "messageLogSize": 8,
              "playerProfessionId": "vanguard",
              "playerRaceId": "human",
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
                    "rows": ["${".".repeat(60)}"],
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

    @Test
    fun `load foundation session rejects saves without current phase4 v4 talent schema marker`() {
        val saveManager = SaveManager(tempDir.resolve("missing-talent-schema-save"))
        Files.createDirectories(saveManager.savePath().parent)
        saveManager.savePath().writeText(
            """
            {
              "schemaVersion": ${SaveSnapshot.CURRENT_SCHEMA_VERSION},
              "saveContractVersion": { "major": ${SaveContractVersion.CURRENT.major}, "minor": ${SaveContractVersion.CURRENT.minor} },
              "buildMetadata": "phase4-v4-pr01-dev"
            }
            """.trimIndent(),
        )

        val missingMarker =
            assertThrows(InvalidSaveException::class.java) {
                GameModule.loadFoundationSession(saveManager)
            }

        assertTrue(missingMarker.message!!.contains("INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA"))

        saveManager.savePath().writeText(
            """
            {
              "schemaVersion": ${SaveSnapshot.CURRENT_SCHEMA_VERSION},
              "saveContractVersion": { "major": ${SaveContractVersion.CURRENT.major}, "minor": ${SaveContractVersion.CURRENT.minor} },
              "talentSchemaVersion": ${SaveSnapshot.CURRENT_TALENT_SCHEMA_VERSION - 1},
              "buildMetadata": "phase4-v4-pr01-dev"
            }
            """.trimIndent(),
        )

        val staleMarker =
            assertThrows(InvalidSaveException::class.java) {
                GameModule.loadFoundationSession(saveManager)
            }

        assertTrue(staleMarker.message!!.contains("INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA"))
    }

    @Test
    fun `load foundation session rejects saves whose player is missing profession resource pool`() {
        val saveManager = SaveManager(tempDir.resolve("missing-profession-resource-save"))
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
                playerProfessionId = "arcanist",
                playerRaceId = "human",
                maxFloor = 2,
                turnCount = 0,
                player =
                    PlayerSnapshot(
                        entity =
                            EntitySnapshot(
                                id = 1,
                                position = PointSnapshot(1, 1),
                                resourcePools = listOf(ResourcePoolSnapshot(type = "STAMINA", current = 40, max = 40)),
                                isPlayerControlled = true,
                            ),
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

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                GameModule.loadFoundationSession(saveManager)
            }

        assertTrue(exception.message!!.contains("MANA"))
    }

    @Test
    fun `load foundation session rejects saves whose player is missing stamina pool`() {
        val saveManager = SaveManager(tempDir.resolve("missing-stamina-resource-save"))
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
                playerProfessionId = "arcanist",
                playerRaceId = "human",
                maxFloor = 2,
                turnCount = 0,
                player =
                    PlayerSnapshot(
                        entity =
                            EntitySnapshot(
                                id = 1,
                                position = PointSnapshot(1, 1),
                                resourcePools = listOf(ResourcePoolSnapshot(type = "MANA", current = 30, max = 80)),
                                isPlayerControlled = true,
                            ),
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

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                GameModule.loadFoundationSession(saveManager)
            }

        assertTrue(exception.message!!.contains("STAMINA"))
    }

    @Test
    fun `new foundation session derives starter talents combat profile and kit from profession schema`() {
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
        val rogueSession =
            GameModule.newFoundationSession(
                FoundationGameConfig(playerProfessionId = "rogue"),
                SaveManager(tempDir.resolve("rogue-save")),
            )
        val templarSession =
            GameModule.newFoundationSession(
                FoundationGameConfig(playerProfessionId = "templar"),
                SaveManager(tempDir.resolve("templar-save")),
            )

        assertEquals(listOf("猛击", "盾击", "格挡姿态"), vanguardSession.talentSlots().map { slot -> slot.name })
        assertEquals(listOf("火球", "闪现", "奥术护盾"), arcanistSession.talentSlots().map { slot -> slot.name })
        assertEquals(listOf("背刺", "潜行", "翻滚"), rogueSession.talentSlots().map { slot -> slot.name })
        assertEquals(listOf("圣击", "圣光术", "神圣护盾"), templarSession.talentSlots().map { slot -> slot.name })
        listOf(vanguardSession, arcanistSession, rogueSession, templarSession).forEach { session ->
            assertEquals(listOf(1, 2, 3), session.talentSlots().map { slot -> slot.slot })
            assertFalse(session.talentSlots().any { slot -> slot.slot == 4 })
        }
        assertEquals(listOf("长剑", "基础盾牌", "锁甲", "治疗药水"), vanguardSession.inventoryItems().map { item -> item.name })
        assertEquals(listOf("奥术法杖", "学徒法袍", "法力药水"), arcanistSession.inventoryItems().map { item -> item.name })
        assertEquals(listOf("短剑", "皮甲", "治疗药水", "传送卷轴"), rogueSession.inventoryItems().map { item -> item.name })
        assertEquals(listOf("长剑", "基础盾牌", "锁甲", "治疗药水"), templarSession.inventoryItems().map { item -> item.name })
        assertEquals("长剑", vanguardSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.WEAPON }.itemName)
        assertEquals("基础盾牌", vanguardSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.OFF_HAND }.itemName)
        assertEquals("锁甲", vanguardSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.ARMOR }.itemName)
        assertEquals("短剑", rogueSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.WEAPON }.itemName)
        assertNull(rogueSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.OFF_HAND }.itemName)
        assertEquals("皮甲", rogueSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.ARMOR }.itemName)
        assertEquals("长剑", templarSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.WEAPON }.itemName)
        assertEquals("基础盾牌", templarSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.OFF_HAND }.itemName)
        assertEquals("锁甲", templarSession.equipmentSlots().first { slot -> slot.slot == EquipSlot.ARMOR }.itemName)
        assertTrue(vanguardSession.playerStatus().maxHp > arcanistSession.playerStatus().maxHp)
        assertTrue(rogueSession.playerStatus().speed > vanguardSession.playerStatus().speed)
        assertTrue(templarSession.playerStatus().defense > rogueSession.playerStatus().defense)
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
        assertTrue(
            monsterIds.all { monsterId ->
                monsterId in
                    setOf(
                        "beast.rat",
                        "beast.thorn_stalker",
                        "undead.bone_archer",
                        "bandit.archer",
                        "undead.moss_archer",
                        "bandit.trapper",
                        "bandit.sentry",
                        "bandit.wild_huntmaster",
                    )
            },
        )
    }

    @Test
    fun `default shattered outpost opens with a real encounter pack on floor one`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "shattered_outpost"),
                SaveManager(tempDir.resolve("shattered-outpost-save")),
            )
        val world = extractWorld(session)
        val monsterIds =
            world.entitiesWith(MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value }
        val interactableIds =
            world.entitiesWith(Interactable::class)
                .map { entityId -> requireNotNull(world.get<Interactable>(entityId)).id }
                .toSet()

        assertTrue(monsterIds.isNotEmpty())
        assertTrue("bandit.archer" !in monsterIds)
        assertEquals(setOf("supply_crate", "alarm_bonfire"), interactableIds)
    }

    @Test
    fun `greenwood fringe starts with a deterministic early pressure pack`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "greenwood_fringe"),
                SaveManager(tempDir.resolve("greenwood-pack-save")),
            )
        val world = extractWorld(session)
        val monsterIds =
            world.entitiesWith(MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value }

        assertTrue(monsterIds.size >= 2)
        assertTrue(monsterIds.any { monsterId -> monsterId == "beast.rat" })
    }

    @Test
    fun `route visible encounter catalog preserves early fairness reachable packs and later ranged pressure`() {
        val loader = DataLoader()
        val zonesById = loader.loadSchemaCatalog().zones.associateBy { zone -> zone.id }
        val runtimeCatalog = loader.loadMonsterCatalog().monsters.associateBy(MonsterTemplate::id)
        val bossTemplateIdsByEncounterId = loader.loadSchemaCatalog().bossEncounters.associate { encounter -> encounter.id to encounter.templateId }
        val encounterCatalog =
            FOUNDATION_ZONE_ROUTE.flatMap { zoneId ->
                val zone = requireNotNull(zonesById[zoneId]) { "Missing zone schema for $zoneId." }
                (1..zone.floorCount).map { floor ->
                    routeVisibleEncounterFloor(zone, floor, runtimeCatalog, bossTemplateIdsByEncounterId)
                }
            }

        val shatteredOutpostFloorOne =
            encounterCatalog.single { encounter ->
                encounter.zoneId == "shattered_outpost" && encounter.floor == 1
            }
        assertFalse("bandit.archer" in shatteredOutpostFloorOne.monsterIds)
        assertTrue(shatteredOutpostFloorOne.packEnabled)

        val firstReachableRangedPressure =
            requireNotNull(
                encounterCatalog.firstOrNull { encounter ->
                    !encounter.isBossFloor &&
                        encounter.monsterIds.any { monsterId -> isRangedPressure(requireNotNull(runtimeCatalog[monsterId])) }
                },
            ) { "Expected at least one route-visible non-boss ranged pressure floor." }
        assertTrue(
            firstReachableRangedPressure.zoneId in setOf("greenwood_fringe", "deep_iron_pit"),
            "Expected first route-visible ranged/control pressure no later than deep_iron_pit, but was ${firstReachableRangedPressure.zoneId}.",
        )
        assertTrue(
            firstReachableRangedPressure.monsterIds.any { monsterId ->
                monsterId == "bandit.archer" ||
                    monsterId == "undead.bone_archer" ||
                    monsterId == "bandit.trapper" ||
                    monsterId == "undead.moss_archer" ||
                    monsterId == "cultist.ember_adept"
            },
        )
        assertTrue(
            encounterCatalog.any { encounter -> encounter.packEnabled && !encounter.isBossFloor && encounter.monsterIds.isNotEmpty() },
            "Expected pack logic to land on a route-visible non-boss floor.",
        )
    }

    @Test
    fun `zone schema owns run length and non boss final floor exits the run`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(
                    zoneId = "greenwood_fringe",
                    zoneRoute = listOf("greenwood_fringe", "deep_iron_pit"),
                    routeIndex = 0,
                ),
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
        assertFalse(session.isVictory())
        assertEquals("deep_iron_pit", session.config.zoneId)
        assertEquals(1, session.config.routeIndex)
        assertEquals(1, session.currentFloor())
    }

    @Test
    fun `zone boss encounter id drives final floor boss routing`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(zoneId = "shattered_outpost"),
                SaveManager(tempDir.resolve("shattered-outpost-boss-save")),
            )

        session.automationMovePlayerTo(requireNotNull(session.automationStairPoint(StairDirection.DOWN)))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = extractWorld(session)
        val monsterIds =
            world.entitiesWith(MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value }
                .toSet()

        assertEquals(2, session.maxFloor())
        assertTrue("bandit.captain" in monsterIds)
        assertNotNull(session.automationEntityByTemplateId("bandit.captain"))
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
                playerRaceId = "human",
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
              "schemaVersion": 3,
              "saveContractVersion": { "major": ${SaveContractVersion.CURRENT.major}, "minor": ${SaveContractVersion.CURRENT.minor} },
              "talentSchemaVersion": ${SaveSnapshot.CURRENT_TALENT_SCHEMA_VERSION},
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
              "playerRaceId": "human",
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

    @Test
    fun `load foundation session rejects cached pre routing final floor for non boss zone`() {
        val saveManager = SaveManager(tempDir.resolve("stale-final-floor-save"))
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
                playerRaceId = "human",
                maxFloor = 2,
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
                            stairsDown = PointSnapshot(10, 10),
                        ),
                        FloorSnapshot(
                            floorIndex = 2,
                            map = zoneSizedMap(width = 70, height = 45, playerStart = PointSnapshot(2, 2)),
                            entities =
                                listOf(
                                    EntitySnapshot(
                                        id = 99,
                                        position = PointSnapshot(20, 20),
                                        monsterTemplateId = FOUNDATION_BOSS_TEMPLATE_ID,
                                    ),
                                ),
                        ),
                    ),
            ),
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

    private fun hasAdjacentMonsterPair(world: World): Boolean {
        val monsterPoints =
            world.entitiesWith(Position::class, MonsterTemplateId::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return monsterPoints.any { first ->
            monsterPoints.any { second ->
                first != second && abs(first.x - second.x) + abs(first.y - second.y) == 1
            }
        }
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
