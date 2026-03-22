package com.ktome.game

import com.ktome.core.dungeon.FloorState
import com.ktome.core.ecs.get
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePoolSnapshot
import com.ktome.core.save.ActiveEffectSnapshot
import com.ktome.core.save.AiTriggerTrackerSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.ItemSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StatModifierSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import com.ktome.core.talent.TalentRegistry
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionSnapshotMapperTest {
    @Test
    fun `restore world preserves stamina resource pool current`() {
        val content = content()
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        stats = com.ktome.core.save.StatsSnapshot(str = 10, dex = 10, con = 10, wil = 10),
                        combatProfile =
                            com.ktome.core.save.CombatProfileSnapshot(
                                baseAttack = 5,
                                baseDefense = 2,
                                baseHp = 50,
                                baseStamina = 40,
                            ),
                        resourcePools =
                            listOf(
                                ResourcePoolSnapshot(type = "STAMINA", current = 23, max = 90),
                            ),
                        isPlayerControlled = true,
                    ),
            )
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                stairsDown = Point(4, 1),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities = mutableListOf(),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val restoredPlayer = com.ktome.core.ecs.EntityId(1)
        val pool = requireNotNull(requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(restoredPlayer)).pool(com.ktome.core.resource.ResourceType.STAMINA))

        assertEquals(23, pool.current)
        assertEquals(90, pool.max)
    }

    @Test
    fun `to save snapshot canonicalizes collection ordering and preserves semantic ids`() {
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        cooldowns = linkedMapOf("zeta" to 1, "alpha" to 2),
                        effects =
                            listOf(
                                ActiveEffectSnapshot(id = "zeta", type = "WAR_CRY_DEBUFF", remainingTurns = 1),
                                ActiveEffectSnapshot(id = "alpha", type = "WAR_CRY_BUFF", remainingTurns = 2),
                            ),
                        talentLoadout =
                            TalentLoadoutSnapshot(
                                slotToTalentId = linkedMapOf(2 to "sweep", 1 to "power_strike"),
                                talentLevels = linkedMapOf("zeta" to 2, "alpha" to 1),
                            ),
                        inventory = InventorySnapshot(capacity = 12, itemIds = listOf(31, 21)),
                        equipment = EquipmentSnapshot(slots = linkedMapOf("WEAPON" to 21, "ARMOR" to 22)),
                        isPlayerControlled = true,
                    ),
                carriedEntities =
                    listOf(
                        EntitySnapshot(
                            id = 31,
                            itemState =
                                ItemSnapshot(
                                    baseId = "dagger",
                                    type = "WEAPON",
                                    quality = "NORMAL",
                                    affixIds = listOf("keen", "agile"),
                                    stats = StatModifierSnapshot(attack = 1),
                                ),
                        ),
                        EntitySnapshot(
                            id = 21,
                            itemState =
                                ItemSnapshot(
                                    baseId = "short_sword",
                                    type = "WEAPON",
                                    quality = "MAGIC",
                                    affixIds = listOf("zeta", "alpha"),
                                    stats = StatModifierSnapshot(attack = 2),
                                ),
                        ),
                    ),
            )

        val floors =
            listOf(
                FloorState(
                    floor = 1,
                    stairsUp = null,
                    stairsDown = Point(4, 4),
                    payload =
                        FloorRuntimeState(
                            map = GameMap.fromAscii(rows = List(5) { "....." }, playerStart = Point(1, 1)),
                            stairsDown = Point(4, 4),
                            exploredTiles = linkedSetOf(Point(3, 1), Point(1, 0), Point(1, 1)),
                            entities =
                                mutableListOf(
                                    EntitySnapshot(id = 11, position = PointSnapshot(3, 3), interactableId = "supply_crate"),
                                    EntitySnapshot(id = 9, position = PointSnapshot(4, 4)),
                                    EntitySnapshot(id = 7, position = PointSnapshot(2, 2)),
                                ),
                        ),
                ),
            )

        val snapshot =
            SessionSnapshotMapper.toSaveSnapshot(
                config =
                    FoundationGameConfig(
                        width = 5,
                        height = 5,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 1,
                    ),
                currentFloor = 1,
                turnCount = 12,
                player = player,
                floors = floors,
                combatRandomState = null,
                sessionRandomState = null,
                pendingActionIds = listOf(1, 7),
                activeTurnActorId = 1,
            )

        assertEquals("greenwood_fringe", snapshot.currentZoneId)
        assertEquals(FOUNDATION_ZONE_ROUTE, snapshot.zoneRoute)
        assertEquals(1, snapshot.routeIndex)
        assertEquals("vanguard", snapshot.playerProfessionId)
        assertEquals(listOf(21, 31), snapshot.player.carriedEntities.map(EntitySnapshot::id))
        assertEquals(listOf("ARMOR", "WEAPON"), snapshot.player.entity.equipment?.slots?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.cooldowns?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.talentLoadout?.talentLevels?.keys?.toList())
        assertEquals(listOf(1, 2), snapshot.player.entity.talentLoadout?.slotToTalentId?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.effects?.map(ActiveEffectSnapshot::id))
        assertEquals(listOf("alpha", "zeta"), snapshot.player.carriedEntities.first().itemState?.affixIds)
        assertEquals(listOf(PointSnapshot(1, 0), PointSnapshot(1, 1), PointSnapshot(3, 1)), snapshot.floors.single().exploredTiles)
        assertEquals(listOf(7, 9, 11), snapshot.floors.single().entities.map(EntitySnapshot::id))
        assertEquals("supply_crate", snapshot.floors.single().entities.last().interactableId)
    }

    @Test
    fun `from save snapshot restores zone and profession ids into config`() {
        val snapshot =
            SaveSnapshot(
                timestampEpochMillis = 1L,
                worldSeed = 20260316L,
                currentZoneId = "greenwood_fringe",
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 1,
                floorIndex = 2,
                mapWidth = 70,
                mapHeight = 45,
                fovRadius = 8,
                messageLogSize = 8,
                playerProfessionId = "rogue",
                maxFloor = 2,
                turnCount = 10,
                player =
                    PlayerSnapshot(
                        entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true),
                    ),
                floors =
                    listOf(
                        com.ktome.core.save.FloorSnapshot(
                            floorIndex = 2,
                            map =
                                com.ktome.core.save.MapSnapshot(
                                    rows = List(45) { ".".repeat(70) },
                                    playerStart = PointSnapshot(1, 1),
                                ),
                        ),
                    ),
            )

        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot)

        assertEquals("greenwood_fringe", restored.config.zoneId)
        assertEquals("rogue", restored.config.playerProfessionId)
        assertEquals(FOUNDATION_ZONE_ROUTE, restored.config.zoneRoute)
        assertEquals(1, restored.config.routeIndex)
    }

    @Test
    fun `restore item instance derives passive from base item schema instead of save payload`() {
        val content = content()
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        inventory = InventorySnapshot(capacity = 12, itemIds = listOf(21)),
                        isPlayerControlled = true,
                    ),
                carriedEntities =
                    listOf(
                        EntitySnapshot(
                            id = 21,
                            itemState =
                                ItemSnapshot(
                                    baseId = "bandit_trophy",
                                    type = "ARMOR",
                                    slot = "OFF_HAND",
                                    quality = "COMMON",
                                    stats = StatModifierSnapshot(dex = 1, accuracy = 1),
                                ),
                        ),
                    ),
            )
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                stairsDown = Point(4, 1),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities = mutableListOf(),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val itemId = requireNotNull(world.get<com.ktome.core.item.Inventory>(com.ktome.core.ecs.EntityId(1))).itemIds.single()
        val restored = requireNotNull(world.get<com.ktome.core.item.ItemInstance>(itemId))

        assertTrue(restored.passive is EquipmentPassive.DamageVsTag)
        assertEquals("bandit", (restored.passive as EquipmentPassive.DamageVsTag).tag)
        assertEquals(0.15, (restored.passive as EquipmentPassive.DamageVsTag).bonusPercent, 0.0001)
    }

    @Test
    fun `restore world preserves ai trigger tracker state`() {
        val content = content()
        val player = PlayerSnapshot(entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true))
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities =
                    mutableListOf(
                        EntitySnapshot(
                            id = 7,
                            position = PointSnapshot(3, 1),
                            monsterTemplateId = "cultist.dungeon_lord",
                            aiTriggerTracker =
                                AiTriggerTrackerSnapshot(
                                    consumedTriggerIds = listOf("dungeon_lord_opening_war_cry"),
                                    engagedInCombat = true,
                                ),
                        ),
                    ),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val tracker = requireNotNull(world.get<com.ktome.core.ecs.AiTriggerTracker>(com.ktome.core.ecs.EntityId(7)))

        assertTrue(tracker.engagedInCombat)
        assertEquals(setOf("dungeon_lord_opening_war_cry"), tracker.consumedTriggerIds)
    }

    private fun content(): GameContent {
        val loader = DataLoader()
        val talents = loader.loadTalentDefinitions()
        return GameContent(
            talents = talents,
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = loader.loadSchemaCatalog(),
            localizer = loader.localizer,
        )
    }
}
