package com.ktome.game

import com.ktome.core.dungeon.FloorState
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.save.ActiveEffectSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.ItemSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StatModifierSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SessionSnapshotMapperTest {
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
                                    EntitySnapshot(id = 9, position = PointSnapshot(4, 4)),
                                    EntitySnapshot(id = 7, position = PointSnapshot(2, 2)),
                                ),
                        ),
                ),
            )

        val snapshot =
            SessionSnapshotMapper.toSaveSnapshot(
                config = FoundationGameConfig(width = 5, height = 5, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                currentFloor = 1,
                turnCount = 12,
                player = player,
                floors = floors,
                combatRandomState = null,
                sessionRandomState = null,
                pendingActionIds = listOf(1, 7),
                activeTurnActorId = 1,
            )

        assertEquals("shattered_outpost", snapshot.currentZoneId)
        assertEquals("vanguard", snapshot.playerProfessionId)
        assertEquals(listOf(21, 31), snapshot.player.carriedEntities.map(EntitySnapshot::id))
        assertEquals(listOf("ARMOR", "WEAPON"), snapshot.player.entity.equipment?.slots?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.cooldowns?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.talentLoadout?.talentLevels?.keys?.toList())
        assertEquals(listOf(1, 2), snapshot.player.entity.talentLoadout?.slotToTalentId?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.effects?.map(ActiveEffectSnapshot::id))
        assertEquals(listOf("alpha", "zeta"), snapshot.player.carriedEntities.first().itemState?.affixIds)
        assertEquals(listOf(PointSnapshot(1, 0), PointSnapshot(1, 1), PointSnapshot(3, 1)), snapshot.floors.single().exploredTiles)
        assertEquals(listOf(7, 9), snapshot.floors.single().entities.map(EntitySnapshot::id))
    }

    @Test
    fun `from save snapshot restores zone and profession ids into config`() {
        val snapshot =
            SaveSnapshot(
                timestampEpochMillis = 1L,
                worldSeed = 20260316L,
                currentZoneId = "greenwood_fringe",
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
    }
}
